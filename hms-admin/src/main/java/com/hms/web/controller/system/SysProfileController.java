package com.hms.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.hms.common.annotation.Log;
import com.hms.common.config.RuoYiConfig;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.enums.BusinessType;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.file.FileUploadUtils;
import com.hms.common.utils.file.MimeTypeUtils;
import com.hms.framework.web.service.TokenService;
import com.hms.system.domain.SysUserRole;
import com.hms.system.mapper.SysRoleMapper;
import com.hms.system.mapper.SysUserRoleMapper;
import com.hms.system.service.ISysUserService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(String oldPassword, String newPassword)
    {
        LoginUser loginUser = getLoginUser();
        String userName = loginUser.getUsername();
        String password = loginUser.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userName, newPassword) > 0)
        {
            // 更新缓存用户密码
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 订阅量化通知 - 维护邮箱并授予 quant 角色 add by CYQ 2026-09-02
     * 新用户邮箱为空时弹窗维护，同时自动分配邮件通知角色
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PostMapping("/subscribeQuant")
    public AjaxResult subscribeQuant(@RequestBody Map<String, String> params)
    {
        String email = params != null ? StringUtils.trim(params.get("email")) : null;
        if (StringUtils.isEmpty(email))
        {
            return error("邮箱不能为空");
        }
        if (!EMAIL_PATTERN.matcher(email).matches())
        {
            return error("邮箱格式不正确");
        }
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        // 邮箱唯一性校验（排除自己）
        SysUser checkUser = new SysUser();
        checkUser.setUserId(currentUser.getUserId());
        checkUser.setEmail(email);
        if (!userService.checkEmailUnique(checkUser))
        {
            return error("邮箱 '" + email + "' 已被占用");
        }
        // 1. 更新邮箱
        currentUser.setEmail(email);
        userService.updateUserProfile(currentUser);

        // 2. 授予 quant 角色（幂等）
        // 查询 quant 角色
        com.hms.common.core.domain.entity.SysRole quantRole = roleMapper.checkRoleKeyUnique("quant");
        if (quantRole == null)
        {
            // 兜底：按 role_key like 查询
            com.hms.common.core.domain.entity.SysRole q = new com.hms.common.core.domain.entity.SysRole();
            q.setRoleKey("quant");
            List<com.hms.common.core.domain.entity.SysRole> list = roleMapper.selectRoleList(q);
            if (list != null && !list.isEmpty())
            {
                quantRole = list.stream().filter(r -> "quant".equals(r.getRoleKey())).findFirst().orElse(list.get(0));
            }
        }
        if (quantRole == null)
        {
            return error("量化角色 quant 不存在，请先执行 hms_quant_role.sql 初始化");
        }
        Long quantRoleId = quantRole.getRoleId();
        List<Long> roleIds = roleMapper.selectRoleListByUserId(currentUser.getUserId());
        boolean alreadyHas = roleIds != null && roleIds.contains(quantRoleId);
        if (!alreadyHas)
        {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(currentUser.getUserId());
            ur.setRoleId(quantRoleId);
            userRoleMapper.batchUserRole(Collections.singletonList(ur));
        }
        // 3. 刷新缓存
        loginUser.getUser().setEmail(email);
        tokenService.setLoginUser(loginUser);

        AjaxResult ajax = AjaxResult.success(alreadyHas ? "已订阅（邮箱已更新）" : "订阅成功，已为您开通邮件通知");
        ajax.put("email", email);
        ajax.put("subscribed", true);
        ajax.put("roleId", quantRoleId);
        return ajax;
    }

    /**
     * 查询是否已订阅量化通知
     */
    @GetMapping("/subscribeQuant/status")
    public AjaxResult subscribeQuantStatus()
    {
        LoginUser loginUser = getLoginUser();
        Long userId = loginUser.getUser().getUserId();
        String email = loginUser.getUser().getEmail();
        com.hms.common.core.domain.entity.SysRole quantRole = roleMapper.checkRoleKeyUnique("quant");
        Long quantRoleId = quantRole != null ? quantRole.getRoleId() : null;
        boolean subscribed = false;
        if (quantRoleId != null)
        {
            List<Long> roleIds = roleMapper.selectRoleListByUserId(userId);
            subscribed = roleIds != null && roleIds.contains(quantRoleId);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("email", email);
        ajax.put("subscribed", subscribed);
        ajax.put("quantRoleId", quantRoleId);
        return ajax;
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION);
            if (userService.updateUserAvatar(loginUser.getUsername(), avatar))
            {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}

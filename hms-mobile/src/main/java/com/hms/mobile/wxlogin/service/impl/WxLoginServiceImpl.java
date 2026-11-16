package com.hms.mobile.wxlogin.service.impl;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.hms.common.constant.Constants;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.enums.UserStatus;
import com.hms.common.exception.ServiceException;
import com.hms.common.exception.user.UserPasswordNotMatchException;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.MessageUtils;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.ip.IpUtils;
import com.hms.framework.manager.AsyncManager;
import com.hms.framework.manager.factory.AsyncFactory;
import com.hms.framework.security.context.AuthenticationContextHolder;
import com.hms.framework.web.service.SysPermissionService;
import com.hms.framework.web.service.TokenService;
import com.hms.mobile.push.domain.PushDevice;
import com.hms.mobile.push.mapper.PushDeviceMapper;
import com.hms.mobile.push.service.IWxMaSubscribeService;
import com.hms.mobile.wxlogin.service.IWxLoginService;
import com.hms.system.service.ISysUserService;

/**
 * 微信小程序一键登录服务实现
 * add by CYQ 2026-09-06 微信小程序一键登录，首次使用需绑定系统账号
 */
@Service
public class WxLoginServiceImpl implements IWxLoginService {

    @Autowired
    private IWxMaSubscribeService wxMaSubscribeService;

    @Autowired
    private PushDeviceMapper pushDeviceMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Override
    public Map<String, Object> wxLogin(String code) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> session = wxMaSubscribeService.code2session(code);
        if (!session.containsKey("openid")) {
            result.put("errmsg", session.getOrDefault("errmsg", "换取openid失败"));
            return result;
        }
        String openid = (String) session.get("openid");
        // openid 主数据源：sys_user.wx_openid
        // modify by CYQ 2026-09-06 openid 改记到用户表
        SysUser user = userService.selectUserByWxOpenid(openid);
        if (StringUtils.isNull(user)) {
            result.put("needBind", true);
            result.put("openid", openid);
            return result;
        }
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            result.put("errmsg", "账号已被删除");
            return result;
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            result.put("errmsg", "账号已停用");
            return result;
        }
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user,
                permissionService.getMenuPermission(user));
        recordLoginInfo(user.getUserId());
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS,
                "微信一键登录成功"));
        result.put("token", tokenService.createToken(loginUser));
        return result;
    }

    @Override
    public String bindAndLogin(String openid, String username, String password) {
        if (StringUtils.isEmpty(openid)) {
            throw new ServiceException("openid不能为空");
        }
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new ServiceException("账号或密码不能为空");
        }
        Authentication authentication;
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 复用 UserDetailsServiceImpl.loadUserByUsername：校验账号状态 + 密码
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL,
                        MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
            throw new ServiceException(e.getMessage());
        } finally {
            AuthenticationContextHolder.clearContext();
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Long userId = loginUser.getUserId();
        // 该 openid 如曾绑定过其他账号，先解绑，保证同一时间只归属一个用户
        // modify by CYQ 2026-09-06 openid 主记到 sys_user，推送表同步一份（订阅消息发送用）
        SysUser clear = new SysUser();
        clear.setUserId(userId);
        clear.setWxOpenid(openid);
        userService.clearUserWxOpenid(clear);
        SysUser bind = new SysUser();
        bind.setUserId(userId);
        bind.setWxOpenid(openid);
        userService.updateUserProfile(bind);
        pushDeviceMapper.clearWxOpenid(openid, userId);
        PushDevice device = new PushDevice();
        device.setUserId(userId);
        device.setWxOpenid(openid);
        int rows = pushDeviceMapper.bindWxOpenid(device);
        if (rows == 0) {
            device.setPlatform("wx_miniapp");
            pushDeviceMapper.insertPushDevice(device);
        }
        recordLoginInfo(userId);
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS,
                "微信绑定账号登录成功"));
        return tokenService.createToken(loginUser);
    }

    /**
     * 静默检查当前微信是否已绑定系统账号（登录页控制一键登录按钮显隐用，不签发token）
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    @Override
    public boolean checkWxBound(String code) {
        try {
            if (StringUtils.isEmpty(code)) {
                return false;
            }
            Map<String, Object> session = wxMaSubscribeService.code2session(code);
            if (!session.containsKey("openid")) {
                return false;
            }
            String openid = (String) session.get("openid");
            if (StringUtils.isEmpty(openid)) {
                return false;
            }
            SysUser user = userService.selectUserByWxOpenid(openid);
            return !StringUtils.isNull(user);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 已登录用户在设置页手动绑定当前微信
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    @Override
    public void bindWxForCurrentUser(Long userId, String openid) {
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }
        if (StringUtils.isEmpty(openid)) {
            throw new ServiceException("openid不能为空");
        }
        // 该 openid 如曾绑定过其他账号，先解绑，保证同一时间只归属一个用户
        SysUser clear = new SysUser();
        clear.setUserId(userId);
        clear.setWxOpenid(openid);
        userService.clearUserWxOpenid(clear);
        SysUser bind = new SysUser();
        bind.setUserId(userId);
        bind.setWxOpenid(openid);
        userService.updateUserProfile(bind);
        pushDeviceMapper.clearWxOpenid(openid, userId);
        PushDevice device = new PushDevice();
        device.setUserId(userId);
        device.setWxOpenid(openid);
        int rows = pushDeviceMapper.bindWxOpenid(device);
        if (rows == 0) {
            device.setPlatform("wx_miniapp");
            pushDeviceMapper.insertPushDevice(device);
        }
    }

    /**
     * 已登录用户在设置页手动解绑微信
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    @Override
    public void unbindWx(Long userId) {
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }
        userService.clearWxOpenidByUserId(userId);
        pushDeviceMapper.clearWxOpenidByUserId(userId);
    }

    /**
     * 查询指定用户是否已绑定微信
     * add by CYQ 2026-09-09 设置页展示关联状态用
     */
    @Override
    public boolean isUserWxBound(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = userService.selectUserById(userId);
        return !StringUtils.isNull(user) && !StringUtils.isEmpty(user.getWxOpenid());
    }

    /**
     * 记录登录信息（与 SysLoginService.recordLoginInfo 一致）
     */
    private void recordLoginInfo(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr());
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }
}

package com.hms.mobile.wxlogin.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.annotation.Anonymous;
import com.hms.common.constant.Constants;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.utils.SecurityUtils;
import com.hms.mobile.wxlogin.service.IWxLoginService;

/**
 * 微信小程序一键登录Controller
 * add by CYQ 2026-09-06 微信小程序一键登录，首次使用需绑定系统账号
 */
@RestController
@RequestMapping("/mobile/wx/login")
public class WxLoginController extends BaseController {

    @Autowired
    private IWxLoginService wxLoginService;

    /**
     * 微信一键登录：小程序 wx.login() 拿到 code 后调用
     * openid 已绑定账号直接返回 token，未绑定返回 needBind=true + openid
     */
    @Anonymous
    @PostMapping("/wxLogin")
    public AjaxResult wxLogin(@RequestBody Map<String, String> params) {
        String code = params.get("code");
        if (code == null || code.isEmpty()) {
            return error("code不能为空");
        }
        Map<String, Object> result = wxLoginService.wxLogin(code);
        if (result.containsKey("token")) {
            AjaxResult ajax = AjaxResult.success();
            ajax.put(Constants.TOKEN, result.get("token"));
            return ajax;
        }
        if (Boolean.TRUE.equals(result.get("needBind"))) {
            AjaxResult ajax = AjaxResult.success("该微信尚未绑定账号，请先绑定系统账号");
            ajax.put("needBind", true);
            ajax.put("openid", result.get("openid"));
            return ajax;
        }
        return error("微信登录失败: " + result.getOrDefault("errmsg", "未知错误"));
    }

    /**
     * 绑定系统账号并登录：首次使用微信登录时，用系统账号密码 + openid 绑定，之后可一键登录
     */
    @Anonymous
    @PostMapping("/bindLogin")
    public AjaxResult bindLogin(@RequestBody Map<String, String> params) {
        try {
            String token = wxLoginService.bindAndLogin(params.get("openid"), params.get("username"),
                    params.get("password"));
            AjaxResult ajax = AjaxResult.success();
            ajax.put(Constants.TOKEN, token);
            return ajax;
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 静默检查当前微信是否已绑定系统账号（登录页控制一键登录按钮显隐用，不签发token）
     * 小程序端 wx.login() 拿到 code 后调用，未绑定或失败均返回 bound=false，前端隐藏一键登录按钮
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    @Anonymous
    @PostMapping("/checkBound")
    public AjaxResult checkBound(@RequestBody Map<String, String> params) {
        boolean bound = wxLoginService.checkWxBound(params.get("code"));
        AjaxResult ajax = AjaxResult.success();
        ajax.put("bound", bound);
        return ajax;
    }

    /**
     * 已登录用户在设置页手动绑定当前微信（需先调 /mobile/wx/subscribe/code2session 换 openid）
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody Map<String, String> params) {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            wxLoginService.bindWxForCurrentUser(loginUser.getUserId(), params.get("openid"));
            return success("微信关联成功，下次登录可使用微信一键登录");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 已登录用户在设置页手动解绑微信
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    @PostMapping("/unbind")
    public AjaxResult unbind() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            wxLoginService.unbindWx(loginUser.getUserId());
            return success("已解除微信关联");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 查询当前登录用户是否已绑定微信（设置页展示关联状态用）
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    @PostMapping("/status")
    public AjaxResult status() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        boolean bound = wxLoginService.isUserWxBound(loginUser.getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("bound", bound);
        return ajax;
    }
}

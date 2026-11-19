package com.hms.mobile.wxlogin.service;

import java.util.Map;

/**
 * 微信小程序一键登录服务接口
 * add by CYQ 2026-09-06 微信小程序一键登录，首次使用需绑定系统账号
 * @author 崔永琦
 */
public interface IWxLoginService {

    /**
     * 微信一键登录：用 wx.login() 的 code 换 openid，按 openid 反查已绑定用户
     *
     * @param code 小程序 wx.login() 返回的 code
     * @return 含 token（已绑定直接登录）或 needBind=true + openid（未绑定需先绑账号），失败含 errmsg
     */
    Map<String, Object> wxLogin(String code);

    /**
     * 绑定系统账号并登录：校验账号密码，绑定 openid 后签发 token
     *
     * @param openid 微信小程序 openid
     * @param username 系统账号
     * @param password 系统密码
     * @return 登录 token
     */
    String bindAndLogin(String openid, String username, String password);

    /**
     * 静默检查当前微信是否已绑定系统账号（登录页控制一键登录按钮显隐用，不签发token）
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     *
     * @param code 小程序 wx.login() 返回的 code
     * @return true 已绑定，false 未绑定或换取openid失败
     */
    boolean checkWxBound(String code);

    /**
     * 已登录用户在设置页手动绑定当前微信
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     *
     * @param userId 当前登录用户ID
     * @param openid 微信小程序 openid
     */
    void bindWxForCurrentUser(Long userId, String openid);

    /**
     * 已登录用户在设置页手动解绑微信
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     *
     * @param userId 当前登录用户ID
     */
    void unbindWx(Long userId);

    /**
     * 查询指定用户是否已绑定微信
     * add by CYQ 2026-09-09 设置页展示关联状态用
     *
     * @param userId 用户ID
     * @return true 已绑定
     */
    boolean isUserWxBound(Long userId);
}

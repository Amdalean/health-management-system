package com.hms.mobile.push.service;

import java.util.Map;

/**
 * 微信小程序订阅消息服务接口
 * add by CYQ 2026-06-10
 * @author 崔永琦
 */
public interface IWxMaSubscribeService {

    /** 获取 access_token（带缓存，自动刷新） */
    String getAccessToken();

    /** 向指定用户发送订阅消息 */
    String sendSubscribeMessage(String openId, String page, Map<String, Object> data);

    /** 发送打卡提醒 */
    String sendClockinRemind(String openId, String name, String date, String status);

    /**
     * 通过 code 换取 openid（wx.login 回调）
     * @param code 小程序 wx.login() 返回的 code
     * @return Map 包含 openid, session_key, unionid(可选) 或 errcode 错误信息
     */
    Map<String, Object> code2session(String code);
}

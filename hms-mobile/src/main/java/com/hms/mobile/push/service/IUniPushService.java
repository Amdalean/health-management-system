package com.hms.mobile.push.service;

import com.hms.mobile.push.domain.PushDevice;

import java.util.List;

/**
 * UniPush 推送发送服务接口
 * add by CYQ 2026-05-22
 * @author 崔永琦
 */
public interface IUniPushService {

    /**
     * 向单个设备发送通知推送
     *
     * @param clientId 设备CID
     * @param title    通知标题
     * @param body     通知内容
     * @return 推送结果JSON
     */
    String pushToSingle(String clientId, String title, String body);

    /**
     * 向所有设备发送通知推送（群推）
     *
     * @param title 通知标题
     * @param body  通知内容
     * @return 推送结果JSON
     */
    String pushToAll(String title, String body);

    /**
     * 向指定用户的所有设备发送通知推送
     *
     * @param userId 用户ID
     * @param title  通知标题
     * @param body   通知内容
     * @return 推送结果(成功数/总量)
     */
    String pushToUser(Long userId, String title, String body);
}
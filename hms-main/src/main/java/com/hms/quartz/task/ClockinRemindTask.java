package com.hms.quartz.task;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hms.common.utils.DateUtils;
import com.hms.mobile.clockin.service.IClockinRecordService;
import com.hms.mobile.push.domain.PushDevice;
import com.hms.mobile.push.service.IPushDeviceService;
import com.hms.mobile.push.service.IWxMaSubscribeService;

/**
 * 打卡提醒定时任务
 * add by CYQ 2026-06-10 说明: 向未打卡用户发送微信订阅消息提醒
 */
@Component("ClockinRemindTask")
public class ClockinRemindTask {

    private static final Logger log = LoggerFactory.getLogger(ClockinRemindTask.class);

    @Autowired
    private IPushDeviceService pushDeviceService;

    @Autowired
    private IClockinRecordService clockinRecordService;

    @Autowired
    private IWxMaSubscribeService wxMaSubscribeService;

    /**
     * 执行打卡提醒任务
     * 调用方式: quartz -> invoke_target = ClockinRemindTask.execute()
     */
    public String execute() {
        try {
            log.info("开始执行打卡提醒任务...");
            String today = DateUtils.dateTimeNow("yyyy-MM-dd");

            // 1. 查询今日未打卡的考勤用户
            List<Map<String, Object>> notClockedIn = clockinRecordService.selectNotClockedInUsersByDate(today);
            if (notClockedIn == null || notClockedIn.isEmpty()) {
                log.info("今日所有考勤用户均已打卡，无需发送提醒");
                return "所有用户均已打卡，未发送提醒";
            }

            // 构建未打卡用户ID集合和姓名映射
            Set<Long> notClockedInUserIds = new HashSet<>();
            Map<Long, String> userIdNameMap = new java.util.HashMap<>();
            for (Map<String, Object> user : notClockedIn) {
                Object userIdObj = user.get("userId");
                if (userIdObj != null) {
                    Long userId = Long.valueOf(userIdObj.toString());
                    notClockedInUserIds.add(userId);
                    String userName = (String) user.getOrDefault("userName", "");
                    userIdNameMap.put(userId, userName);
                }
            }
            log.info("今日未打卡考勤用户: {} 人", notClockedInUserIds.size());

            // 2. 查询微信小程序设备（platform = 'wx_miniapp'）
            PushDevice query = new PushDevice();
            query.setPlatform("wx_miniapp");
            List<PushDevice> devices = pushDeviceService.selectPushDeviceList(query);

            // 3. 筛选有openid且未打卡的设备，发送提醒
            int sent = 0;
            int skipped = 0;
            for (PushDevice device : devices) {
                if (device.getWxOpenid() == null || device.getWxOpenid().isEmpty()) {
                    skipped++;
                    continue;
                }
                if (device.getUserId() == null || !notClockedInUserIds.contains(device.getUserId())) {
                    continue;
                }
                String userName = userIdNameMap.getOrDefault(device.getUserId(), "员工");
                try {
                    String result = wxMaSubscribeService.sendClockinRemind(
                            device.getWxOpenid(), userName, today, "待签到");
                    log.info("发送打卡提醒: userId={}, userName={}, openId={}, result={}",
                            device.getUserId(), userName, device.getWxOpenid(), result);
                    sent++;
                } catch (Exception e) {
                    log.error("发送打卡提醒失败: userId={}, openId={}", device.getUserId(), device.getWxOpenid(), e);
                }
            }

            String msg = String.format("打卡提醒任务完成: 发送 %d 条, 跳过无openid %d 个", sent, skipped);
            log.info(msg);
            return msg;
        } catch (Exception e) {
            log.error("执行打卡提醒任务异常", e);
            return "执行失败: " + e.getMessage();
        }
    }
}
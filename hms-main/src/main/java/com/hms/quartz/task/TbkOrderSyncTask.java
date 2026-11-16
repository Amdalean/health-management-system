package com.hms.quartz.task;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.hms.main.tbk.service.TbkOrderService;

/**
 * 联盟订单同步定时任务
 *
 * 每 30 分钟回看最近几个小时，覆盖淘宝/拼多多订单状态变化。
 * 注意：淘宝客订单查询为邀请制接口，未开通时任务只记录提示，不影响拼多多。
 *
 * add by CYQ 2026-11-09 联盟收益 PC 看板
 */
@Component("tbkOrderSyncTask")
public class TbkOrderSyncTask {

    private static final Logger log = LoggerFactory.getLogger(TbkOrderSyncTask.class);

    @Autowired
    private TbkOrderService orderService;

    public String excute() {
        try {
            Map<String, Object> result = orderService.syncRecent();
            String message = "联盟订单同步完成：" + JSON.toJSONString(result);
            log.info(message);
            return message;
        } catch (Exception e) {
            log.error("联盟订单同步失败", e);
            return "联盟订单同步失败：" + e.getMessage();
        }
    }
}

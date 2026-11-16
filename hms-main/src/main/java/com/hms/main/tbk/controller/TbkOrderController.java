package com.hms.main.tbk.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.main.tbk.domain.TbkOrder;
import com.hms.main.tbk.service.TbkOrderService;

/**
 * 联盟收益（PC 端订单/收益看板）
 *
 * add by CYQ 2026-11-09 联盟收益 PC 看板
 */
@RestController
@RequestMapping("/tbk/order")
public class TbkOrderController extends BaseController {

    @Autowired
    private TbkOrderService orderService;

    /**
     * 订单列表
     */
    @PreAuthorize("@ss.hasPermi('main:tbk:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(TbkOrder query) {
        startPage();
        return getDataTable(orderService.selectTbkOrderList(query));
    }

    /**
     * 收益汇总：今日/本月/累计 + 平台拆分
     */
    @PreAuthorize("@ss.hasPermi('main:tbk:order:list')")
    @GetMapping("/stats")
    public AjaxResult stats(TbkOrder query) {
        return success(orderService.selectStats(query));
    }

    /**
     * 手动同步订单
     *
     * 不传 days 时只回看最近几个小时；传 days 时按 2 小时切片补同步。
     */
    @PreAuthorize("@ss.hasPermi('main:tbk:order:sync')")
    @Log(title = "联盟订单", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public AjaxResult sync(@RequestBody(required = false) Map<String, Object> body) {
        int days = 0;
        if (body != null && body.get("days") != null) {
            try {
                days = Integer.parseInt(String.valueOf(body.get("days")));
            } catch (NumberFormatException ignored) {
                days = 0;
            }
        }
        return success(days > 0 ? orderService.syncDays(days) : orderService.syncRecent());
    }
}

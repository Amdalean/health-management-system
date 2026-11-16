package com.hms.mobile.push.controller;

import com.hms.common.annotation.Anonymous;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.common.annotation.Log;
import com.hms.common.utils.SecurityUtils;
import com.hms.mobile.push.domain.PushDevice;
import com.hms.mobile.push.service.IPushDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推送设备信息Controller (移动端专用)
 * add by CYQ 2026-05-17 说明: 提供移动端设备上报等接口
 */
@RestController
@RequestMapping("/mobile/push")
public class MobilePushController extends BaseController {
    @Autowired
    private IPushDeviceService pushDeviceService;

    /**
     * 上报推送设备信息 (移动端调用，无需登录)
     * 注意：虽然标记为Anonymous，但实际业务中可能需要用户登录后才能绑定用户ID
     * 这里为了兼容前端在登录前上报CID的场景，允许匿名访问，但后台会尝试获取登录用户
     */
    @Anonymous
    @PostMapping("/device")
    public AjaxResult reportDevice(@RequestBody PushDevice pushDevice) {
        // 尝试设置当前用户ID（如果已登录）
        try {
            Long userId = SecurityUtils.getLoginUser().getUserId();
            pushDevice.setUserId(userId);
        } catch (Exception e) {
            // 未登录用户，userId保持为null或前端传入的值
        }
        // clientId已存在则更新，否则新增
        PushDevice existing = pushDeviceService.selectPushDeviceByClientId(pushDevice.getClientId());
        int result;
        if (existing != null) {
            pushDevice.setId(existing.getId());
            // 如果前端未传userId且原记录有userId，保留原值
            if (pushDevice.getUserId() == null && existing.getUserId() != null) {
                pushDevice.setUserId(existing.getUserId());
            }
            result = pushDeviceService.updatePushDevice(pushDevice);
        } else {
            result = pushDeviceService.insertPushDevice(pushDevice);
        }
        if (result > 0) {
            return AjaxResult.success("设备信息上报成功");
        } else {
            return AjaxResult.error("设备信息上报失败");
        }
    }

    /**
     * 根据客户端ID获取推送设备信息
     * 用于验证设备是否存在，或获取绑定的用户信息
     */
    @Anonymous
    @GetMapping("/device/client/{clientId}")
    public AjaxResult getByClientId(@PathVariable String clientId) {
        PushDevice device = pushDeviceService.selectPushDeviceByClientId(clientId);
        if (device != null) {
            return success(device);
        } else {
            return error("未找到该设备");
        }
    }

    // ========== 以下为管理后台接口，保留但不建议移动端普通用户调用 ==========

    /**
     * 查询推送设备信息列表
     */
    @PreAuthorize("@ss.hasPermi('system:push:list')")
    @GetMapping("/device/list")
    public TableDataInfo list(PushDevice pushDevice) {
        startPage();
        List<PushDevice> list = pushDeviceService.selectPushDeviceList(pushDevice);
        return getDataTable(list);
    }

    /**
     * 获取推送设备信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:push:query')")
    @GetMapping(value = "/device/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(pushDeviceService.selectPushDeviceById(id));
    }

    /**
     * 修改推送设备信息
     */
    @PreAuthorize("@ss.hasPermi('system:push:edit')")
    @Log(title = "推送设备", businessType = BusinessType.UPDATE)
    @PutMapping("/device")
    public AjaxResult edit(@RequestBody PushDevice pushDevice) {
        return toAjax(pushDeviceService.updatePushDevice(pushDevice));
    }

    /**
     * 删除推送设备信息
     */
    @PreAuthorize("@ss.hasPermi('system:push:remove')")
    @Log(title = "推送设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/device/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pushDeviceService.deletePushDeviceByIds(ids));
    }
}

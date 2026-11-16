package com.hms.mobile.appversion.controller;

import com.hms.common.annotation.Anonymous;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.common.annotation.Log;
import com.hms.mobile.appversion.domain.SysAppVersion;
import com.hms.mobile.appversion.service.ISysAppVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * APP版本管理Controller (移动端专用)
 * add by CYQ 2026-05-17 说明: 提供移动端版本检查等接口
 */
@RestController
@RequestMapping("/mobile/appVersion")
public class SysAppVersionController extends BaseController {
    @Autowired
    private ISysAppVersionService appVersionService;

    /**
     * 获取最新版本信息 (移动端调用，无需登录)
     */
    @Anonymous
    @GetMapping("/latest")
    public AjaxResult getLatestVersion(@RequestParam(required = false) String platform) {
        if (platform == null || platform.trim().isEmpty()) {
            platform = "android";
        }
        SysAppVersion version = appVersionService.selectLatestAppVersion(platform);
        if (version == null) {
            return AjaxResult.error("暂无版本信息");
        }
        return AjaxResult.success(version);
    }

    // ========== 以下为管理后台接口 ==========

    @PreAuthorize("@ss.hasPermi('mobile:appVersion:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAppVersion appVersion) {
        startPage();
        return getDataTable(appVersionService.selectAppVersionList(appVersion));
    }

    @PreAuthorize("@ss.hasPermi('mobile:appVersion:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(appVersionService.selectAppVersionById(id));
    }

    @PreAuthorize("@ss.hasPermi('mobile:appVersion:add')")
    @Log(title = "APP版本", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysAppVersion appVersion) {
        return toAjax(appVersionService.insertAppVersion(appVersion));
    }

    @PreAuthorize("@ss.hasPermi('mobile:appVersion:edit')")
    @Log(title = "APP版本", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysAppVersion appVersion) {
        return toAjax(appVersionService.updateAppVersion(appVersion));
    }

    @PreAuthorize("@ss.hasPermi('mobile:appVersion:remove')")
    @Log(title = "APP版本", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(appVersionService.deleteAppVersionByIds(ids));
    }
}

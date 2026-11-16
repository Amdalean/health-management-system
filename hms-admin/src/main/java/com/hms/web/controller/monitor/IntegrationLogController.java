package com.hms.web.controller.monitor;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.system.domain.HmsIntegrationLog;
import com.hms.system.service.IHmsIntegrationLogService;

/**
 * 系统集成日志
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 */
@RestController
@RequestMapping("/monitor/integrationlog")
public class IntegrationLogController extends BaseController
{
    @Autowired
    private IHmsIntegrationLogService integrationLogService;

    /**
     * 查询集成日志列表
     */
    @PreAuthorize("@ss.hasPermi('monitor:integrationlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmsIntegrationLog integrationLog)
    {
        startPage();
        List<HmsIntegrationLog> list = integrationLogService.selectHmsIntegrationLogList(integrationLog);
        return getDataTable(list);
    }

    /**
     * 导出集成日志
     */
    @Log(title = "集成日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:integrationlog:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, HmsIntegrationLog integrationLog)
    {
        List<HmsIntegrationLog> list = integrationLogService.selectHmsIntegrationLogList(integrationLog);
        ExcelUtil<HmsIntegrationLog> util = new ExcelUtil<HmsIntegrationLog>(HmsIntegrationLog.class);
        util.exportExcel(response, list, "集成日志");
    }

    /**
     * 获取集成日志详细
     */
    @PreAuthorize("@ss.hasPermi('monitor:integrationlog:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        return success(integrationLogService.selectHmsIntegrationLogById(logId));
    }

    /**
     * 删除集成日志
     */
    @Log(title = "集成日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:integrationlog:remove')")
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(integrationLogService.deleteHmsIntegrationLogByIds(logIds));
    }

    /**
     * 清空集成日志
     */
    @Log(title = "集成日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:integrationlog:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        integrationLogService.cleanHmsIntegrationLog();
        return success();
    }
}

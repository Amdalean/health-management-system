package com.hms.mobile.officelocation.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.mobile.officelocation.domain.OfficeLocation;
import com.hms.mobile.officelocation.service.IOfficeLocationService;

/**
 * 办公地点Controller
 * add by CYQ 2026-06-06 说明: 办公地点CRUD管理，用于打卡地理围栏配置
 */
@RestController
@RequestMapping("/mobile/officeLocation")
/**
 * public class OfficeLocationController extends BaseController {
 *
 * @author 崔永琦
 */
public class OfficeLocationController extends BaseController {

    @Autowired
    private IOfficeLocationService officeLocationService;

    /**
     * 查询办公地点列表
     */
    @PreAuthorize("@ss.hasPermi('mobile:officeLocation:list')")
    @GetMapping("/list")
    public TableDataInfo list(OfficeLocation officeLocation) {
        startPage();
        List<OfficeLocation> list = officeLocationService.selectOfficeLocationList(officeLocation);
        return getDataTable(list);
    }

    /**
     * 导出办公地点
     */
    @PreAuthorize("@ss.hasPermi('mobile:officeLocation:export')")
    @Log(title = "办公地点", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OfficeLocation officeLocation) {
        List<OfficeLocation> list = officeLocationService.selectOfficeLocationList(officeLocation);
        ExcelUtil<OfficeLocation> util = new ExcelUtil<OfficeLocation>(OfficeLocation.class);
        util.exportExcel(response, list, "办公地点数据");
    }

    /**
     * 获取办公地点详情
     */
    @PreAuthorize("@ss.hasPermi('mobile:officeLocation:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(officeLocationService.selectOfficeLocationById(id));
    }

    /**
     * 新增办公地点
     */
    @PreAuthorize("@ss.hasPermi('mobile:officeLocation:add')")
    @Log(title = "办公地点", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OfficeLocation officeLocation) {
        return toAjax(officeLocationService.insertOfficeLocation(officeLocation));
    }

    /**
     * 修改办公地点
     */
    @PreAuthorize("@ss.hasPermi('mobile:officeLocation:edit')")
    @Log(title = "办公地点", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OfficeLocation officeLocation) {
        return toAjax(officeLocationService.updateOfficeLocation(officeLocation));
    }

    /**
     * 删除办公地点
     */
    @PreAuthorize("@ss.hasPermi('mobile:officeLocation:remove')")
    @Log(title = "办公地点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(officeLocationService.deleteOfficeLocationByIds(ids));
    }
}

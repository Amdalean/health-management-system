package com.hms.main.reminder.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.hms.main.reminder.domain.HmsReminderVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.enums.BusinessType;
import com.hms.main.reminder.domain.HmsReminder;
import com.hms.main.reminder.service.IHmsReminderService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 提醒管理Controller
 * 
 * @author CYQ
 * @date 2026-01-26
 */
@RestController
@RequestMapping("/main/reminder")
public class HmsReminderController extends BaseController
{
    @Autowired
    private IHmsReminderService hmsReminderService;

    /**
     * 查询提醒管理列表
     */
    @PreAuthorize("@ss.hasPermi('main:reminder:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmsReminder hmsReminder)
    {
        startPage();
        List<HmsReminderVO> list = hmsReminderService.selectHmsReminderVOList(hmsReminder);
        return getDataTable(list);
    }

    /**
     * 导出提醒管理列表
     */
    @PreAuthorize("@ss.hasPermi('main:reminder:export')")
    @Log(title = "提醒管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HmsReminder hmsReminder)
    {
        List<HmsReminder> list = hmsReminderService.selectHmsReminderList(hmsReminder);
        ExcelUtil<HmsReminder> util = new ExcelUtil<HmsReminder>(HmsReminder.class);
        util.exportExcel(response, list, "提醒管理数据");
    }

    /**
     * 获取提醒管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:reminder:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hmsReminderService.selectHmsReminderById(id));
    }

    /**
     * 新增提醒管理
     */
    @PreAuthorize("@ss.hasPermi('main:reminder:add')")
    @Log(title = "提醒管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmsReminder hmsReminder)
    {
        return toAjax(hmsReminderService.insertHmsReminder(hmsReminder));
    }

    /**
     * 修改提醒管理
     */
    @PreAuthorize("@ss.hasPermi('main:reminder:edit')")
    @Log(title = "提醒管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmsReminder hmsReminder)
    {
        return toAjax(hmsReminderService.updateHmsReminder(hmsReminder));
    }

    /**
     * 删除提醒管理
     */
    @PreAuthorize("@ss.hasPermi('main:reminder:remove')")
    @Log(title = "提醒管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hmsReminderService.deleteHmsReminderByIds(ids));
    }
}

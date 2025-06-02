package com.hms.main.summary.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
import com.hms.main.summary.domain.HsmSummary;
import com.hms.main.summary.service.IHsmSummaryService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 财务汇总主Controller
 * 
 * @author CYQ
 * @date 2025-02-05
 */
@RestController
@RequestMapping("/main/summary")
public class HsmSummaryController extends BaseController
{
    @Autowired
    private IHsmSummaryService hsmSummaryService;

    /**
     * 查询财务汇总主列表
     */
    @PreAuthorize("@ss.hasPermi('main:summary:list')")
    @GetMapping("/list")
    public TableDataInfo list(HsmSummary hsmSummary)
    {
        startPage();
        List<HsmSummary> list = hsmSummaryService.selectHsmSummaryList(hsmSummary);
        return getDataTable(list);
    }

    /**
     * 导出财务汇总主列表
     */
    @PreAuthorize("@ss.hasPermi('main:summary:export')")
    @Log(title = "财务汇总主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HsmSummary hsmSummary)
    {
        List<HsmSummary> list = hsmSummaryService.selectHsmSummaryList(hsmSummary);
        ExcelUtil<HsmSummary> util = new ExcelUtil<HsmSummary>(HsmSummary.class);
        util.exportExcel(response, list, "财务汇总主数据");
    }

    /**
     * 获取财务汇总主详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:summary:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hsmSummaryService.selectHsmSummaryById(id));
    }

    /**
     * 获取财务汇总主详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:summary:init')")
    @GetMapping(value = "/init")
    public AjaxResult getInit()
    {
        return success(hsmSummaryService.initHsmSummary());
//        return success(null);
    }

    /**
     * 新增财务汇总主
     */
    @PreAuthorize("@ss.hasPermi('main:summary:add')")
    @Log(title = "财务汇总主", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HsmSummary hsmSummary)
    {
        return toAjax(hsmSummaryService.insertHsmSummary(hsmSummary));
    }

    /**
     * 修改财务汇总主
     */
    @PreAuthorize("@ss.hasPermi('main:summary:edit')")
    @Log(title = "财务汇总主", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HsmSummary hsmSummary)
    {
        return toAjax(hsmSummaryService.updateHsmSummary(hsmSummary));
    }

    /**
     * 删除财务汇总主
     */
    @PreAuthorize("@ss.hasPermi('main:summary:remove')")
    @Log(title = "财务汇总主", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hsmSummaryService.deleteHsmSummaryByIds(ids));
    }
    /**
     * 检查支出
     */
//    @PreAuthorize("@ss.hasPermi('main:summary:list')")
    @GetMapping("/checkExpense")
    public AjaxResult forms()
    {
//        startPage();
        HsmSummary hsmSummary = new HsmSummary();
        List<HsmSummary> list = hsmSummaryService.selectHsmSummaryList(hsmSummary);
        JSONArray array = new JSONArray();
        list.stream().forEach(v->{
            JSONObject json = new JSONObject();
            json.put("date", v.getYear()+"-"+v.getMonth());
            json.put("expense", v.getExpense());
            array.add(json);
        });

        return success(array);
    }
}

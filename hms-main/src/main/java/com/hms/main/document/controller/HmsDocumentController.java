package com.hms.main.document.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import com.hms.main.document.domain.HmsDocument;
import com.hms.main.document.service.IHmsDocumentService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 资料库管理Controller
 * 
 * @author CYQ
 * @date 2025-01-28
 */
@RestController
@RequestMapping("/main/document")
public class HmsDocumentController extends BaseController
{
    @Autowired
    private IHmsDocumentService hmsDocumentService;

    /**
     * 查询资料库管理列表
     */
    @PreAuthorize("@ss.hasPermi('main:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmsDocument hmsDocument)
    {
        startPage();
        List<HmsDocument> list = hmsDocumentService.selectHmsDocumentList(hmsDocument);
        return getDataTable(list);
    }

    /**
     * 导出资料库管理列表
     */
    @PreAuthorize("@ss.hasPermi('main:document:export')")
    @Log(title = "资料库管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HmsDocument hmsDocument)
    {
        List<HmsDocument> list = hmsDocumentService.selectHmsDocumentList(hmsDocument);
        ExcelUtil<HmsDocument> util = new ExcelUtil<HmsDocument>(HmsDocument.class);
        util.exportExcel(response, list, "资料库管理数据");
    }

    /**
     * 获取资料库管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:document:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hmsDocumentService.selectHmsDocumentById(id));
    }

    /**
     * 新增资料库管理
     */
    @PreAuthorize("@ss.hasPermi('main:document:add')")
    @Log(title = "资料库管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmsDocument hmsDocument)
    {
        return toAjax(hmsDocumentService.insertHmsDocument(hmsDocument));
    }

    /**
     * 修改资料库管理
     */
    @PreAuthorize("@ss.hasPermi('main:document:edit')")
    @Log(title = "资料库管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmsDocument hmsDocument)
    {
        return toAjax(hmsDocumentService.updateHmsDocument(hmsDocument));
    }

    /**
     * 删除资料库管理
     */
    @PreAuthorize("@ss.hasPermi('main:document:remove')")
    @Log(title = "资料库管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hmsDocumentService.deleteHmsDocumentByIds(ids));
    }
}

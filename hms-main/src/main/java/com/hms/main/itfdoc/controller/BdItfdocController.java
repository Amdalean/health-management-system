package com.hms.main.itfdoc.controller;

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
import com.hms.main.itfdoc.domain.BdItfdoc;
import com.hms.main.itfdoc.service.IBdItfdocService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 接口档案Controller
 * 
 * @author CYQ
 * @date 2025-04-08
 */
@RestController
@RequestMapping("/main/itfdoc")
public class BdItfdocController extends BaseController
{
    @Autowired
    private IBdItfdocService bdItfdocService;

    /**
     * 查询接口档案列表
     */
    @PreAuthorize("@ss.hasPermi('main:itfdoc:list')")
    @GetMapping("/list")
    public TableDataInfo list(BdItfdoc bdItfdoc)
    {
        startPage();
        List<BdItfdoc> list = bdItfdocService.selectBdItfdocList(bdItfdoc);
        return getDataTable(list);
    }

    /**
     * 导出接口档案列表
     */
    @PreAuthorize("@ss.hasPermi('main:itfdoc:export')")
    @Log(title = "接口档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BdItfdoc bdItfdoc)
    {
        List<BdItfdoc> list = bdItfdocService.selectBdItfdocList(bdItfdoc);
        ExcelUtil<BdItfdoc> util = new ExcelUtil<BdItfdoc>(BdItfdoc.class);
        util.exportExcel(response, list, "接口档案数据");
    }

    /**
     * 获取接口档案详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:itfdoc:query')")
    @GetMapping(value = "/{pkItfdoc}")
    public AjaxResult getInfo(@PathVariable("pkItfdoc") Long pkItfdoc)
    {
        return success(bdItfdocService.selectBdItfdocByPkItfdoc(pkItfdoc));
    }

    /**
     * 新增接口档案
     */
    @PreAuthorize("@ss.hasPermi('main:itfdoc:add')")
    @Log(title = "接口档案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BdItfdoc bdItfdoc)
    {
        return toAjax(bdItfdocService.insertBdItfdoc(bdItfdoc));
    }

    /**
     * 修改接口档案
     */
    @PreAuthorize("@ss.hasPermi('main:itfdoc:edit')")
    @Log(title = "接口档案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BdItfdoc bdItfdoc)
    {
        return toAjax(bdItfdocService.updateBdItfdoc(bdItfdoc));
    }

    /**
     * 删除接口档案
     */
    @PreAuthorize("@ss.hasPermi('main:itfdoc:remove')")
    @Log(title = "接口档案", businessType = BusinessType.DELETE)
	@DeleteMapping("/{pkItfdocs}")
    public AjaxResult remove(@PathVariable Long[] pkItfdocs)
    {
        return toAjax(bdItfdocService.deleteBdItfdocByPkItfdocs(pkItfdocs));
    }
}

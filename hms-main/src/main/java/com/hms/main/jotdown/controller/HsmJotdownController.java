package com.hms.main.jotdown.controller;

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
import com.hms.main.jotdown.domain.HsmJotdown;
import com.hms.main.jotdown.service.IHsmJotdownService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 随手记Controller
 * 
 * @author ruoyi
 * @date 2024-11-16
 */
@RestController
@RequestMapping("/main/jotdown")
public class HsmJotdownController extends BaseController
{
    @Autowired
    private IHsmJotdownService hsmJotdownService;

    /**
     * 查询随手记列表
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:list')")
    @GetMapping("/list")
    public TableDataInfo list(HsmJotdown hsmJotdown)
    {
        startPage();
        List<HsmJotdown> list = hsmJotdownService.selectHsmJotdownList(hsmJotdown);
        return getDataTable(list);
    }

    /**
     * 导出随手记列表
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:export')")
    @Log(title = "随手记", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HsmJotdown hsmJotdown)
    {
        List<HsmJotdown> list = hsmJotdownService.selectHsmJotdownList(hsmJotdown);
        ExcelUtil<HsmJotdown> util = new ExcelUtil<HsmJotdown>(HsmJotdown.class);
        util.exportExcel(response, list, "随手记数据");
    }

    /**
     * 获取随手记详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hsmJotdownService.selectHsmJotdownById(id));
    }

    /**
     * 新增随手记
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:add')")
    @Log(title = "随手记", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HsmJotdown hsmJotdown)
    {
        return toAjax(hsmJotdownService.insertHsmJotdown(hsmJotdown));
    }

    /**
     * 修改随手记
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:edit')")
    @Log(title = "随手记", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HsmJotdown hsmJotdown)
    {
        return toAjax(hsmJotdownService.updateHsmJotdown(hsmJotdown));
    }

    /**
     * 删除随手记
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:remove')")
    @Log(title = "随手记", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hsmJotdownService.deleteHsmJotdownByIds(ids));
    }
}

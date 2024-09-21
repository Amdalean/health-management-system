package com.hms.main.student.controller;

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
import com.hms.main.student.domain.HmsStudent;
import com.hms.main.student.service.IHmsStudentService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 学生信息Controller
 * 
 * @author CYQ
 * @date 2024-09-13
 */
@RestController
@RequestMapping("/main/student")
public class HmsStudentController extends BaseController
{
    @Autowired
    private IHmsStudentService hmsStudentService;

    /**
     * 查询学生信息列表
     */
    @PreAuthorize("@ss.hasPermi('main:student:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmsStudent hmsStudent)
    {
        startPage();
        List<HmsStudent> list = hmsStudentService.selectHmsStudentList(hmsStudent);
        return getDataTable(list);
    }

    /**
     * 导出学生信息列表
     */
    @PreAuthorize("@ss.hasPermi('main:student:export')")
    @Log(title = "学生信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HmsStudent hmsStudent)
    {
        List<HmsStudent> list = hmsStudentService.selectHmsStudentList(hmsStudent);
        ExcelUtil<HmsStudent> util = new ExcelUtil<HmsStudent>(HmsStudent.class);
        util.exportExcel(response, list, "学生信息数据");
    }

    /**
     * 获取学生信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:student:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hmsStudentService.selectHmsStudentById(id));
    }

    /**
     * 新增学生信息
     */
    @PreAuthorize("@ss.hasPermi('main:student:add')")
    @Log(title = "学生信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmsStudent hmsStudent)
    {
        return toAjax(hmsStudentService.insertHmsStudent(hmsStudent));
    }

    /**
     * 修改学生信息
     */
    @PreAuthorize("@ss.hasPermi('main:student:edit')")
    @Log(title = "学生信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmsStudent hmsStudent)
    {
        return toAjax(hmsStudentService.updateHmsStudent(hmsStudent));
    }

    /**
     * 删除学生信息
     */
    @PreAuthorize("@ss.hasPermi('main:student:remove')")
    @Log(title = "学生信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hmsStudentService.deleteHmsStudentByIds(ids));
    }
}

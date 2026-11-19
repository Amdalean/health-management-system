package com.hms.main.course.controller;

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
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.main.course.domain.HmsCourse;
import com.hms.main.course.domain.HmsStudentCourse;
import com.hms.main.course.service.IHmsCourseService;
import com.hms.main.student.domain.HmsStudent;

/**
 * 课程建档Controller
 *
 * @author 崔永琦
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 按论文表3新建课程建档模块，提供课程增删改查、导出，
 *   以及学生选课/退课管理（论文表5 学生课程分配表）的接口。
 */
@RestController
@RequestMapping("/main/course")
public class HmsCourseController extends BaseController
{
    @Autowired
    private IHmsCourseService hmsCourseService;

    /**
     * 查询课程建档列表
     */
    @PreAuthorize("@ss.hasPermi('main:course:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmsCourse hmsCourse)
    {
        startPage();
        List<HmsCourse> list = hmsCourseService.selectHmsCourseList(hmsCourse);
        return getDataTable(list);
    }

    /**
     * 课程下拉选项（供成绩分析等模块选择课程）
     */
    @PreAuthorize("@ss.hasPermi('main:course:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(hmsCourseService.selectHmsCourseList(new HmsCourse()));
    }

    /**
     * 导出课程建档列表
     */
    @PreAuthorize("@ss.hasPermi('main:course:export')")
    @Log(title = "课程信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HmsCourse hmsCourse)
    {
        List<HmsCourse> list = hmsCourseService.selectHmsCourseList(hmsCourse);
        ExcelUtil<HmsCourse> util = new ExcelUtil<HmsCourse>(HmsCourse.class);
        util.exportExcel(response, list, "课程信息数据");
    }

    /**
     * 获取课程建档详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:course:query')")
    @GetMapping(value = "/{courseId}")
    public AjaxResult getInfo(@PathVariable("courseId") Long courseId)
    {
        return success(hmsCourseService.selectHmsCourseById(courseId));
    }

    /**
     * 新增课程建档
     */
    @PreAuthorize("@ss.hasPermi('main:course:add')")
    @Log(title = "课程信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmsCourse hmsCourse)
    {
        HmsCourse exist = hmsCourseService.selectCourseByNameAndSemester(hmsCourse.getCourseName(), hmsCourse.getSemester());
        if (exist != null)
        {
            return error("新增课程失败，同一学期已存在课程【" + hmsCourse.getCourseName() + "】");
        }
        return toAjax(hmsCourseService.insertHmsCourse(hmsCourse));
    }

    /**
     * 修改课程建档
     */
    @PreAuthorize("@ss.hasPermi('main:course:edit')")
    @Log(title = "课程信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmsCourse hmsCourse)
    {
        HmsCourse exist = hmsCourseService.selectCourseByNameAndSemester(hmsCourse.getCourseName(), hmsCourse.getSemester());
        if (exist != null && !exist.getCourseId().equals(hmsCourse.getCourseId()))
        {
            return error("修改课程失败，同一学期已存在课程【" + hmsCourse.getCourseName() + "】");
        }
        return toAjax(hmsCourseService.updateHmsCourse(hmsCourse));
    }

    /**
     * 删除课程建档
     */
    @PreAuthorize("@ss.hasPermi('main:course:remove')")
    @Log(title = "课程信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{courseIds}")
    public AjaxResult remove(@PathVariable Long[] courseIds)
    {
        return toAjax(hmsCourseService.deleteHmsCourseByIds(courseIds));
    }

    /**
     * 查询某课程已选学生
     */
    @PreAuthorize("@ss.hasPermi('main:course:assign')")
    @GetMapping("/{courseId}/students")
    public AjaxResult students(@PathVariable("courseId") Long courseId)
    {
        return success(hmsCourseService.selectCourseStudents(courseId));
    }

    /**
     * 查询某课程尚未选课的学生（选课候选）
     */
    @PreAuthorize("@ss.hasPermi('main:course:assign')")
    @GetMapping("/{courseId}/candidates")
    public AjaxResult candidates(@PathVariable("courseId") Long courseId)
    {
        List<HmsStudent> list = hmsCourseService.selectUnselectedStudents(courseId);
        return success(list);
    }

    /**
     * 批量选课
     */
    @PreAuthorize("@ss.hasPermi('main:course:assign')")
    @Log(title = "课程信息", businessType = BusinessType.INSERT)
    @PostMapping("/{courseId}/students")
    public AjaxResult assign(@PathVariable("courseId") Long courseId, @RequestBody Long[] studentIds)
    {
        return toAjax(hmsCourseService.assignStudents(courseId, studentIds));
    }

    /**
     * 批量退课
     */
    @PreAuthorize("@ss.hasPermi('main:course:assign')")
    @Log(title = "课程信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{courseId}/students/{studentIds}")
    public AjaxResult removeStudents(@PathVariable("courseId") Long courseId,
                                     @PathVariable("studentIds") Long[] studentIds)
    {
        return toAjax(hmsCourseService.removeStudents(courseId, studentIds));
    }
}

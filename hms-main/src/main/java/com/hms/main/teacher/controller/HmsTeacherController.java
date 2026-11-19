package com.hms.main.teacher.controller;

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
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.main.account.domain.UserGenerateResult;
import com.hms.main.account.service.IAccountGenerateService;
import com.hms.main.teacher.domain.HmsTeacher;
import com.hms.main.teacher.service.IHmsTeacherService;

/**
 * 教师建档Controller
 *
 * @author 崔永琦
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 按论文表2新建教师建档模块，提供教师信息的增删改查、导出，
 *   以及「生成账号」接口（用户名 = 工号，自动分配「教师」角色）。
 */
@RestController
@RequestMapping("/main/teacher")
public class HmsTeacherController extends BaseController
{
    @Autowired
    private IHmsTeacherService hmsTeacherService;

    @Autowired
    private IAccountGenerateService accountGenerateService;

    /**
     * 查询教师信息列表
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmsTeacher hmsTeacher)
    {
        startPage();
        List<HmsTeacher> list = hmsTeacherService.selectHmsTeacherList(hmsTeacher);
        return getDataTable(list);
    }

    /**
     * 导出教师信息列表
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:export')")
    @Log(title = "教师信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HmsTeacher hmsTeacher)
    {
        List<HmsTeacher> list = hmsTeacherService.selectHmsTeacherList(hmsTeacher);
        ExcelUtil<HmsTeacher> util = new ExcelUtil<HmsTeacher>(HmsTeacher.class);
        util.exportExcel(response, list, "教师信息数据");
    }

    /**
     * 获取教师信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:query')")
    @GetMapping(value = "/{teacherId}")
    public AjaxResult getInfo(@PathVariable("teacherId") Long teacherId)
    {
        return success(hmsTeacherService.selectHmsTeacherById(teacherId));
    }

    /**
     * 新增教师信息
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:add')")
    @Log(title = "教师信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmsTeacher hmsTeacher)
    {
        if (hmsTeacherService.selectHmsTeacherByNo(hmsTeacher.getStaffNo()) != null)
        {
            return error("新增教师失败，教师工号【" + hmsTeacher.getStaffNo() + "】已存在");
        }
        return toAjax(hmsTeacherService.insertHmsTeacher(hmsTeacher));
    }

    /**
     * 修改教师信息
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:edit')")
    @Log(title = "教师信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmsTeacher hmsTeacher)
    {
        return toAjax(hmsTeacherService.updateHmsTeacher(hmsTeacher));
    }

    /**
     * 删除教师信息
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:remove')")
    @Log(title = "教师信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{teacherIds}")
    public AjaxResult remove(@PathVariable Long[] teacherIds)
    {
        return toAjax(hmsTeacherService.deleteHmsTeacherByIds(teacherIds));
    }

    /**
     * 为教师生成系统账号（单条）
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:generateUser')")
    @Log(title = "教师信息", businessType = BusinessType.INSERT)
    @PostMapping("/generateUser/{teacherId}")
    public AjaxResult generateUser(@PathVariable("teacherId") Long teacherId)
    {
        Long userId = accountGenerateService.generateTeacherAccount(teacherId);
        return success("账号生成成功，系统用户ID：" + userId);
    }

    /**
     * 批量为教师生成系统账号
     */
    @PreAuthorize("@ss.hasPermi('main:teacher:generateUser')")
    @Log(title = "教师信息", businessType = BusinessType.INSERT)
    @PostMapping("/generateUser/batch")
    public AjaxResult generateUserBatch(@RequestBody Long[] teacherIds)
    {
        UserGenerateResult result = accountGenerateService.generateTeacherAccounts(teacherIds);
        return success(result);
    }
}

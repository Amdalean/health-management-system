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
import com.hms.main.account.domain.UserGenerateResult;
import com.hms.main.account.service.IAccountGenerateService;
import com.hms.main.student.domain.HmsStudent;
import com.hms.main.student.service.IHmsStudentService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 学生建档Controller
 *
 * @author 崔永琦
 * @date 2024-09-13
 * add by CYQ 2026-10-18 说明: 按论文表1对齐学生建档模块，新增「生成账号」接口
 *   （单条 generateUser/{studentId}、批量 generateUser/batch），
 *   用于依据学号为学生开通系统账号并回写 user_id。
 */
@RestController
@RequestMapping("/main/student")
public class HmsStudentController extends BaseController
{
    @Autowired
    private IHmsStudentService hmsStudentService;

    @Autowired
    private IAccountGenerateService accountGenerateService;

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
        if (hmsStudentService.selectHmsStudentByNo(hmsStudent.getStudentNo()) != null)
        {
            return error("新增学生失败，学号【" + hmsStudent.getStudentNo() + "】已存在");
        }
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

    /**
     * 为学生生成系统账号（单条）
     * add by CYQ 2026-10-18 说明: 用户名 = 学号，初始密码取系统配置 sys.user.initPassword
     */
    @PreAuthorize("@ss.hasPermi('main:student:generateUser')")
    @Log(title = "学生信息", businessType = BusinessType.INSERT)
    @PostMapping("/generateUser/{studentId}")
    public AjaxResult generateUser(@PathVariable("studentId") Long studentId)
    {
        Long userId = accountGenerateService.generateStudentAccount(studentId);
        return success("账号生成成功，系统用户ID：" + userId);
    }

    /**
     * 批量为学生生成系统账号
     * add by CYQ 2026-10-18 说明: 逐条独立事务，允许部分成功，返回成功/失败统计
     */
    @PreAuthorize("@ss.hasPermi('main:student:generateUser')")
    @Log(title = "学生信息", businessType = BusinessType.INSERT)
    @PostMapping("/generateUser/batch")
    public AjaxResult generateUserBatch(@RequestBody Long[] studentIds)
    {
        UserGenerateResult result = accountGenerateService.generateStudentAccounts(studentIds);
        return success(result);
    }
}

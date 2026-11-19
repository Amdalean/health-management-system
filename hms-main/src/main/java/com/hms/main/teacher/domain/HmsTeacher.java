package com.hms.main.teacher.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 教师建档对象 hms_teacher
 *
 * @author 崔永琦
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 按论文表2「教师建档表 teacher」建立教师实体，
 *   字段为 teacher_id / staff_no / name / gender / title / phone / hire_date / department，
 *   并新增 userId 用于关联「生成账号」后创建的系统用户（sys_user.user_id）。
 */
@Data
public class HmsTeacher extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 教师ID（主键） */
    private Long teacherId;

    /** 教师工号 */
    @Excel(name = "教师工号")
    private String staffNo;

    /** 姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 性别（0=男 1=女 2=未知） */
    @Excel(name = "性别", readConverterExp = "0=男,1=女,2=未知")
    private String gender;

    /** 职称 */
    @Excel(name = "职称")
    private String title;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 入职时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "入职时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date hireDate;

    /** 所属部门 */
    @Excel(name = "所属部门")
    private String department;

    /** 关联系统用户ID（生成账号后回写） */
    @Excel(name = "系统账号ID")
    private Long userId;

    /** 账号生成状态筛选（0=未生成系统账号 1=已生成），仅作为列表查询条件，不持久化 */
    private Integer hasAccount;
}

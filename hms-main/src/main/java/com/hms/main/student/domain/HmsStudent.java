package com.hms.main.student.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 学生建档对象 hms_student
 *
 * @author CYQ
 * @date 2024-09-13
 * add by CYQ 2026-10-18 说明: 按论文表1「学生建档表 student」重建实体，
 *   字段与数据库 hms_student 保持一致（student_id / student_no / name / gender /
 *   birth_date / major / grade / phone / enrollment_date），
 *   并新增 userId 用于关联「生成账号」后创建的系统用户（sys_user.user_id）。
 */
@Data
public class HmsStudent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学生ID（主键） */
    private Long studentId;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 性别（0=男 1=女 2=未知） */
    @Excel(name = "性别", readConverterExp = "0=男,1=女,2=未知")
    private String gender;

    /** 出生日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date birthDate;

    /** 专业 */
    @Excel(name = "专业")
    private String major;

    /** 年级 */
    @Excel(name = "年级")
    private String grade;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 入学时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "入学时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date enrollmentDate;

    /** 关联系统用户ID（生成账号后回写） */
    @Excel(name = "系统账号ID")
    private Long userId;

    /** 账号生成状态筛选（0=未生成系统账号 1=已生成），仅作为列表查询条件，不持久化 */
    private Integer hasAccount;
}

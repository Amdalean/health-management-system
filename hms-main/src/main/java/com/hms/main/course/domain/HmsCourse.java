package com.hms.main.course.domain;

import lombok.Data;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 课程建档对象 hms_course
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 按论文表3「课程建档表 course」建立课程实体，
 *   字段为 course_id / course_name / teacher_id / credit / hours / semester，
 *   并补充 teacherName（授课教师姓名）、studentCount（选课人数）两个展示字段。
 */
@Data
public class HmsCourse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 课程ID（主键） */
    private Long courseId;

    /** 课程名称 */
    @Excel(name = "课程名称")
    private String courseName;

    /** 授课教师ID（hms_teacher.teacher_id） */
    private Long teacherId;

    /** 授课教师姓名（关联查询，非持久化） */
    @Excel(name = "授课教师")
    private String teacherName;

    /** 课程学分 */
    @Excel(name = "学分")
    private Integer credit;

    /** 学时 */
    @Excel(name = "学时")
    private Integer hours;

    /** 学期 */
    @Excel(name = "学期")
    private String semester;

    /** 选课人数（关联统计，非持久化） */
    @Excel(name = "选课人数")
    private Integer studentCount;
}

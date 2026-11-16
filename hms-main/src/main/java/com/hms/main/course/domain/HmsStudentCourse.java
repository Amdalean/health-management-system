package com.hms.main.course.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 学生选课（学生课程分配）对象 hms_student_course
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 按论文表5「学生课程分配表 Student_Course_Assignment」建模，
 *   字段为 assignment_id / student_id / course_id / assign_time；
 *   另带 studentNo、studentName、grade、major 等展示字段，供「已选学生」列表使用。
 */
@Data
public class HmsStudentCourse
{
    /** 分配ID（主键） */
    private Long assignmentId;

    /** 学生ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 选课（分配）时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date assignTime;

    /** 学号（关联查询，非持久化） */
    private String studentNo;

    /** 学生姓名（关联查询，非持久化） */
    private String studentName;

    /** 年级（关联查询，非持久化） */
    private String grade;

    /** 专业（关联查询，非持久化） */
    private String major;
}

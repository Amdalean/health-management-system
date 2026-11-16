package com.hms.main.course.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.hms.main.course.domain.HmsCourse;
import com.hms.main.course.domain.HmsStudentCourse;
import com.hms.main.student.domain.HmsStudent;

/**
 * 课程建档 Mapper 接口
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 课程建档 + 学生选课数据层。
 *   课程列表关联 hms_teacher 取授课教师姓名、关联 hms_student_course 统计选课人数。
 */
public interface HmsCourseMapper
{
    /**
     * 查询课程建档列表
     *
     * @param hmsCourse 课程信息
     * @return 课程集合
     */
    public List<HmsCourse> selectHmsCourseList(HmsCourse hmsCourse);

    /**
     * 查询课程建档详情
     *
     * @param courseId 课程ID
     * @return 课程信息
     */
    public HmsCourse selectHmsCourseById(Long courseId);

    /**
     * 按课程名称 + 学期查询（唯一性校验，学期可为空）
     *
     * @param courseName 课程名称
     * @param semester   学期
     * @return 课程信息
     */
    public HmsCourse selectCourseByNameAndSemester(@Param("courseName") String courseName,
                                                   @Param("semester") String semester);

    /**
     * 新增课程建档
     *
     * @param hmsCourse 课程信息
     * @return 结果
     */
    public int insertHmsCourse(HmsCourse hmsCourse);

    /**
     * 修改课程建档
     *
     * @param hmsCourse 课程信息
     * @return 结果
     */
    public int updateHmsCourse(HmsCourse hmsCourse);

    /**
     * 删除课程建档
     *
     * @param courseId 课程ID
     * @return 结果
     */
    public int deleteHmsCourseById(Long courseId);

    /**
     * 批量删除课程建档
     *
     * @param courseIds 课程ID集合
     * @return 结果
     */
    public int deleteHmsCourseByIds(Long[] courseIds);

    /**
     * 查询某课程已选学生
     *
     * @param courseId 课程ID
     * @return 选课记录集合
     */
    public List<HmsStudentCourse> selectCourseStudents(Long courseId);

    /**
     * 查询某课程尚未选课的学生
     *
     * @param courseId 课程ID
     * @return 学生集合
     */
    public List<HmsStudent> selectUnselectedStudents(Long courseId);

    /**
     * 批量选课
     *
     * @param courseId   课程ID
     * @param studentIds 学生ID集合
     * @return 结果
     */
    public int insertStudentCourse(@Param("courseId") Long courseId,
                                   @Param("studentIds") Long[] studentIds);

    /**
     * 批量退课
     *
     * @param courseId   课程ID
     * @param studentIds 学生ID集合
     * @return 结果
     */
    public int deleteStudentCourse(@Param("courseId") Long courseId,
                                   @Param("studentIds") Long[] studentIds);

    /**
     * 按课程ID清理选课记录（删除课程时调用）
     *
     * @param courseIds 课程ID集合
     * @return 结果
     */
    public int deleteStudentCourseByCourseIds(Long[] courseIds);
}

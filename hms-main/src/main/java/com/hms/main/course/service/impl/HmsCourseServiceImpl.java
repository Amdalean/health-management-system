package com.hms.main.course.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hms.main.course.domain.HmsCourse;
import com.hms.main.course.domain.HmsStudentCourse;
import com.hms.main.course.mapper.HmsCourseMapper;
import com.hms.main.course.service.IHmsCourseService;
import com.hms.main.student.domain.HmsStudent;

/**
 * 课程建档 Service 业务层处理
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 课程建档与选课业务层实现。
 *   删除课程时级联清理 hms_student_course 中的选课记录，避免脏数据。
 */
@Service
public class HmsCourseServiceImpl implements IHmsCourseService
{
    @Autowired
    private HmsCourseMapper hmsCourseMapper;

    @Override
    public List<HmsCourse> selectHmsCourseList(HmsCourse hmsCourse)
    {
        return hmsCourseMapper.selectHmsCourseList(hmsCourse);
    }

    @Override
    public HmsCourse selectHmsCourseById(Long courseId)
    {
        return hmsCourseMapper.selectHmsCourseById(courseId);
    }

    @Override
    public HmsCourse selectCourseByNameAndSemester(String courseName, String semester)
    {
        return hmsCourseMapper.selectCourseByNameAndSemester(courseName, semester);
    }

    @Override
    public int insertHmsCourse(HmsCourse hmsCourse)
    {
        return hmsCourseMapper.insertHmsCourse(hmsCourse);
    }

    @Override
    public int updateHmsCourse(HmsCourse hmsCourse)
    {
        return hmsCourseMapper.updateHmsCourse(hmsCourse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHmsCourseByIds(Long[] courseIds)
    {
        hmsCourseMapper.deleteStudentCourseByCourseIds(courseIds);
        return hmsCourseMapper.deleteHmsCourseByIds(courseIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHmsCourseById(Long courseId)
    {
        hmsCourseMapper.deleteStudentCourseByCourseIds(new Long[] { courseId });
        return hmsCourseMapper.deleteHmsCourseById(courseId);
    }

    @Override
    public List<HmsStudentCourse> selectCourseStudents(Long courseId)
    {
        return hmsCourseMapper.selectCourseStudents(courseId);
    }

    @Override
    public List<HmsStudent> selectUnselectedStudents(Long courseId)
    {
        return hmsCourseMapper.selectUnselectedStudents(courseId);
    }

    @Override
    public int assignStudents(Long courseId, Long[] studentIds)
    {
        if (studentIds == null || studentIds.length == 0)
        {
            return 0;
        }
        return hmsCourseMapper.insertStudentCourse(courseId, studentIds);
    }

    @Override
    public int removeStudents(Long courseId, Long[] studentIds)
    {
        if (studentIds == null || studentIds.length == 0)
        {
            return 0;
        }
        return hmsCourseMapper.deleteStudentCourse(courseId, studentIds);
    }
}

package com.hms.main.score.service;

import java.util.List;
import com.hms.main.course.domain.HmsCourse;
import com.hms.main.score.domain.HmsScoreRecord;
import com.hms.main.student.domain.HmsStudent;

/**
 * 成绩智能分析 Service 接口
 *
 * @author 崔永琦
 * @date 2026-03-01
 * add by CYQ 2026-10-18 说明: 与学生建档、课程建档、学生选课联动，
 *   新增学生/课程下拉与"我的课程"查询接口。
 */
public interface IHmsScoreRecordService
{
    /**
     * 查询成绩智能分析
     *
     * @param id 成绩智能分析主键
     * @return 成绩智能分析
     */
    public HmsScoreRecord selectHmsScoreRecordById(Long id);

    /**
     * 查询成绩智能分析列表
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 成绩智能分析集合
     */
    public List<HmsScoreRecord> selectHmsScoreRecordList(HmsScoreRecord hmsScoreRecord);

    /**
     * 新增成绩智能分析
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 结果
     */
    public int insertHmsScoreRecord(HmsScoreRecord hmsScoreRecord);

    /**
     * 修改成绩智能分析
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 结果
     */
    public int updateHmsScoreRecord(HmsScoreRecord hmsScoreRecord);

    /**
     * 批量删除成绩智能分析
     *
     * @param ids 需要删除的成绩智能分析主键集合
     * @return 结果
     */
    public int deleteHmsScoreRecordByIds(Long[] ids);

    /**
     * 删除成绩智能分析信息
     *
     * @param id 成绩智能分析主键
     * @return 结果
     */
    public int deleteHmsScoreRecordById(Long id);

    /**
     * 根据任务 ID 查询成绩智能分析
     *
     * @param taskId 任务 ID
     * @return 成绩智能分析
     */
    public HmsScoreRecord selectHmsScoreRecordByTaskId(String taskId);

    /**
     * 提交视频进行 AI 分析
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 结果
     */
    public HmsScoreRecord submitForAnalysis(HmsScoreRecord hmsScoreRecord);

    /**
     * 学生下拉选项（学号 + 姓名）
     *
     * @return 学生集合
     */
    public List<HmsStudent> selectStudentOptions();

    /**
     * 查询某学生已选课程（studentId 为空时返回全部课程）
     *
     * @param studentId 学生ID
     * @return 课程集合
     */
    public List<HmsCourse> selectCourseOptions(Long studentId);

    /**
     * 查询当前登录用户可选的课程
     * 学生账号：返回其已选课程；无关联学生档案的账号：返回全部课程
     *
     * @return 课程集合
     */
    public List<HmsCourse> selectMyCourses();
}

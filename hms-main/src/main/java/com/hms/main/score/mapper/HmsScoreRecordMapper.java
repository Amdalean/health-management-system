package com.hms.main.score.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.hms.main.course.domain.HmsCourse;
import com.hms.main.score.domain.HmsScoreRecord;
import com.hms.main.student.domain.HmsStudent;

/**
 * 成绩智能分析 Mapper 接口
 *
 * @author CYQ
 * @date 2026-03-01
 * add by CYQ 2026-10-18 说明: 与学生建档、课程建档、学生选课联动，
 *   补充「学生下拉」「按学生查已选课程」「校验选课关系」三个查询。
 */
public interface HmsScoreRecordMapper
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
     * 根据任务 ID 查询成绩智能分析
     *
     * @param taskId 任务 ID
     * @return 成绩智能分析
     */
    public HmsScoreRecord selectHmsScoreRecordByTaskId(String taskId);

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
     * 提交分析前置重置：状态置为分析中，并清空上一轮的评分/评语/分析详情
     * add by CYQ 2026-11-15 说明: score/feedback/analysis_details 允许写 null，
     * 避免重新提交时旧结果残留（updateHmsScoreRecord 的动态 if 不会写 null）
     *
     * @param hmsScoreRecord 成绩智能分析（需要 id、status、taskId）
     * @return 结果
     */
    public int resetForAnalysis(HmsScoreRecord hmsScoreRecord);

    /**
     * 删除成绩智能分析
     *
     * @param id 成绩智能分析主键
     * @return 结果
     */
    public int deleteHmsScoreRecordById(Long id);

    /**
     * 批量删除成绩智能分析
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHmsScoreRecordByIds(Long[] ids);

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
    public List<HmsCourse> selectCourseOptionsByStudent(@Param("studentId") Long studentId);

    /**
     * 统计学生与课程的选课关系（用于提交成绩分析前的联动校验）
     *
     * @param studentId 学生ID
     * @param courseId  课程ID
     * @return 记录数，0 表示未选课
     */
    public int countStudentCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}

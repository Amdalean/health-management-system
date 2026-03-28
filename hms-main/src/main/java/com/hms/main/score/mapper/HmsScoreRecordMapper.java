package com.hms.main.score.mapper;

import java.util.List;
import com.hms.main.score.domain.HmsScoreRecord;

/**
 * 成绩分析记录 Mapper 接口
 *
 * @author CYQ
 * @date 2026-03-01
 */
public interface HmsScoreRecordMapper
{
    /**
     * 查询成绩分析记录
     *
     * @param id 成绩分析记录主键
     * @return 成绩分析记录
     */
    public HmsScoreRecord selectHmsScoreRecordById(Long id);

    /**
     * 查询成绩分析记录列表
     *
     * @param hmsScoreRecord 成绩分析记录
     * @return 成绩分析记录集合
     */
    public List<HmsScoreRecord> selectHmsScoreRecordList(HmsScoreRecord hmsScoreRecord);

    /**
     * 根据任务 ID 查询成绩分析记录
     *
     * @param taskId 任务 ID
     * @return 成绩分析记录
     */
    public HmsScoreRecord selectHmsScoreRecordByTaskId(String taskId);

    /**
     * 新增成绩分析记录
     *
     * @param hmsScoreRecord 成绩分析记录
     * @return 结果
     */
    public int insertHmsScoreRecord(HmsScoreRecord hmsScoreRecord);

    /**
     * 修改成绩分析记录
     *
     * @param hmsScoreRecord 成绩分析记录
     * @return 结果
     */
    public int updateHmsScoreRecord(HmsScoreRecord hmsScoreRecord);

    /**
     * 删除成绩分析记录
     *
     * @param id 成绩分析记录主键
     * @return 结果
     */
    public int deleteHmsScoreRecordById(Long id);

    /**
     * 批量删除成绩分析记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHmsScoreRecordByIds(Long[] ids);
}

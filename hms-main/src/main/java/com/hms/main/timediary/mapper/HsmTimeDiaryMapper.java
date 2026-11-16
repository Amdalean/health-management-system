package com.hms.main.timediary.mapper;

import java.util.List;
import java.util.Map;
import com.hms.main.timediary.domain.HsmTimeDiary;

/**
 * 时间管理局Mapper接口
 *
 * add by CYQ 2026年8月4日
 */
public interface HsmTimeDiaryMapper
{
    /**
     * 查询时间管理局
     *
     * @param diaryId 时间管理局主键
     * @return 时间管理局
     */
    public HsmTimeDiary selectHsmTimeDiaryById(Long diaryId);

    /**
     * 查询时间管理局列表
     *
     * @param hsmTimeDiary 时间管理局
     * @return 时间管理局集合
     */
    public List<HsmTimeDiary> selectHsmTimeDiaryList(HsmTimeDiary hsmTimeDiary);

    /**
     * 新增时间管理局
     *
     * @param hsmTimeDiary 时间管理局
     * @return 结果
     */
    public int insertHsmTimeDiary(HsmTimeDiary hsmTimeDiary);

    /**
     * 修改时间管理局
     *
     * @param hsmTimeDiary 时间管理局
     * @return 结果
     */
    public int updateHsmTimeDiary(HsmTimeDiary hsmTimeDiary);

    /**
     * 删除时间管理局
     *
     * @param diaryId 时间管理局主键
     * @return 结果
     */
    public int deleteHsmTimeDiaryById(Long diaryId);

    /**
     * 批量删除时间管理局
     *
     * @param diaryIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHsmTimeDiaryByIds(Long[] diaryIds);

    /**
     * 按分类统计时长
     *
     * @param hsmTimeDiary 查询条件
     * @return List<Map> {category, totalMinutes}
     */
    public List<Map<String, Object>> selectTimeDiaryCategoryStats(HsmTimeDiary hsmTimeDiary);

    /**
     * 按日期统计总时长
     *
     * @param hsmTimeDiary 查询条件
     * @return List<Map> {diaryDate, totalMinutes}
     */
    public List<Map<String, Object>> selectTimeDiaryDailyStats(HsmTimeDiary hsmTimeDiary);

    /**
     * 按日期和分类统计时长（用于堆叠柱状图）
     *
     * @param hsmTimeDiary 查询条件
     * @return List<Map> {diaryDate, category, totalMinutes}
     */
    public List<Map<String, Object>> selectTimeDiaryDailyCategoryStats(HsmTimeDiary hsmTimeDiary);

    /**
     * 查询某天的时间管理局列表（用于时间轴展示）
     *
     * @param hsmTimeDiary 查询条件（diaryDate 必填）
     * @return 时间管理局集合
     */
    public List<HsmTimeDiary> selectTimeDiaryByDate(HsmTimeDiary hsmTimeDiary);
}

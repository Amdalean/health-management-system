package com.hms.mobile.clockin.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.hms.mobile.clockin.domain.ClockinRecord;

/**
 * 打卡记录 数据层
 * add by CYQ 2026-06-05 说明: 移动端打卡记录数据访问接口
 */
public interface ClockinRecordMapper {

    public ClockinRecord selectClockinRecordById(Long id);

    public List<ClockinRecord> selectClockinRecordList(ClockinRecord record);

    public int insertClockinRecord(ClockinRecord record);

    public int updateClockinRecord(ClockinRecord record);

    public int deleteClockinRecordById(Long id);

    /**
     * 打卡统计：按日期汇总考勤用户的打卡人数
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectClockinStatistics(@Param("beginDate") String beginDate, @Param("endDate") String endDate);

    /**
     * 查询考勤用户总数
     * add by CYQ 2026-06-10
     */
    public int selectAttendanceUserCount();

    /**
     * 查询指定日期已打卡的考勤用户明细（按用户分组，每人一条）
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectClockedInUsersByDate(@Param("date") String date);

    /**
     * 查询指定日期未打卡的考勤用户
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectNotClockedInUsersByDate(@Param("date") String date);

    /**
     * 查询指定用户指定日期的全部打卡记录（用于展开明细）
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectUserClockinRecordsByDate(@Param("userId") Long userId, @Param("date") String date);
}

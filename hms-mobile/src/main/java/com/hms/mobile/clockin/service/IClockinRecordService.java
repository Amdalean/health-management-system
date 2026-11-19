package com.hms.mobile.clockin.service;

import java.util.List;
import java.util.Map;
import com.hms.mobile.clockin.domain.ClockinRecord;

/**
 * 打卡记录 服务层
 * add by CYQ 2026-06-05 说明: 移动端打卡记录业务接口
 * @author 崔永琦
 */
public interface IClockinRecordService {

    public ClockinRecord selectClockinRecordById(Long id);

    public List<ClockinRecord> selectClockinRecordList(ClockinRecord record);

    public int insertClockinRecord(ClockinRecord record);

    public int updateClockinRecord(ClockinRecord record);

    public int deleteClockinRecordById(Long id);

    /**
     * 是否启用内勤外勤区分（读取参数 mobile.clockin.enableOfficeType）
     * add by CYQ 2026-08-13
     */
    public boolean isOfficeTypeEnabled();

    /**
     * 打卡统计：按日期汇总考勤用户的打卡人数
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectClockinStatistics(String beginDate, String endDate);

    /**
     * 查询考勤用户总数
     * add by CYQ 2026-06-10
     */
    public int selectAttendanceUserCount();

    /**
     * 查询指定日期已打卡的考勤用户明细（按用户分组，每人一条）
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectClockedInUsersByDate(String date);

    /**
     * 查询指定日期未打卡的考勤用户
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectNotClockedInUsersByDate(String date);

    /**
     * 查询指定用户指定日期的全部打卡记录（用于展开明细）
     * add by CYQ 2026-06-10
     */
    public List<Map<String, Object>> selectUserClockinRecordsByDate(Long userId, String date);

    /**
     * 查询指定日期是否为工作日（调用互联网节假日API）
     * add by CYQ 2026-06-10
     */
    public Map<String, Object> queryWorkdayStatus(String date);
}

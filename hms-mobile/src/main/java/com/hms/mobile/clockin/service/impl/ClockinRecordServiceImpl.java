package com.hms.mobile.clockin.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.hms.mobile.common.util.GeoUtils;
import com.hms.mobile.clockin.domain.ClockinRecord;
import com.hms.mobile.clockin.mapper.ClockinRecordMapper;
import com.hms.mobile.clockin.service.IClockinRecordService;
import com.hms.mobile.officelocation.domain.OfficeLocation;
import com.hms.mobile.officelocation.service.IOfficeLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 打卡记录 服务层实现
 * add by CYQ 2026-06-05 说明: 移动端打卡记录业务实现
 */
@Service
public class ClockinRecordServiceImpl implements IClockinRecordService {

    private static final Logger log = LoggerFactory.getLogger(ClockinRecordServiceImpl.class);

    @Autowired
    private ClockinRecordMapper clockinRecordMapper;

    @Autowired
    private IOfficeLocationService officeLocationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public ClockinRecord selectClockinRecordById(Long id) {
        return clockinRecordMapper.selectClockinRecordById(id);
    }

    @Override
    public List<ClockinRecord> selectClockinRecordList(ClockinRecord record) {
        return clockinRecordMapper.selectClockinRecordList(record);
    }

    @Override
    public int insertClockinRecord(ClockinRecord record) {
        if (isOfficeTypeEnabled() && record.getLatitude() != null && record.getLongitude() != null) {
            List<OfficeLocation> locations = officeLocationService.selectAllEnabledLocations();
            boolean isOffice = false;
            for (OfficeLocation loc : locations) {
                if (loc.getLatitude() != null && loc.getLongitude() != null && loc.getRadius() != null) {
                    double dist = GeoUtils.distance(
                            record.getLatitude(), record.getLongitude(),
                            loc.getLatitude(), loc.getLongitude());
                    if (dist <= loc.getRadius()) {
                        isOffice = true;
                        break;
                    }
                }
            }
            record.setClockinType(isOffice ? "内勤" : "外勤");
        }
        return clockinRecordMapper.insertClockinRecord(record);
    }

    /**
     * 是否启用内勤外勤区分，读取参数 mobile.clockin.enableOfficeType，默认启用
     * add by CYQ 2026-08-13
     */
    @Override
    public boolean isOfficeTypeEnabled() {
        try {
            String value = jdbcTemplate.queryForObject(
                "SELECT config_value FROM sys_config WHERE config_key = 'mobile.clockin.enableOfficeType'",
                String.class);
            return !"false".equalsIgnoreCase(value);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public int updateClockinRecord(ClockinRecord record) {
        return clockinRecordMapper.updateClockinRecord(record);
    }

    @Override
    public int deleteClockinRecordById(Long id) {
        return clockinRecordMapper.deleteClockinRecordById(id);
    }

    @Override
    public List<Map<String, Object>> selectClockinStatistics(String beginDate, String endDate) {
        return clockinRecordMapper.selectClockinStatistics(beginDate, endDate);
    }

    @Override
    public int selectAttendanceUserCount() {
        return clockinRecordMapper.selectAttendanceUserCount();
    }

    @Override
    public List<Map<String, Object>> selectClockedInUsersByDate(String date) {
        return clockinRecordMapper.selectClockedInUsersByDate(date);
    }

    @Override
    public List<Map<String, Object>> selectNotClockedInUsersByDate(String date) {
        return clockinRecordMapper.selectNotClockedInUsersByDate(date);
    }

    @Override
    public List<Map<String, Object>> selectUserClockinRecordsByDate(Long userId, String date) {
        return clockinRecordMapper.selectUserClockinRecordsByDate(userId, date);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> queryWorkdayStatus(String date) {
        log.info("工作日查询接口缓存未命中, date={}", date);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("isWorkday", false);
        result.put("typeName", "未知");
        result.put("holidayName", "");

        try {
            String apiUrl = "http://timor.tech/api/holiday/info/" + date;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "HMS/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String rawResponse = sb.toString();
                log.info("工作日查询接口原始响应: {}", rawResponse);

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> respMap = mapper.readValue(rawResponse, Map.class);
                if (respMap != null && Integer.valueOf(0).equals(respMap.get("code"))) {
                    Map<String, Object> typeMap = (Map<String, Object>) respMap.get("type");
                    if (typeMap != null) {
                        Integer type = (Integer) typeMap.get("type");
                        String name = (String) typeMap.get("name");
                        result.put("typeName", name);
                        if (type != null && (type == 0 || type == 3)) {
                            result.put("isWorkday", true);
                        }
                    }
                    Map<String, Object> holiday = (Map<String, Object>) respMap.get("holiday");
                    if (holiday != null && holiday.get("name") != null) {
                        result.put("holidayName", holiday.get("name"));
                    }
                }
            } else {
                log.warn("工作日查询接口HTTP状态码: {}, date={}", code, date);
                // HTTP 非200（403/429等），使用本地工作日判断做兜底
                fallbackToLocalWeekday(date, result);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.warn("查询工作日状态失败, date={}, error={}", date, e.getMessage());
            fallbackToLocalWeekday(date, result);
        }
        log.info("工作日查询接口调用成功, date={}, result={}", date, result);
        return result;
    }

    /** 本地工作日兜底判断（周一至周五为工作日）add by CYQ 2026-06-16 */
    private void fallbackToLocalWeekday(String date, Map<String, Object> result) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(date));
            int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
            if (dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY) {
                result.put("isWorkday", true);
                result.put("typeName", "工作日(本地)");
            } else {
                result.put("typeName", "休息日(本地)");
            }
        } catch (Exception ex) {
            log.error("本地工作日判断失败", ex);
        }
    }
}

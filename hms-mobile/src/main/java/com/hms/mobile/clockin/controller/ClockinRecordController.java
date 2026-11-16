package com.hms.mobile.clockin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.mobile.clockin.domain.ClockinRecord;
import com.hms.mobile.clockin.service.IClockinRecordService;
import com.hms.mobile.officelocation.domain.OfficeLocation;
import com.hms.mobile.officelocation.service.IOfficeLocationService;
import com.hms.mobile.push.domain.PushDevice;
import com.hms.mobile.push.service.IPushDeviceService;

/**
 * 打卡记录Controller (移动端专用)
 * add by CYQ 2026-06-05 说明: 提供移动端打卡上报、查询等接口
 */
@RestController
@RequestMapping("/mobile/clockin")
public class ClockinRecordController extends BaseController {

    @Autowired
    private IClockinRecordService clockinRecordService;

    @Autowired
    private IOfficeLocationService officeLocationService;

    @Autowired
    private IPushDeviceService pushDeviceService;

    /** 工作日查询缓存，避免重复请求外部API */
    private final ConcurrentHashMap<String, Map<String, Object>> workdayCache = new ConcurrentHashMap<>();

    @GetMapping("/officeLocations")
    public AjaxResult officeLocations() {
        List<OfficeLocation> list = officeLocationService.selectAllEnabledLocations();
        Map<String, Object> data = new HashMap<>();
        data.put("enableOfficeType", clockinRecordService.isOfficeTypeEnabled());
        data.put("locations", list);
        return success(data);
    }

    @PostMapping
    public AjaxResult clockin(@RequestBody ClockinRecord record) {
        LoginUser loginUser;
        try {
            loginUser = SecurityUtils.getLoginUser();
            record.setUserId(loginUser.getUserId());
            record.setUserName(loginUser.getUsername());
            record.setCreateBy(loginUser.getUsername());
            if (loginUser.getUser() != null) {
                record.setNickName(loginUser.getUser().getNickName());
                if (loginUser.getUser().getDept() != null) {
                    record.setDeptName(loginUser.getUser().getDept().getDeptName());
                }
            }
            record.setDeptId(loginUser.getDeptId());
        } catch (Exception e) {
            return error("请先登录后再打卡");
        }
        int result = clockinRecordService.insertClockinRecord(record);
        if (result > 0) {
            // 同步微信小程序 openid 到推送设备表
            // add by CYQ 2026-06-15
            syncWxOpenid(record.getOpenid(), loginUser);

            Map<String, Object> data = new HashMap<>();
            data.put("id", record.getId());
            data.put("clockinType", record.getClockinType());
            return success(data);
        } else {
            return error("打卡失败");
        }
    }

    /**
     * 将微信小程序 openid 同步到推送设备表
     * 如果该用户没有推送设备记录，或最新记录的 openid 与当前不同，则新增一条
     * add by CYQ 2026-06-15
     */
    private void syncWxOpenid(String openid, LoginUser loginUser) {
        if (openid == null || openid.isEmpty()) {
            return;
        }
        try {
            PushDevice query = new PushDevice();
            query.setUserId(loginUser.getUserId());
            List<PushDevice> devices = pushDeviceService.selectPushDeviceList(query);

            boolean needInsert = devices.isEmpty();
            if (!needInsert) {
                PushDevice latest = devices.get(0);
                String existingOpenid = latest.getWxOpenid();
                needInsert = (existingOpenid == null || !existingOpenid.equals(openid));
            }

            if (needInsert) {
                PushDevice device = new PushDevice();
                device.setUserId(loginUser.getUserId());
                device.setPlatform("wx_miniapp");
                device.setWxOpenid(openid);
                pushDeviceService.insertPushDevice(device);
            }
        } catch (Exception e) {
            // 同步失败不影响打卡结果
        }
    }

    @GetMapping("/list")
    public AjaxResult list() {
        try {
            Long userId = SecurityUtils.getLoginUser().getUserId();
            ClockinRecord query = new ClockinRecord();
            query.setUserId(userId);
            List<ClockinRecord> list = clockinRecordService.selectClockinRecordList(query);
            return success(list);
        } catch (Exception e) {
            return error("请先登录");
        }
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        ClockinRecord record = clockinRecordService.selectClockinRecordById(id);
        return record != null ? success(record) : error("记录不存在");
    }

    // ========== 管理后台接口 ==========

    @PreAuthorize("@ss.hasPermi('mobile:clockin:export')")
    @Log(title = "打卡记录", businessType = BusinessType.EXPORT)
    @PostMapping("/admin/export")
    public void export(HttpServletResponse response, ClockinRecord record) {
        List<ClockinRecord> list = clockinRecordService.selectClockinRecordList(record);
        ExcelUtil<ClockinRecord> util = new ExcelUtil<ClockinRecord>(ClockinRecord.class);
        util.exportExcel(response, list, "打卡记录数据");
    }

    @PreAuthorize("@ss.hasPermi('mobile:clockin:list')")
    @GetMapping("/admin/list")
    public TableDataInfo adminList(ClockinRecord record) {
        startPage();
        List<ClockinRecord> list = clockinRecordService.selectClockinRecordList(record);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('mobile:clockin:query')")
    @GetMapping("/admin/{id}")
    public AjaxResult adminGetInfo(@PathVariable Long id) {
        return getInfo(id);
    }

    @PreAuthorize("@ss.hasPermi('mobile:clockin:remove')")
    @Log(title = "打卡记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/admin/{ids}")
    public AjaxResult adminRemove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            clockinRecordService.deleteClockinRecordById(id);
        }
        return success();
    }

    /**
     * 打卡统计（含工作日状态）
     * add by CYQ 2026-06-10
     */
    @PreAuthorize("@ss.hasPermi('mobile:clockin:statistics')")
    @GetMapping("/admin/statistics")
    public AjaxResult statistics(
            @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate) {
        List<Map<String, Object>> list = clockinRecordService.selectClockinStatistics(beginDate, endDate);
        int totalAssigned = clockinRecordService.selectAttendanceUserCount();

        // 为每条统计记录附加工作日状态
        for (Map<String, Object> row : list) {
            Object dateObj = row.get("clockinDate");
            String date = dateObj != null ? dateObj.toString() : null;
            if (date != null) {
                Map<String, Object> wd = getWorkdayCached(date);
                row.put("isWorkday", wd.get("isWorkday"));
                row.put("typeName", wd.get("typeName"));
                row.put("holidayName", wd.get("holidayName"));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalAssigned", totalAssigned);
        return success(result);
    }

    /**
     * 查询指定日期的打卡明细
     * add by CYQ 2026-06-10
     */
    @PreAuthorize("@ss.hasPermi('mobile:clockin:statistics')")
    @GetMapping("/admin/dailyDetail")
    public AjaxResult dailyDetail(@RequestParam String date) {
        List<Map<String, Object>> clockedInUsers = clockinRecordService.selectClockedInUsersByDate(date);
        List<Map<String, Object>> notClockedInUsers = clockinRecordService.selectNotClockedInUsersByDate(date);
        Map<String, Object> workdayStatus = getWorkdayCached(date);

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("clockedInUsers", clockedInUsers);
        result.put("notClockedInUsers", notClockedInUsers);
        result.put("isWorkday", workdayStatus.get("isWorkday"));
        result.put("typeName", workdayStatus.get("typeName"));
        result.put("holidayName", workdayStatus.get("holidayName"));
        return success(result);
    }

    /**
     * 查询指定用户指定日期的全部打卡记录（展开明细）
     * add by CYQ 2026-06-10
     */
    @PreAuthorize("@ss.hasPermi('mobile:clockin:statistics')")
    @GetMapping("/admin/userRecords")
    public AjaxResult userRecords(@RequestParam Long userId, @RequestParam String date) {
        List<Map<String, Object>> records = clockinRecordService.selectUserClockinRecordsByDate(userId, date);
        return success(records);
    }

    /** 带缓存的工作日查询，仅缓存API成功返回的结果，避免429等失败结果被持久化 */
    private Map<String, Object> getWorkdayCached(String date) {
        Map<String, Object> cached = workdayCache.get(date);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> result = clockinRecordService.queryWorkdayStatus(date);
        if (!"未知".equals(result.get("typeName"))) {
            workdayCache.putIfAbsent(date, result);
        }
        return result;
    }
}

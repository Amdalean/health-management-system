package com.hms.mobile.location.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.utils.SecurityUtils;
import com.hms.mobile.location.domain.LocationReport;
import com.hms.mobile.location.util.AmapRegeoUtils;
import com.hms.system.service.ISysUserService;

/**
 * 用户所在城市 Controller（移动端专用）
 * add by Qwen 2026-10-26 说明: App 启动时静默上报当前城市，直接写入 sys_user，天气推送按此取数
 */
@RestController
@RequestMapping("/mobile/location")
/**
 * public class UserLocationController extends BaseController {
 *
 * @author 崔永琦
 */
public class UserLocationController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(UserLocationController.class);

    @Autowired
    private ISysUserService userService;

    /**
     * 上报当前所在城市（App 启动/回到前台时静默调用）
     */
    @PostMapping("/report")
    public AjaxResult report(@RequestBody LocationReport report) {
        // 客户端只传经纬度也可以：服务端用高德逆地理补全城市（小程序端不必配高德域名白名单）
        if (report.getAdcode() == null || report.getAdcode().trim().isEmpty()) {
            if (!AmapRegeoUtils.fillByRegeo(report)) {
                return error("定位失败：无法解析当前城市");
            }
        }
        Long userId;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            return error("请先登录后再上报位置");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setCurrentCity(report.getCity());
        user.setCurrentDistrict(report.getDistrict());
        user.setCurrentAdcode(report.getAdcode().trim());
        user.setCurrentLatitude(report.getLatitude());
        user.setCurrentLongitude(report.getLongitude());
        int rows = userService.updateUserLocation(user);
        log.info("用户位置上报 userId={} 城市={}{} adcode={} source={}",
                userId, report.getCity(), report.getDistrict(), report.getAdcode(), report.getSource());
        return rows > 0 ? success("位置上报成功") : error("位置上报失败");
    }

    /**
     * 查询当前登录用户最近一次上报的城市
     */
    @GetMapping("/current")
    public AjaxResult current() {
        try {
            SysUser user = userService.selectUserById(SecurityUtils.getUserId());
            if (user == null) {
                return error("用户不存在");
            }
            AjaxResult ajax = success();
            ajax.put("city", user.getCurrentCity());
            ajax.put("district", user.getCurrentDistrict());
            ajax.put("adcode", user.getCurrentAdcode());
            ajax.put("latitude", user.getCurrentLatitude());
            ajax.put("longitude", user.getCurrentLongitude());
            ajax.put("locationTime", user.getLocationTime());
            return ajax;
        } catch (Exception e) {
            return error("请先登录");
        }
    }
}

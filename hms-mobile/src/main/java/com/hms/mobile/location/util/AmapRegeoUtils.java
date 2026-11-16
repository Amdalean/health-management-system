package com.hms.mobile.location.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.http.HttpUtils;
import com.hms.mobile.location.domain.LocationReport;

/**
 * 高德逆地理编码工具
 * add by Qwen 2026-10-26 说明: 服务端把经纬度解析成城市/adcode，客户端不必再调高德（省掉小程序域名白名单）
 */
public class AmapRegeoUtils {

    private static final Logger log = LoggerFactory.getLogger(AmapRegeoUtils.class);

    /** 高德 Web 服务 key（与天气接口同一个） */
    private static final String AMAP_KEY = "9073e847b0ab5658da7bd99b6de16f34";

    /**
     * 根据经纬度补全 report 的 province/city/district/adcode
     *
     * @return 是否解析成功（拿到 adcode 才算成功）
     */
    public static boolean fillByRegeo(LocationReport report) {
        if (report.getLongitude() == null || report.getLatitude() == null) {
            return false;
        }
        try {
            String url = "https://restapi.amap.com/v3/geocode/regeo?key=" + AMAP_KEY
                    + "&location=" + report.getLongitude() + "," + report.getLatitude()
                    + "&extensions=base";
            String resp = HttpUtils.sendGet(url);
            JSONObject json = JSON.parseObject(resp);
            if (json == null || !"1".equals(json.getString("status"))) {
                log.warn("逆地理编码失败: {}", resp);
                return false;
            }
            JSONObject regeocode = json.getJSONObject("regeocode");
            if (regeocode == null) {
                return false;
            }
            JSONObject comp = regeocode.getJSONObject("addressComponent");
            if (comp == null) {
                return false;
            }
            String province = asString(comp.get("province"));
            String district = asString(comp.get("district"));
            String adcode = asString(comp.get("adcode"));
            // 直辖市场景 city 返回空数组，用省名兜底
            String city = asString(comp.get("city"));
            if (!province.isEmpty()) {
                report.setProvince(province);
            }
            report.setCity(city.isEmpty() ? province : city);
            report.setDistrict(district);
            report.setAdcode(adcode);
            log.info("逆地理编码结果: {} {} {} adcode={}", province, report.getCity(), district, adcode);
            return !adcode.isEmpty();
        } catch (Exception e) {
            log.warn("逆地理编码异常: {}", e.getMessage());
            return false;
        }
    }

    /** 高德返回值可能是字符串，也可能是空数组 [] */
    private static String asString(Object value) {
        return (value instanceof String) ? ((String) value).trim() : "";
    }
}

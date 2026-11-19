package com.hms.mobile.common.util;

/**
 * 地理坐标工具类
 * add by CYQ 2026-06-06 说明: 提供基于Haversine公式的经纬度距离计算
 * @author 崔永琦
 */
public class GeoUtils {

    /** 地球半径 (米) */
    private static final double EARTH_RADIUS = 6371000.0;

    /**
     * 计算两点间的距离 (米)
     *
     * @param lat1 点1纬度
     * @param lon1 点1经度
     * @param lat2 点2纬度
     * @param lon2 点2经度
     * @return 距离(米)
     */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}

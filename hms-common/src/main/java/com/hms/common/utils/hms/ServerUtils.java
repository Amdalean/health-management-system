package com.hms.common.utils.hms;

import com.hms.common.utils.StringUtils;
import com.hms.common.utils.ip.IpUtils;

/**
 * 服务器标识工具类
 * <p>
 * 统一调度日志、集成日志等服务标识口径：优先取 JVM 系统属性 {@code server.name}，
 * 未配置则使用 {@code hostname(ip)}。
 *
 * add by CYQ 2026-09-21 统一服务器标识
 */
public final class ServerUtils {

    /**
     * 当前服务器标识（进程内只计算一次）
     */
    private static final String SERVER_NAME = buildServerName();

    private ServerUtils() {
    }

    /**
     * 获取当前服务器标识
     */
    public static String getServerName() {
        return SERVER_NAME;
    }

    private static String buildServerName() {
        String name = System.getProperty("server.name");
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        return IpUtils.getHostName() + "(" + IpUtils.getHostIp() + ")";
    }
}

package com.hms.common.utils.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.config.RuoYiConfig;
import com.hms.common.constant.Constants;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.http.HttpUtils;

/**
 * 获取地址类
 * 
 * @author ruoyi
 */
public class AddressUtils
{
    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    // IP地址查询（使用 ip-api.com 免费接口，支持中文，45次/分钟/IP）
    // add by CYQ 2026年7月14日 原 whois.pconline.com.cn 已返回403，替换为 ip-api.com
    public static final String IP_URL = "http://ip-api.com/json/";

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip)
    {
        // 内网不查询
        if (IpUtils.internalIp(ip))
        {
            return "内网IP";
        }
        if (RuoYiConfig.isAddressEnabled())
        {
            try
            {
                String url = IP_URL + ip + "?lang=zh-CN&fields=status,country,regionName,city";
                String rspStr = HttpUtils.sendGet(url);
                if (StringUtils.isEmpty(rspStr))
                {
                    log.error("获取地理位置异常 {}", ip);
                    return UNKNOWN;
                }
                JSONObject obj = JSON.parseObject(rspStr);
                if (!"success".equals(obj.getString("status")))
                {
                    log.error("获取地理位置失败 {}: {}", ip, rspStr);
                    return UNKNOWN;
                }
                String region = obj.getString("regionName");
                String city = obj.getString("city");
                return String.format("%s %s", region, city);
            }
            catch (Exception e)
            {
                log.error("获取地理位置异常 {}", ip);
            }
        }
        return UNKNOWN;
    }
}

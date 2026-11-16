package com.hms.mobile.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * UniPush 2.0 配置属性（通过 uniCloud 云函数发送）
 * add by CYQ 2026-05-22
 * modify by CYQ 2026-06-08
 */
@Component
@ConfigurationProperties(prefix = "unipush")
public class UniPushProperties {

    /** uniCloud 云函数 URL化地址 */
    private String functionUrl;

    /** 应用 AppID */
    private String appId = "__UNI__73E64E4";

    public String getFunctionUrl() {
        return functionUrl;
    }

    public void setFunctionUrl(String functionUrl) {
        this.functionUrl = functionUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
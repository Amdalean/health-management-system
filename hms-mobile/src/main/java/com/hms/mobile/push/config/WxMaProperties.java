package com.hms.mobile.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性
 * add by CYQ 2026-06-10
 */
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
/**
 * public class WxMaProperties {
 *
 * @author 崔永琦
 */
public class WxMaProperties {

    /** 小程序 AppID */
    private String appId;

    /** 小程序 AppSecret */
    private String secret;

    /** 订阅消息模板ID */
    private String templateId;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
}

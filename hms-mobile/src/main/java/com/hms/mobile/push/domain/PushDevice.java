package com.hms.mobile.push.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hms.common.core.domain.BaseEntity;

/**
 * 推送设备信息对象
 * add by CYQ 2026-05-17 说明: 移动端推送设备信息实体
 * modify by CYQ 2026-06-10 添加 wxOpenid 字段存储微信小程序 openid
 * @author 崔永琦
 */
public class PushDevice extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 推送客户端ID（UniPush CID） */
    private String clientId;

    /** 平台 (android/ios/wx_miniapp) */
    private String platform;

    /** 应用版本 */
    private String appVersion;

    /** 微信小程序 openid */
    private String wxOpenid;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getWxOpenid() { return wxOpenid; }
    public void setWxOpenid(String wxOpenid) { this.wxOpenid = wxOpenid; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}

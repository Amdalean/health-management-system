package com.hms.mobile.appversion.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * APP版本管理对象 sys_app_version
 * add by CYQ 2026-05-17 说明: APP版本实体 (移动端专属)
 * @author 崔永琦
 */
public class SysAppVersion extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 版本号 (如 1.2.0) */
    @Excel(name = "版本号")
    private String version;

    /** 版本码 (如 12) */
    @Excel(name = "版本码")
    private Integer versionCode;

    /** 平台 (android/ios) */
    @Excel(name = "平台", readConverterExp = "0=Android,1=iOS")
    private String platform;

    /** 文件名称 */
    @Excel(name = "文件名称")
    private String fileName;

    /** 文件路径 */
    private String filePath;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 是否强制更新 (0=否, 1=是) */
    @Excel(name = "强制更新", readConverterExp = "0=否,1=是")
    private Integer forceUpdate;

    /** 更新日志 */
    @Excel(name = "更新日志")
    private String releaseNotes;

    /** 状态 (0=禁用, 1=启用) */
    @Excel(name = "状态", readConverterExp = "0=禁用,1=启用")
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Integer getVersionCode() { return versionCode; }
    public void setVersionCode(Integer versionCode) { this.versionCode = versionCode; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getForceUpdate() { return forceUpdate; }
    public void setForceUpdate(Integer forceUpdate) { this.forceUpdate = forceUpdate; }
    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}

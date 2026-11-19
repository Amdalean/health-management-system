package com.hms.mobile.clockin.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 打卡记录对象
 * add by CYQ 2026-06-05 说明: 移动端打卡记录实体
 * @author 崔永琦
 */
public class ClockinRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long id;

    @Excel(name = "用户ID")
    private Long userId;

    @Excel(name = "用户名称")
    private String userName;

    @Excel(name = "用户昵称")
    private String nickName;

    @Excel(name = "部门ID")
    private Long deptId;

    @Excel(name = "部门名称")
    private String deptName;

    @Excel(name = "纬度")
    private Double latitude;

    @Excel(name = "经度")
    private Double longitude;

    @Excel(name = "地址")
    private String address;

    @Excel(name = "打卡类型")
    private String clockinType;

    @Excel(name = "备注")
    private String remark;

    /** 微信小程序 openid（仅用于接收请求参数，不持久化到打卡表） */
    private String openid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "打卡时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getClockinType() {
        return clockinType;
    }

    public void setClockinType(String clockinType) {
        this.clockinType = clockinType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

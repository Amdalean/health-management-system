package com.hms.mobile.location.domain;

/**
 * 位置上报请求对象（不落库，直接写入 sys_user 的 current_* 字段）
 * add by Qwen 2026-10-26
 * @author 崔永琦
 */
public class LocationReport {

    /** 省 */
    private String province;

    /** 市（直辖市场景由客户端用省名兜底） */
    private String city;

    /** 区/县 */
    private String district;

    /** 高德行政区划编码 */
    private String adcode;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 来源，默认 app */
    private String source;

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAdcode() { return adcode; }
    public void setAdcode(String adcode) { this.adcode = adcode; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

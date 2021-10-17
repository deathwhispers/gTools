package com.guangjian.gtool.geo;

/**
 * @Author: yanggj
 * @Description: 坐标系枚举类
 * @Date: 2021/10/15 14:29
 * @Version: 1.0.0
 */
public enum CoordinateEnum {

    WGS84("WGS84", "地球坐标系", "国际通用坐标系,无坐标转换,GPS,谷歌地图"),
    GCJ02("GCJ02", "火星坐标系", "国家测绘局制定的地理信息坐标系统,加密后存在偏移,高德地图,腾讯地图,阿里云地图谷歌国内地图"),
    BD09("BD09", "百度坐标系", "在GCJ02的基础上进行二次加密,百度地图");

    private String code;
    private String name;
    private String desc;

    CoordinateEnum(String code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    CoordinateEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}

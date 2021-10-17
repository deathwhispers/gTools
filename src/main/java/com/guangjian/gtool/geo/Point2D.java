package com.guangjian.gtool.geo;

/**
 * @Author: yanggj
 * @Description: 平面坐标点
 * @Date: 2021/10/15 14:35
 * @Version: 1.0.0
 */
public class Point2D implements Comparable<Point2D> {

    private double lon;
    private double lat;
    // 斜率(根据斜率排序)
    private double slope;
    // 坐标类型
    private String coordinateType;


    public int compareTo(Point2D p) {
        if (this.slope < p.slope) {
            return -1;
        } else if (this.slope > p.slope) {
            return 1;
        } else {
            if (this.lon == p.lon && this.slope == p.slope && this.slope == Double.MAX_VALUE) {
                return Double.compare(p.lat, this.lat);
            }
            return 0;
        }
    }

    public Point2D() {
    }

    public Point2D(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public String getCoordinateType() {
        return coordinateType;
    }

    public void setCoordinateType(String coordinateType) {
        this.coordinateType = coordinateType;
    }
}

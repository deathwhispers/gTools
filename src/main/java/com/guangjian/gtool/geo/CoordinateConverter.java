package com.guangjian.gtool.geo;

/**
 * @Author: yanggj
 * @Description: 坐标转换器
 * @Date: 2021/10/15 14:15
 * @Version: 1.0.0
 */
public class CoordinateConverter {

    // Π
    public final static double PI = Math.PI;
    public final static double AXIS = 6378245.0;
    private final static double OFFSET = 0.00669342162296594323; // (a^2 - b^2) / a^2
    private final static double X_PI = PI * 3000.0 / 180.0;

    // GCJ02 => BD09 国家测绘局坐标系 => 百度09坐标系
    public static Point2D GCJ02ToBD09(Point2D point) {
        if (!point.getCoordinateType().equals(CoordinateEnum.GCJ02.getCode())) {
            throw new BadCoordinateException("坐标系错误");
        }
        double glat = point.getLat();
        double glon = point.getLon();
        Point2D point2D = new Point2D();
        double z = Math.sqrt(glon * glon + glat * glat) + 0.00002 * Math.sin(glat * X_PI);
        double theta = Math.atan2(glat, glon) + 0.000003 * Math.cos(glon * X_PI);
        double lat = z * Math.sin(theta) + 0.006;// 纬度
        double lon = z * Math.cos(theta) + 0.0065;// 精度
        point2D.setLat(lat);
        point2D.setLon(lon);
        point2D.setCoordinateType(CoordinateEnum.BD09.getCode());// 设置坐标系
        return point2D;
    }

    // BD09 => GCJ-02 百度09坐标系 => 国家测绘局坐标系
    public static Point2D BD09ToGCJ02(Point2D point) {
        double x = point.getLon() - 0.0065;
        double y = point.getLat() - 0.006;
        double[] latlon = new double[2];
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        latlon[0] = z * Math.sin(theta);
        latlon[1] = z * Math.cos(theta);
        Point2D point2D = new Point2D(latlon[1], latlon[0]);
        point2D.setCoordinateType(CoordinateEnum.GCJ02.getCode());
        return point2D;
    }

    // BD09 => WGS84 百度09坐标系 => 地球坐标系
    public static Point2D BD09ToWGS84(Point2D point) {
        Point2D point2D = BD09ToGCJ02(point);
        return gcj2WGS(point2D);
    }

    // WGS84=百度BD09  地球坐标系 => 百度09坐标系
    public static Point2D WGS84ToBD09(Point2D point) {
        Point2D point2D = WGS84ToGCJ02(point);
        return GCJ02ToBD09(point2D);
    }

    // WGS84 => GCJ02 地球坐标系 => 国家测绘局坐标系
    public static Point2D WGS84ToGCJ02(Point2D point) {
        double[] latlon = new double[2];
        if (outOfChina(point.getLat(), point.getLon())) {
            point.setCoordinateType(CoordinateEnum.GCJ02.getCode());
            return point;
        }
        Point2D deltaD = delta(point);
        return new Point2D(point.getLon() + deltaD.getLon(), point.getLat() + deltaD.getLat());
    }

    // GCJ02=>WGS84  国家测绘局坐标系 => 地球坐标系
    public static Point2D gcj2WGS(Point2D point) {
        double[] latlon = new double[2];
        if (outOfChina(point.getLat(), point.getLon())) {
            point.setCoordinateType(CoordinateEnum.GCJ02.getCode());
            return point;
        }
        Point2D deltaD = delta(point);
        Point2D point2D = new Point2D();
        point2D.setLon(point.getLon() - deltaD.getLon());
        point2D.setLat(point.getLat() - deltaD.getLat());
        point2D.setCoordinateType(CoordinateEnum.GCJ02.getCode());
        return point2D;
    }

    // GCJ02=>WGS84  国家测绘局坐标系 => 精确的地球坐标系
    public static Point2D GCJ02ToWGS84Exactly(Point2D point) {
        double initDelta = 0.01;
        double threshold = 0.000000001;
        double dLat = initDelta, dLon = initDelta;
        double mLat = point.getLat() - dLat, mLon = point.getLon() - dLon;
        double pLat = point.getLat() + dLat, pLon = point.getLon() + dLon;
        double wgsLat, wgsLon, i = 0;
        while (true) {
            wgsLat = (mLat + pLat) / 2;
            wgsLon = (mLon + pLon) / 2;
            Point2D tmp = WGS84ToGCJ02(new Point2D(wgsLon, wgsLat));
            dLat = tmp.getLat() - point.getLat();
            dLon = tmp.getLon() - point.getLon();
            if ((Math.abs(dLat) < threshold) && (Math.abs(dLon) < threshold)) break;
            if (dLat > 0) {
                pLat = wgsLat;
            } else {
                mLat = wgsLat;
            }
            if (dLon > 0) {
                pLon = wgsLon;
            } else {
                mLon = wgsLon;
            }
            if (++i > 10000) break;
        }
        Point2D point2D = new Point2D(wgsLon, wgsLat);
        point2D.setCoordinateType(CoordinateEnum.WGS84.getCode());
        return point2D;
    }

    // 计算两点之间的距离
    public static double distance(Point2D point1, Point2D point2) {
        int earthR = 6371000;
        double x = Math.cos(point1.getLat() * Math.PI / 180) * Math.cos(point2.getLat() * Math.PI / 180) * Math.cos((point1.getLon() - point2.getLon()) * Math.PI / 180);
        double y = Math.sin(point1.getLat() * Math.PI / 180) * Math.sin(point2.getLat() * Math.PI / 180);
        double s = x + y;
        if (s > 1) s = 1;
        if (s < -1) s = -1;
        double alpha = Math.acos(s);
        return alpha * earthR;
    }

    public static Point2D delta(Point2D point) {
        double dLat = transformLat(point.getLon() - 105.0, point.getLat() - 35.0);
        double dLon = transformLon(point.getLon() - 105.0, point.getLat() - 35.0);
        double radLat = point.getLat() / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - OFFSET * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((AXIS * (1 - OFFSET)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (AXIS / sqrtMagic * Math.cos(radLat) * PI);
        return new Point2D(dLon, dLat);
    }

    public static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347) {
            return true;
        }
        if (lat < 0.8293 || lat > 55.8271) {
            return true;
        }
        return false;
    }

    public static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    public static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    //mapbar ==> wgs84 ͼ������ת��������
    public static double[] mapBar2WGS84(double mapbarLon, double mapbarLat) {
        mapbarLon = (double) (mapbarLon) * 100000 % 36000000;
        mapbarLat = (double) (mapbarLat) * 100000 % 36000000;

        double x1 = (double) (long) (-(((Math.cos(mapbarLat / 100000)) * (mapbarLon / 18000)) + ((Math.sin(mapbarLon / 100000)) * (mapbarLat / 9000))) + mapbarLon);
        double y1 = (double) (long) (-(((Math.sin(mapbarLat / 100000)) * (mapbarLon / 18000)) + ((Math.cos(mapbarLon / 100000)) * (mapbarLat / 9000))) + mapbarLat);

        long x2 = (long) (-(((Math.cos(y1 / 100000)) * (x1 / 18000)) + ((Math.sin(x1 / 100000)) * (y1 / 9000))) + mapbarLon + ((mapbarLon > 0) ? 1 : -1));
        long y2 = (long) (-(((Math.sin(y1 / 100000)) * (x1 / 18000)) + ((Math.cos(x1 / 100000)) * (y1 / 9000))) + mapbarLat + ((mapbarLat > 0) ? 1 : -1));

        double[] latlon = new double[2];
        latlon[0] = y2 / 100000.0;
        latlon[1] = x2 / 100000.0;
        return latlon;
    }


}

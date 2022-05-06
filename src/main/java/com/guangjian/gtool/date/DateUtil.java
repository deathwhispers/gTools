package com.guangjian.gtool.date;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 日期工具类
 * <p>
 * 提供常用的日期处理方法
 * 仅用作备忘,用到时便于查找
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2022/4/25 14:32
 */
public class DateUtil {

    // 获取当年第一天
    public static LocalDate firstDayOfYear() {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
    }

    public static LocalDate firstDayOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfYear());
    }

    public static LocalDate firstDayOfYear(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.with(TemporalAdjusters.firstDayOfYear());
    }

    public static LocalDate firstDayOfYear(String dateStr) {
        LocalDate parse = LocalDate.parse(dateStr);
        return parse.with(TemporalAdjusters.firstDayOfYear());
    }

    public static LocalDate firstDayOfYear(String dateStr, DateTimeFormatter formatter) {
        LocalDate parse = LocalDate.parse(dateStr, formatter);
        return parse.with(TemporalAdjusters.firstDayOfYear());
    }

    // 获取当年最后一天
    public static LocalDate lastDayOfYear() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfYear());
    }

    public static LocalDate lastDayOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    public static LocalDate lastDayOfYear(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.with(TemporalAdjusters.lastDayOfYear());
    }

    public static LocalDate lastDayOfYear(String dateStr) {
        LocalDate parse = LocalDate.parse(dateStr);
        return parse.with(TemporalAdjusters.lastDayOfYear());
    }

    public static LocalDate lastDayOfYear(String dateStr, DateTimeFormatter formatter) {
        LocalDate parse = LocalDate.parse(dateStr, formatter);
        return parse.with(TemporalAdjusters.lastDayOfYear());
    }

    // 获取当月第一天
    public static LocalDate firstDayOfMonth() {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
    }

    public static LocalDate firstDayOfMonth(LocalDate date) {
        assert date != null : "date is null";
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    public static LocalDate firstDayOfMonth(Date date) {
        assert date != null : "date is null";
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.with(TemporalAdjusters.firstDayOfMonth());
    }

    public static LocalDate firstDayOfMonth(String dateStr) {
        LocalDate parse = LocalDate.parse(dateStr);
        return parse.with(TemporalAdjusters.firstDayOfMonth());
    }

    public static LocalDate firstDayOfMonth(String dateStr, DateTimeFormatter formatter) {
        LocalDate parse = LocalDate.parse(dateStr, formatter);
        return parse.with(TemporalAdjusters.firstDayOfMonth());
    }

    // 获取当月最后一天
    public static LocalDate lastDayOfMonth() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate lastDayOfMonth(LocalDate date) {
        assert date != null : "date is null";
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate lastDayOfMonth(Date date) {
        assert date != null : "date is null";
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate lastDayOfMonth(String dateStr) {
        LocalDate parse = LocalDate.parse(dateStr);
        return parse.with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate lastDayOfMonth(String dateStr, DateTimeFormatter formatter) {
        LocalDate parse = LocalDate.parse(dateStr, formatter);
        return parse.with(TemporalAdjusters.lastDayOfMonth());
    }

    // Date -> LocalDate
    public static LocalDate toLocalDate(Date date) {
        return toLocalDate(date, ZoneId.systemDefault());
    }

    public static LocalDate toLocalDate(Date date, ZoneId zoneId) {
        assert date != null : "date is null";
        return date.toInstant().atZone(zoneId).toLocalDate();
    }

    // String -> LocalDate
    public static LocalDate toLocalDate(String dateStr) {
        return toLocalDate(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate toLocalDate(String dateStr, DateTimeFormatter formatter) {
        return LocalDate.parse(dateStr, formatter);
    }

    public static LocalDate toLocalDate(String dateStr, String pattern) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    // LocalDate -> Date
    public static Date toDateByAtTime(LocalDate date) {
        assert date != null : "date is null";
        Instant instant = date.atTime(LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    public static Date toDateByAtStartOfDay(LocalDate date) {
        assert date != null : "date is null";
        Instant instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    public static Date toDateByTimestamp(LocalDate date) {
        assert date != null : "date is null";
        Instant instant = Timestamp.valueOf(date.atTime(LocalTime.MIDNIGHT)).toInstant();
        return Date.from(instant);
    }

    public static Date toDateByTimestampGetTime(LocalDate date) {
        assert date != null : "date is null";
        Timestamp timestamp = Timestamp.valueOf(date.atTime(LocalTime.MIDNIGHT));
        return new Date(timestamp.getTime());
    }

}

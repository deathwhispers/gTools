package com.dw.tool.validator;

import com.dw.tool.util.BadRequestException;
import com.dw.tool.validator.constraints.TemporalUnitConstraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Objects;

/**
 * 时间单位校验器
 * {@link TemporalUnitConstraint}
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2022/12/2 9:53
 */
public class TemporalUnitValidator implements ConstraintValidator<TemporalUnitConstraint, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return TemporalUnitEnum.include(value);
    }
}

@Getter
@AllArgsConstructor
enum TemporalUnitEnum {
    SECONDS("Seconds", "秒"),
    MINUTES("Minutes", "分"),
    HOURS("Hours", "小时"),
    DAYS("Days", "天"),
    WEEKS("Weeks", "周"),
    MONTHS("Months", "月"),
    Quarter("Quarter", "季度"),
    YEARS("Years", "年"),
    ;

    private final String name;
    private final String desc;

    public static TemporalUnitEnum find(String temporalUnit) {
        for (TemporalUnitEnum temporalUnitEnum : values()) {
            if (Objects.equals(temporalUnit, temporalUnitEnum.getName())) {
                return temporalUnitEnum;
            }
        }
        throw new BadRequestException("时间单位格式不正确，请检查");
    }

    public static boolean include(String temporalUnit) {
        return Arrays.stream(TemporalUnitEnum.values()).anyMatch(item -> item.getName().equals(temporalUnit));
    }

    public static TemporalUnit toTemporalUnit(TemporalUnitEnum temporalUnitEnum) {
        TemporalUnit temporalUnit;
        switch (temporalUnitEnum) {
            case SECONDS:
                temporalUnit = ChronoUnit.SECONDS;
                break;
            case MINUTES:
                temporalUnit = ChronoUnit.MINUTES;
                break;
            case HOURS:
                temporalUnit = ChronoUnit.HOURS;
                break;
            case DAYS:
                temporalUnit = ChronoUnit.DAYS;
                break;
            case WEEKS:
                temporalUnit = ChronoUnit.WEEKS;
                break;
            case MONTHS:
                temporalUnit = ChronoUnit.MONTHS;
                break;
            case YEARS:
                temporalUnit = ChronoUnit.YEARS;
                break;
            default:
                temporalUnit = ChronoUnit.DAYS;
        }
        return temporalUnit;
    }

    public static String ofPattern(TemporalUnitEnum temporalUnitEnum) {
        String pattern;
        switch (temporalUnitEnum) {
            case SECONDS:
                pattern = "yyyy-MM-dd HH:mm:ss";
                break;
            case MINUTES:
                pattern = "yyyy-MM-dd HH:mm";
                break;
            case HOURS:
                pattern = "yyyy-MM-dd HH";
                break;
            case DAYS:
                pattern = "yyyy-MM-dd";
                break;
            case WEEKS:
                pattern = "yyyy-MM-dd";
                break;
            case MONTHS:
                pattern = "yyyy-MM";
                break;
            case YEARS:
                pattern = "yyyy";
                break;
            default:
                pattern = "yyyy-MM-dd";
        }
        return pattern;
    }
}
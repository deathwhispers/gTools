package com.dw.tool.validator.constraints;

import com.dw.tool.validator.BlankValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 空校验（null、空串、trim后为空串）
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2022/7/21 15:27
 */
@Documented
@Constraint(validatedBy = {BlankValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Blank {

    String message() default "{com.rhy.aibox.common.validator.constraints.Blank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
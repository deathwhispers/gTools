package com.dw.tool.validator.constraints;


import com.dw.tool.validator.NodeTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 组织机构节点类型约束
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2022/12/2 9:50
 */
@Documented
@Constraint(validatedBy = {NodeTypeValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface NodeTypeConstraint {

    String message() default "未知的节点类型，请使用[0，1，2，3]";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

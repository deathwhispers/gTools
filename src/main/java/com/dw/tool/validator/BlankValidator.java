package com.dw.tool.validator;

import cn.hutool.core.util.StrUtil;
import com.dw.tool.validator.constraints.Blank;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link Blank} 校验器
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2022/7/21 15:24
 */
public class BlankValidator implements ConstraintValidator<Blank, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return StrUtil.isBlank(value);
    }
}
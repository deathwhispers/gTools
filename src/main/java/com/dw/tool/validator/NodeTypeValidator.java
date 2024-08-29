package com.dw.tool.validator;

import cn.hutool.core.util.StrUtil;
import com.dw.tool.validator.constraints.NodeTypeConstraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 组织机构节点类型校验器
 * {@link NodeTypeConstraint}
 *
 * @author yanggj
 * @version 1.0.0
 * @date 2022/12/2 9:53
 */
public class NodeTypeValidator implements ConstraintValidator<NodeTypeConstraint, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return StrUtil.isBlank(value) || NodeTypeEnum.include(value);
    }
}
@Getter
@RequiredArgsConstructor
enum NodeTypeEnum {

    // 节点类型 枚举类
    GROUP("0", "group", "集团"),
    ORG("1", "org", "机构组织(运营公司)"),
    WAY("2", "way", "路段"),
    FAC("3", "fac", "设施(地理位置)");

    private final String code;
    private final String name;
    private final String desc;

    public static String getName(String code) {
        for (NodeTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getName();
            }
        }
        return null;
    }

    public static boolean include(String code) {
        return Arrays.stream(values()).anyMatch(e -> e.getCode().equals(code));
    }

    public static boolean excludeByName(String name) {
        return Arrays.stream(values()).noneMatch(e -> e.getName().equals(name));
    }
}

package com.guangjian.gtool;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2023/6/8 11:01
 */
public class Test {

    public static void main(String[] args) {
        int sum = 300;
        double faultMileage = new BigDecimal(sum / 1000d).setScale(2, RoundingMode.HALF_UP).doubleValue();
        System.out.println(faultMileage);
    }
}

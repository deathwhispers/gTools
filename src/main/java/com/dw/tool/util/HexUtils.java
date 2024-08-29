package com.dw.tool.util;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2023/10/8 14:44
 */
public class HexUtils {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static final String HEX_PATTERN = "^[0-9a-fA-F]+$";

    public static String calculateChecksum(String hexString) {
        if (!isHexString(hexString)) {
            throw new IllegalArgumentException("Input is not a hexadecimal string");
        }
        // 去除字符串中的空格
        hexString = hexString.replaceAll("\\s", "");

        // 将字符串转换为字节数组
        byte[] bytes = hexStringToBytes(hexString);

        if (bytes.length < 3) {
            throw new IllegalArgumentException("Input is too short to contain a valid frame");
        }
        return calculateChecksum(bytes);
    }

    private static boolean isHexString(String str) {
        return Pattern.matches(HEX_PATTERN, str);
    }

    /**
     * 将十六进制字符串转换为字节数组
     */
    public static byte[] hexStringToBytes(String hexString) {
        int length = hexString.length();
        byte[] bytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * @param num      待转数字
     * @param byteSize 字节数
     * @return /
     */
    public static String toHexString(int num, int byteSize) {
        String hexString = Integer.toHexString(num);
        return padZero(hexString, byteSize * 2);
    }


    /**
     * 将byte数组转换为十六进制字符串
     */
    public static String bytesToHexString(byte[] bytes, int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(byteToHex(bytes[i]));
        }
        return stringBuilder.toString();
    }

    public static String bytesToHexString(byte[] bytes, int offset, int length) {
        StringBuilder stringBuilder = new StringBuilder(2 * length);
        for (int i = offset; i < offset + length; i++) {
            stringBuilder.append(byteToHex(bytes[i]));
        }
        return stringBuilder.toString();
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(byteToHex(b));
        }
        return sb.toString();
    }

    private static String byteToHex(byte b) {
        char[] hexChars = new char[2];
        hexChars[0] = HEX_ARRAY[(b & 0xF0) >> 4];
        hexChars[1] = HEX_ARRAY[b & 0x0F];
        return new String(hexChars);
    }

    /**
     * 计算校验和
     *
     * @param data 字节数组
     * @return 校验和 16进制字符串，一个字节
     */
    public static String calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte datum : data) {
            checksum += datum & 0xFF;
            checksum %= 256;
        }
        String hexString = Integer.toHexString(checksum);
        return StrUtil.fillBefore(hexString, '0', 2);
    }

    public static int bcdHexStringToDecimal(String hexStr) {

        if (hexStr == null || hexStr.isEmpty()) {
            return 0;
        }

        int result = 0;

        // 计算每一位的值并相加，最终得到十进制数
        for (int i = 0; i < hexStr.length(); i += 2) {
            char c1 = hexStr.charAt(i);
            char c2 = hexStr.charAt(i + 1);

            // 将两个字符转为数值，并将高4位和低4位合并成一个byte
            byte b = (byte) ((Character.digit(c1, 16) << 4) | Character.digit(c2, 16));

            // 将BCD码转为十进制数，并乘以当前位数的权值
            result = result * 100 + ((b & 0xf0) >> 4) * 10 + (b & 0x0f);
        }

        return result;
    }

    public static String subtract33FromHexString(String hexString) {
        return operateOnHexString(hexString, 0x33, false);
    }

    public static String add33FromHexString(String hexString) {
        return operateOnHexString(hexString, 0x33, true);
    }

    // 反转十六进制字符串
    public static String reverseHexString(String hexString) {
        StringBuilder reversedBuilder = new StringBuilder();

        for (int i = hexString.length() - 2; i >= 0; i -= 2) {
            reversedBuilder.append(hexString.substring(i, i + 2));
        }

        return reversedBuilder.toString();
    }

    public static float hexToFloat(String hexString) {
        // 将十六进制字符串解析为整数
        int intValue = Integer.parseInt(hexString, 16);

        // 使用 Float.intBitsToFloat 将整数转换为浮点数
        return Float.intBitsToFloat(intValue);
    }

    public static String operateOnHexString(String hexString, int operand, boolean isAddition) {
        if (hexString == null || hexString.isEmpty()) {
            throw new IllegalArgumentException("Hex string cannot be null or empty");
        }

        if (operand < 0 || operand > 255) {
            throw new IllegalArgumentException("Operand must be between 0 and 255");
        }
        // 去除字符串的空格
        hexString = hexString.replaceAll("\\s", "");

        int length = hexString.length();
        if (length % 2 != 0) {
            throw new IllegalArgumentException("Hex string length must be even");
        }

        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i += 2) {
            char highNibble = hexString.charAt(i);
            char lowNibble = hexString.charAt(i + 1);

            int byteValue = (Character.digit(highNibble, 16) << 4) | Character.digit(lowNibble, 16);
            if (isAddition) {
                byteValue += operand;
                byteValue %= 256; // 处理超过256的情况
            } else {
                byteValue -= operand;
                if (byteValue < 0) {
                    byteValue += 256;
                }
            }

            result.append(String.format("%02X", byteValue));
        }

        return result.toString();
    }


    private static final int CRC_POLYNOMIAL = 0xA001;
    private static final int CRC_INITIAL = 0xFFFF;

    /**
     * 计算 Modbus CRC-16校验值
     *
     * @param str 输入字符串
     * @return CRC-16校验值
     */
    public static String calModbusCRC(String str) {
        byte[] bytes = hexStringToBytes(str);
        int crc = CRC_INITIAL;
        for (byte b : bytes) {
            crc ^= (int) b & 0xFF;
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) == 1) {
                    crc = (crc >>> 1) ^ CRC_POLYNOMIAL;
                } else {
                    crc = crc >>> 1;
                }
            }
        }
        int result = swapBytes(crc & 0xFFFF);// 进行大小端转换
        return String.format("%04X", result);
    }

    /**
     * 在十六进制字符串前补零，直到达到指定的长度
     *
     * @param hexString    十六进制字符串
     * @param targetLength 目标长度
     * @return 补零后的字符串
     */
    private static String padZero(String hexString, int targetLength) {
        int currentLength = hexString.length();
        if (currentLength < targetLength) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < targetLength - currentLength; i++) {
                sb.append("0");
            }
            sb.append(hexString);
            return sb.toString();
        }
        return hexString;
    }

    /**
     * 执行字节的大小端转换
     */
    private static int swapBytes(int value) {
        return ((value & 0xFF00) >>> 8) | ((value & 0xFF) << 8);
    }

    public static void main(String[] args) {
        String s = calculateChecksum("68000000006470680103471200");
        System.out.println(s);
    }
}

package com.guangjian.gtool.util;

import cn.hutool.core.util.ArrayUtil;
import org.apache.commons.codec.binary.Base64;

/**
 * @author zhaohf
 * @date 2019/1/20
 */
public class ByteArrHelper {
    public final static String HEX_STR = "0123456789ABCDEF";
    private static ByteArrHelper byteArrHelper;

    private ByteArrHelper() {
    }


    public static ByteArrHelper getInstance() {
        if (byteArrHelper == null) {
            byteArrHelper = new ByteArrHelper();
        }
        return byteArrHelper;
    }

    /**
     * 将字节数组翻译成16进制字符串
     *
     * @param buf /
     * @return /
     */
    public String toHexString(byte[] buf) {
        if (ArrayUtil.isEmpty(buf)) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            String str = Integer.toHexString(b);
            if (str.length() > 2) {
                str = str.substring(str.length() - 2);
            } else if (str.length() < 2) {
                str = "0" + str;
            }
            sb.append(str);
        }
        return sb.toString().toUpperCase();
    }

    /**
     * 将字节数组翻译成BCD码字符串
     * 字节数组长度为1
     *
     * @param subByte /
     * @return {@link String}
     */
    public String getBCDStr(byte[] subByte) {
        byte b = subByte[0];
        byte b1 = (byte) (b & 0x0f);
        byte b2 = (byte) ((b >>> 4) & 0x0f);
        String str = ("" + b2) + b1;
        if (str.length() == 2) {
            return str;
        } else {
            return "99";
        }
    }

    /**
     * 将字节数组翻译成BCD码字符串
     *
     * @param subByte /
     * @return {@link String}
     */
    public String getBCDStrByArr(byte[] subByte) {
        StringBuilder buf = new StringBuilder();
        for (byte b : subByte) {
            buf.append(getBCDStr(new byte[]{b}));
        }
        return buf.toString();
    }

    /**
     * 将字节转换成16进制字符串
     *
     * @param buf /
     * @return {@link String}
     */
    public String toHexString(byte buf) {
        String str = Integer.toHexString(buf);
        if (str.length() > 2) {
            str = str.substring(str.length() - 2);
        } else if (str.length() < 2) {
            str = "0" + str;
        }
        return str.toUpperCase();
    }

    /**
     * 拼接两个字节数组
     *
     * @param b1 /
     * @param b2 /
     * @return /
     */
    public byte[] union(byte[] b1, byte[] b2) {
        byte[] buf = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, buf, 0, b1.length);
        System.arraycopy(b2, 0, buf, b1.length, b2.length);
        return buf;
    }

    /**
     * 拼接多个字节数组
     *
     * @param b /
     * @return /
     */
    public byte[] union(byte[]... b) {
        byte[] buf;
        int len = 0;
        for (byte[] bytes : b) {
            len += bytes.length;
        }
        buf = new byte[len];
        int pos = 0;
        for (byte[] bytes : b) {
            for (int j = 0; j < bytes.length; j++) {
                buf[pos] = bytes[j];
                pos++;
            }
        }
        return buf;
    }

    /**
     * 截取指定位置到末尾的字节数组 start是数组脚标 从0开始
     *
     * @param data  /
     * @param start /
     * @return /
     */
    public byte[] subByte(byte[] data, int start) {
        byte[] buf = new byte[data.length - start];
        for (int n = 0, i = start; i < data.length; i++, n++) {
            buf[n] = data[i];
        }
        return buf;
    }

    /**
     * 截取指定位置的字节数组 start end是数组脚标 从0开始 算start 不算end
     *
     * @param data  /
     * @param start /
     * @param end   /
     * @return /
     */
    public byte[] subByte(byte[] data, int start, int end) {
        byte[] buf = new byte[end - start];
        for (int n = 0, i = start; i < end; i++, n++) {
            buf[n] = data[i];
        }
        return buf;
    }

    public String subByte2HexStr(byte[] data, int start, int end) {
        byte[] buf = new byte[end - start];
        for (int n = 0, i = start; i < end; i++, n++) {
            buf[n] = data[i];
        }
        return toHexString(buf);
    }

    /**
     * 四字节数组转int
     *
     * @param b /
     * @return /
     */
    public int fourbyte2int(byte[] b) {
        return ((((b[0] << 24) & 0xff000000) ^ ((b[1] << 16) & 0x00ff0000))
                ^ ((b[2] << 8) & 0x0000ff00)) ^ (b[3] & 0x000000ff);
    }

    /**
     * 二字节数组转int
     *
     * @param b /
     * @return /
     */
    public int twobyte2int(byte[] b) {
        return ((b[0] << 8) & 0xff00) ^ (b[1] & 0x00ff);
    }

    public int byte2int(byte[] b) {
        int val = -1;
        switch (b.length) {
            case 1:
                val = b[0];
                break;
            case 2:
                val = twobyte2int(b);
                break;
            case 4:
                val = fourbyte2int(b);
                break;
            default:
                throw new RuntimeException("invalid byte array to int,only accept length 1 ,2,4, this array length is " + b.length);
        }
        return val;
    }


    /**
     * 数字转换成2进制字符串，按照位数补0
     */
    public String int2BinStr(int num, int len) {
        String cover = Integer.toBinaryString(1 << len).substring(1);
        String binStr = Integer.toBinaryString(num);
        return binStr.length() < len ? cover.substring(binStr.length()) + binStr : binStr;
    }

    /**
     * @param binStr 需要被匹配的二进制串
     * @param match  匹配串
     * @param ignore 忽略值  不进行对应位置值的匹配
     * @return /
     */
    public boolean matchBinStr(String binStr, String match, String ignore) {
        for (int i = 0; i < match.length(); i++) {
            String ms = match.substring(i, i + 1);
            String bs = binStr.substring(i, i + 1);
            if (ignore.equalsIgnoreCase(ms)) {
                //忽略值 跳过
                continue;
            }
            //位置相同 值不同  且 匹配值不为 忽略值
            if (!ms.equals(bs)) {
                return false;
            }
        }
        return true;
    }

    /**
     * int 转 二字节数组
     *
     * @param n /
     * @return /
     */
    public byte[] int2twobytes(int n) {
        byte[] buf = new byte[2];
        buf[0] = (byte) ((n >>> 8) & 0x000000ff);
        buf[1] = (byte) (n & 0x000000ff);
        return buf;
    }

    /**
     * int 转 二字节数组
     *
     * @param n /
     * @return /
     */
    public String int2twobytesStr(int n) {
        byte[] buf = int2twobytes(n);
        return toHexString(buf);
    }

    /**
     * int 转 四字节数组
     *
     * @param n /
     * @return /
     */
    public byte[] int2fourbytes(int n) {
        byte[] buf = new byte[4];
        buf[0] = (byte) ((n >>> 24) & 0x000000ff);
        buf[1] = (byte) ((n >>> 16) & 0x000000ff);
        buf[2] = (byte) ((n >>> 8) & 0x000000ff);
        buf[3] = (byte) (n & 0x000000ff);
        return buf;
    }

    public byte[] hexStr2bytes(String hex) {
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        hex = hex.toUpperCase();
        byte[] res = new byte[hex.length() / 2];
        for (int i = 0; i < res.length; i++) {
            int n = i * 2;
            int n_1 = n + 1;
            char c = hex.charAt(n);
            char c_1 = hex.charAt(n_1);
            int buf = HEX_STR.indexOf(c);
            int buf_1 = HEX_STR.indexOf(c_1);
            res[i] = (byte) (((buf << 4) & 0x000000F0) ^ (buf_1 & 0x0000000f));
        }
        return res;
    }

    public byte[] long2eightbytes(long values) {
        byte[] buffer = new byte[8];
        for (int i = 0; i < 8; i++) {
            int offset = 64 - (i + 1) * 8;
            buffer[i] = (byte) ((values >> offset) & 0xff);
        }
        return buffer;
    }

    public long eightbytes2long(byte[] buffer) {
        long values = 0;
        for (int i = 0; i < 8; i++) {
            values <<= 8;
            values |= (buffer[i] & 0xff);
        }
        return values;
    }


    public static String toChineseHex(String s) {
        byte[] bt = s.getBytes();
        StringBuilder s1 = new StringBuilder();
        for (byte b : bt) {
            String tempStr = Integer.toHexString(b);
            if (tempStr.length() > 2) {
                tempStr = tempStr.substring(tempStr.length() - 2);
            }
            s1.append(tempStr).append(" ");
        }
        return s1.toString().toUpperCase();
    }

    /**
     * Convert hex string to byte[]
     *
     * @param hexString /
     * @return byte[]
     */
    public byte[] hexStrToBytes(String hexString) {
        if (hexString == null || "".equals(hexString)) {
            throw new IllegalArgumentException("[toByteArray] this hexString must not be empty");
        }
        hexString = hexString.toLowerCase();
        hexString = hexString.replaceAll("0x", "");
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }


    /**
     * 使用org.apache.commons.codec.binary.Base64压缩字符串
     *
     * @param str 要压缩的字符串
     * @return /
     */
    public static String compress(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return Base64.encodeBase64String(str.getBytes());
    }

    /**
     * 使用org.apache.commons.codec.binary.Base64解压缩
     *
     * @param compressedStr 压缩字符串
     * @return /
     */
    public static String uncompress(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }
        return new String(Base64.decodeBase64(compressedStr));
    }


    public static void main(String[] args) {
        String compress = compress("A2603887-E027-1DDB-BEE5-785CAD9A690E");
        System.out.println(compress);
        String uncompress = uncompress(compress);
        System.out.println(uncompress);
    }
}

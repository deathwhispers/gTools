package com.guangjian.gtool.bloom;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

/**
 * @param <E> 要放入布隆过滤器的元素类型
 * @author yanggj
 * @version 1.0.0
 * @date 2022/4/25 15:05
 */
public class BloomFilter<E> implements Serializable {

    // 二进制数组
    private BitSet bitset;
    private int bitSetSize;
    private double bitsPerElement;
    //能够添加的元素的最大个数
    private int expectedNumberOfFilterElements;
    //过滤器容器中元素的实际数量
    private int numberOfAddedElements;

    // 哈希函数的个数
    private int k;

    //存储哈希值的字符串的编码方式
    static final Charset charset = StandardCharsets.UTF_8;

    //在大多数情况下，MD5提供了较好的散列精确度。如有必要，可以换成 SHA1算法
    static final String hashName = "MD5";

    //MessageDigest类用于为应用程序提供信息摘要算法的功能，如 MD5 或 SHA 算法
    static final MessageDigest digestFunction;

    // 初始化 MessageDigest 的摘要算法对象
    static {
        MessageDigest tmp;
        try {
            tmp = java.security.MessageDigest.getInstance(hashName);
        } catch (NoSuchAlgorithmException e) {
            tmp = null;
        }
        digestFunction = tmp;
    }

}

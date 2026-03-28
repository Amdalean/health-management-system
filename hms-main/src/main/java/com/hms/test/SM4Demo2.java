package com.hms.test;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import java.nio.charset.StandardCharsets;


public class SM4Demo2 {
    /**
     * 加密
     * @param key 密钥
     * @param data 明文
     * @return 加密后密文
     */
    public static String encryptData(String key, String data) {
        SM4 sm4 = SmUtil.sm4(key.getBytes(StandardCharsets.UTF_8));
        return sm4.encryptHex(data);
    }

    /**
     * 解密
     * @param key 密钥
     * @param data 密文
     * @return 解密后明文
     */
    public static String decryptData(String key, String data) {
        SM4 sm4 = SmUtil.sm4(key.getBytes(StandardCharsets.UTF_8));
        return sm4.decryptStr(data);
    }
    public static void main(String[] args) {
        encryptData("0123456789abcdeffedcba9876543210","hello");
    }
}

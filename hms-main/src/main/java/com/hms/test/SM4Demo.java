package com.hms.test;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class SM4Demo {

    // SM4加密算法
    public static byte[] sm4Encrypt(byte[] key, byte[] iv, byte[] data) {
        try {
            // SM4 CBC模式加密
            SM4Engine engine = new SM4Engine();
            CBCBlockCipher blockCipher = new CBCBlockCipher(engine);
            PKCS7Padding padding = new PKCS7Padding();
            CipherParameters params = new ParametersWithIV(new KeyParameter(key), iv);
            blockCipher.init(true, params);

            int blockSize = blockCipher.getBlockSize();
            int paddedDataLength = (data.length + blockSize - 1) / blockSize * blockSize;
            byte[] paddedData = Arrays.copyOf(data, paddedDataLength);

            byte[] encryptedData = new byte[paddedDataLength];
            for (int i = 0; i < paddedDataLength; i += blockSize) {
                blockCipher.processBlock(paddedData, i, encryptedData, i);
            }

            return encryptedData;
        } catch (Exception e) {
            throw new RuntimeException("SM4 encryption failed", e);
        }
    }

    // SM4解密算法
    public static byte[] sm4Decrypt(byte[] key, byte[] iv, byte[] encryptedData) {
        try {
            // SM4 CBC模式解密
            SM4Engine engine = new SM4Engine();
            CBCBlockCipher blockCipher = new CBCBlockCipher(engine);
            PKCS7Padding padding = new PKCS7Padding();
            CipherParameters params = new ParametersWithIV(new KeyParameter(key), iv);
            blockCipher.init(false, params);

            int blockSize = blockCipher.getBlockSize();
            byte[] decryptedData = new byte[encryptedData.length];

            for (int i = 0; i < encryptedData.length; i += blockSize) {
                blockCipher.processBlock(encryptedData, i, decryptedData, i);
            }

            return decryptedData;
        } catch (Exception e) {
            throw new RuntimeException("SM4 decryption failed", e);
        }
    }

    public static void main(String[] args) {
        try {
            // 示例密钥和IV（向量）16字节（128位）
            String key = "0123456789abcdeffedcba9876543210";
            String iv = "fedcba98765432100123456789abcdef"; // 初始向量（IV）

            byte[] keyBytes = Hex.decode(key);
            byte[] ivBytes = Hex.decode(iv);

            // 原始数据
            String originalData = "Hello";
            byte[] data = originalData.getBytes();

            // 加密
            byte[] encryptedData = sm4Encrypt(keyBytes, ivBytes, data);
            System.out.println("Encrypted Data: " + Hex.toHexString(encryptedData));

            // 解密
            byte[] decryptedData = sm4Decrypt(keyBytes, ivBytes, encryptedData);
            String decryptedText = new String(decryptedData).trim();
            System.out.println("Decrypted Data: " + decryptedText);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

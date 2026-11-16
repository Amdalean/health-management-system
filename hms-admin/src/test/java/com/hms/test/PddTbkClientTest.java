package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.hms.main.tbk.client.PddTbkClient;

/**
 * 拼多多签名测试
 *
 * add by CYQ 2026-11-03 转链模块支持拼多多
 */
public class PddTbkClientTest {

    /**
     * 固定参数验签：secret + 排序后的 k1v1k2v2 + secret 取 MD5 大写
     */
    @Test
    public void testSign() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "pdd.time.get");
        params.put("client_id", "cid");
        params.put("timestamp", "1700000000");
        params.put("data_type", "JSON");
        params.put("version", "V1");
        assertEquals("7E8F9DB0035CC5071A52E8FA7F1CFB4A", PddTbkClient.sign(params, "testsecret"));
    }

    @Test
    public void testSignSkipsSignField() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "pdd.time.get");
        params.put("client_id", "cid");
        params.put("timestamp", "1700000000");
        params.put("data_type", "JSON");
        params.put("version", "V1");
        params.put("sign", "IGNORED");
        assertEquals("7E8F9DB0035CC5071A52E8FA7F1CFB4A", PddTbkClient.sign(params, "testsecret"));
    }
}

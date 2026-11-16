package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.hms.main.tbk.client.TopClient;

/**
 * TOP 网关签名测试
 *
 * 期望值由独立实现（Node.js crypto）交叉计算得出，用于保证 Java 侧签名算法正确。
 *
 * add by CYQ 2026-11-03 淘宝客转链模块
 */
public class TbkTopSignTest {

    @Test
    public void testSignMatchesTopSpec() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", "taobao.time.get");
        params.put("app_key", "35490624");
        params.put("timestamp", "2026-11-03 00:00:00");
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");

        String sign = TopClient.sign(params, "testsecret");
        assertEquals("208A27D59C6606EE16D0A595C3C0B4DA", sign);
    }

    @Test
    public void testSignIgnoresExistingSignParam() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", "taobao.time.get");
        params.put("app_key", "35490624");
        params.put("timestamp", "2026-11-03 00:00:00");
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");

        String expected = TopClient.sign(params, "testsecret");
        params.put("sign", "SHOULD_BE_IGNORED");
        assertEquals(expected, TopClient.sign(params, "testsecret"));
    }

    /**
     * 回归：时间戳里的空格必须编码成 %20，不能是 '+'，否则淘宝返回 Invalid timestamp(code=31)
     */
    @Test
    public void testQueryStringEncodesSpaceAsPercent20() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("timestamp", "2026-11-03 00:00:00");
        params.put("method", "taobao.time.get");

        String query = TopClient.toQueryString(params);
        assertEquals("timestamp=2026-11-03%2000%3A00%3A00&method=taobao.time.get", query);
    }
}

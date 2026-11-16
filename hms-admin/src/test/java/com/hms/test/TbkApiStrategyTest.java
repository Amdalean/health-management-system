package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.hms.common.annotation.IntegrationLog;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.integration.PddDdkApiRequest;
import com.hms.main.tbk.integration.PddDdkApiStrategy;
import com.hms.main.tbk.integration.TaobaoTopApiRequest;
import com.hms.main.tbk.integration.TaobaoTopApiStrategy;
import com.hms.main.tbk.integration.TbkApiSigner;

/**
 * 淘宝/拼多多接口策略类测试
 *
 * 覆盖：公共参数组装、签名可复算、日志字段脱敏、@IntegrationLog 元数据。
 *
 * add by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志
 */
public class TbkApiStrategyTest {

    // ------------------------------------------------------------------ 淘宝 TOP

    @Test
    public void testTaobaoBuildParams() {
        TbkProperties.Taobao taobao = new TbkProperties().getTaobao();
        taobao.setAppKey("appkey");
        taobao.setAppSecret("secret");

        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("item_id", "123456");
        TaobaoTopApiRequest request = new TaobaoTopApiRequest("taobao.tbk.item.info.get", biz, null);

        Map<String, String> params = new TaobaoTopApiStrategy().buildParams(request, taobao);

        assertEquals("taobao.tbk.item.info.get", params.get("method"));
        assertEquals("appkey", params.get("app_key"));
        assertEquals("json", params.get("format"));
        assertEquals("2.0", params.get("v"));
        assertEquals("md5", params.get("sign_method"));
        assertEquals("123456", params.get("item_id"));
        assertFalse(params.containsKey("session"));
        // TOP 时间戳必须是 yyyy-MM-dd HH:mm:ss（GMT+8）
        assertTrue(params.get("timestamp").matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));

        // 签名可复算：与商用签名工具的规则一致
        assertNotNull(params.get("sign"));
        assertEquals(32, params.get("sign").length());
        assertEquals(TbkApiSigner.sign(params, "secret"), params.get("sign"));
    }

    @Test
    public void testTaobaoBuildParamsWithSession() {
        TbkProperties.Taobao taobao = new TbkProperties().getTaobao();
        taobao.setAppKey("appkey");
        taobao.setAppSecret("secret");

        TaobaoTopApiRequest request = new TaobaoTopApiRequest("taobao.tbk.privilege.get", null, "session-abc-123");
        Map<String, String> params = new TaobaoTopApiStrategy().buildParams(request, taobao);

        assertEquals("session-abc-123", params.get("session"));
        assertEquals(TbkApiSigner.sign(params, "secret"), params.get("sign"));
    }

    @Test
    public void testTaobaoLogFields() {
        TaobaoTopApiStrategy strategy = new TaobaoTopApiStrategy();
        TaobaoTopApiRequest request = new TaobaoTopApiRequest("taobao.tbk.item.info.get", null, "token-1234567890");
        request.setBillId("654321");

        assertEquals("654321", strategy.resolveBillId(request));
        assertEquals("token-1234567890", strategy.resolveResponseMessage(request.getSession()));

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) strategy.resolveSendMessage(request);
        assertEquals("taobao.tbk.item.info.get", message.get("method"));
        // session 必须脱敏，不能把 OAuth token 落库
        assertEquals("toke****7890", message.get("session"));
    }

    @Test
    public void testTaobaoBillIdFromBiz() {
        TaobaoTopApiStrategy strategy = new TaobaoTopApiStrategy();
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("goods_sign", "GS-10086");
        TaobaoTopApiRequest request = new TaobaoTopApiRequest("taobao.tbk.dg.material.optional.upgrade", biz, null);

        assertEquals("GS-10086", strategy.resolveBillId(request));
    }

    @Test
    public void testTaobaoTopAnnotation() {
        IntegrationLog annotation = TaobaoTopApiStrategy.class.getAnnotation(IntegrationLog.class);
        assertNotNull(annotation, "策略类必须标注 @IntegrationLog 才会记录集成日志");
        assertEquals("淘宝TOP接口", annotation.title());
        assertEquals("TAOBAO_TOP", annotation.bizType());
        assertEquals("淘宝开放平台", annotation.receiver());
    }

    // ------------------------------------------------------------------ 拼多多 DDK

    @Test
    public void testPddBuildParams() {
        TbkProperties.Pdd pdd = new TbkProperties().getPdd();
        pdd.setClientId("cid");
        pdd.setClientSecret("csecret");

        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("goods_sign", "GS-10086");
        PddDdkApiRequest request = new PddDdkApiRequest("pdd.ddk.goods.detail", biz);

        Map<String, String> params = new PddDdkApiStrategy().buildParams(request, pdd);

        assertEquals("pdd.ddk.goods.detail", params.get("type"));
        assertEquals("cid", params.get("client_id"));
        assertEquals("JSON", params.get("data_type"));
        assertEquals("V1", params.get("version"));
        assertEquals("GS-10086", params.get("goods_sign"));
        // 拼多多时间戳是秒级 unix 时间
        assertTrue(params.get("timestamp").matches("\\d{10}"));

        assertNotNull(params.get("sign"));
        assertEquals(32, params.get("sign").length());
        assertEquals(TbkApiSigner.sign(params, "csecret"), params.get("sign"));
    }

    @Test
    public void testPddLogFields() {
        PddDdkApiStrategy strategy = new PddDdkApiStrategy();
        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("goods_sign", "GS-10086");
        PddDdkApiRequest request = new PddDdkApiRequest("pdd.ddk.goods.detail", biz);

        assertEquals("GS-10086", strategy.resolveBillId(request));

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) strategy.resolveSendMessage(request);
        assertEquals("pdd.ddk.goods.detail", message.get("type"));
        assertEquals(biz, message.get("biz"));
        // 没有注入配置时不能出现 client_id，更不能出现 client_secret
        assertFalse(message.containsKey("client_secret"));
        assertFalse(message.containsKey("client_id"));

        assertEquals("body", strategy.resolveResponseMessage("body"));
    }

    @Test
    public void testPddAnnotation() {
        IntegrationLog annotation = PddDdkApiStrategy.class.getAnnotation(IntegrationLog.class);
        assertNotNull(annotation, "策略类必须标注 @IntegrationLog 才会记录集成日志");
        assertEquals("拼多多DDK接口", annotation.title());
        assertEquals("PDD_DDK", annotation.bizType());
        assertEquals("拼多多开放平台", annotation.receiver());
    }

    // ------------------------------------------------------------------ 公共签名工具

    @Test
    public void testSignerQueryStringEncodesSpace() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("timestamp", "2026-11-10 20:00:00");
        params.put("method", "taobao.time.get");
        assertEquals("timestamp=2026-11-10%2020%3A00%3A00&method=taobao.time.get",
                TbkApiSigner.toQueryString(params));
    }

    @Test
    public void testSignerMask() {
        assertEquals(null, TbkApiSigner.mask(null));
        assertEquals("****", TbkApiSigner.mask("short"));
        assertEquals("abcd****wxyz", TbkApiSigner.mask("abcdefghijklmnwxyz"));
    }

    // ------------------------------------------------------------------ Spring 注册

    /**
     * @IntegrationLog 上集成了 @Component，策略类应能被组件扫描自动注册为 Bean，
     * 业务类才能直接 @Autowired 注入。这里用最小 Spring 容器验证一次，避免以后改注解踩坑。
     */
    @Test
    public void testStrategiesRegisteredAsSpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TbkProperties.class);
            context.scan("com.hms.main.tbk.integration");
            context.refresh();

            assertNotNull(context.getBean(TaobaoTopApiStrategy.class));
            assertNotNull(context.getBean(PddDdkApiStrategy.class));
        }
    }
}

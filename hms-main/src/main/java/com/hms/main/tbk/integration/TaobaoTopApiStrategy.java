package com.hms.main.tbk.integration;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import com.hms.common.annotation.IntegrationLog;
import com.hms.common.integration.AbstractIntegrationStrategy;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.client.TopApiException;
import com.hms.main.tbk.config.TbkProperties;

/**
 * 淘宝开放平台（TOP）接口策略类
 * <p>
 * 所有对淘宝开放平台的接口调用（转链、物料搜索、商品信息、淘口令、订单同步等）
 * 统一通过 {@link #execute(TaobaoTopApiRequest)} 发起：
 * <ul>
 *     <li>公共参数组装与签名在 {@link #buildParams(TaobaoTopApiRequest, TbkProperties.Taobao)} 内完成；</li>
 *     <li>HTTP 调用与失败兜底在 {@link #doSend(TaobaoTopApiRequest)} 内完成；</li>
 *     <li>类上的 {@link IntegrationLog} 让切面自动把请求参数、响应报文、耗时、成功/失败
 *         异步落库到 {@code hms_integration_log}（监控-集成日志页面可查）。</li>
 * </ul>
 * 请求日志里的 session（OAuth token）会脱敏，app_secret 不进入日志。
 * <p>
 * add by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志
 */
@IntegrationLog(title = "淘宝TOP接口", bizType = "TAOBAO_TOP", sender = "HMS", receiver = "淘宝开放平台")
public class TaobaoTopApiStrategy extends AbstractIntegrationStrategy<TaobaoTopApiRequest, String> {

    private static final Logger log = LoggerFactory.getLogger(TaobaoTopApiStrategy.class);

    /** 日志/异常提示里截断响应体的长度 */
    private static final int LOG_BODY_LENGTH = 1500;

    /** 单据标识候选参数名（按顺序查找，仅用于集成日志排查） */
    private static final String[] BILL_ID_KEYS = { "item_id", "num_iid", "goods_sign", "goods_sign_list", "order_sn", "tid" };

    @Autowired
    private TbkProperties properties;

    private final RestTemplate restTemplate;

    public TaobaoTopApiStrategy() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    protected String doSend(TaobaoTopApiRequest request) {
        TbkProperties.Taobao taobao = properties.getTaobao();
        Map<String, String> params = buildParams(request, taobao);
        String url = taobao.getGateway() + "?" + TbkApiSigner.toQueryString(params);

        String body;
        try {
            // 用 URI 对象发请求，避免 RestTemplate 对已编码的 query 二次编码
            body = restTemplate.getForObject(URI.create(url), String.class);
        } catch (Exception e) {
            log.error("淘宝TOP接口请求失败 method={}: {}", request.getMethod(), e.getMessage());
            throw new TopApiException("-1", "", "淘宝接口请求失败，请稍后重试");
        }
        if (StringUtils.isEmpty(body)) {
            throw new TopApiException("-1", "", "淘宝接口返回为空");
        }
        log.info("淘宝TOP接口响应 method={} length={} body={}", request.getMethod(), body.length(), trim(body));
        return body;
    }

    /**
     * 组装 TOP 公共参数 + 业务参数 + 签名
     * <p>
     * 抽成 public 便于单元测试直接校验验签结果，不依赖真实网络。
     *
     * @param request 接口请求
     * @param taobao  淘宝开放平台配置
     * @return 完整参数（含 sign）
     */
    public Map<String, String> buildParams(TaobaoTopApiRequest request, TbkProperties.Taobao taobao) {
        if (request == null || StringUtils.isEmpty(request.getMethod())) {
            throw new TopApiException("-1", "", "淘宝接口请求参数为空");
        }
        if (taobao == null || StringUtils.isEmpty(taobao.getAppKey()) || StringUtils.isEmpty(taobao.getAppSecret())) {
            throw new TopApiException("-1", "", "尚未配置淘宝开放平台 appKey/appSecret");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", request.getMethod());
        params.put("app_key", taobao.getAppKey());
        params.put("timestamp", TbkApiSigner.topTimestamp());
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");
        if (StringUtils.isNotEmpty(request.getSession())) {
            params.put("session", request.getSession());
        }
        if (request.getBiz() != null) {
            params.putAll(request.getBiz());
        }
        params.put("sign", TbkApiSigner.sign(params, taobao.getAppSecret()));
        return params;
    }

    @Override
    public String resolveBillId(TaobaoTopApiRequest request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(request.getBillId())) {
            return request.getBillId();
        }
        Map<String, String> biz = request.getBiz();
        if (biz == null) {
            return null;
        }
        for (String key : BILL_ID_KEYS) {
            String value = biz.get(key);
            if (StringUtils.isNotEmpty(value)) {
                return limitBillId(value);
            }
        }
        return null;
    }

    /**
     * bill_id 数据库列只有 100 字符，超长值直接截断，避免异步写日志时插入失败
     */
    private String limitBillId(String value) {
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    /**
     * 日志里的请求报文：只保留接口名、业务参数和脱敏后的 session，不落 app_secret
     */
    @Override
    public Object resolveSendMessage(TaobaoTopApiRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("method", request.getMethod());
        message.put("biz", request.getBiz());
        if (StringUtils.isNotEmpty(request.getSession())) {
            message.put("session", TbkApiSigner.mask(request.getSession()));
        }
        return message;
    }

    @Override
    public Object resolveResponseMessage(String response) {
        return response;
    }

    private String trim(String body) {
        return body.length() > LOG_BODY_LENGTH ? body.substring(0, LOG_BODY_LENGTH) : body;
    }
}

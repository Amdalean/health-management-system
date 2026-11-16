 package com.hms.main.tbk.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.hms.common.annotation.IntegrationLog;
import com.hms.common.exception.ServiceException;
import com.hms.common.integration.AbstractIntegrationStrategy;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;

/**
 * 拼多多「多多进宝」（DDK）接口策略类
 * <p>
 * 所有对拼多多开放平台的接口调用（商品详情、推广链接、搜索、订单查询、推广位查询、
 * 授权备案查询等）统一通过 {@link #execute(PddDdkApiRequest)} 发起：
 * <ul>
 *     <li>公共参数组装与签名在 {@link #buildParams(PddDdkApiRequest, TbkProperties.Pdd)} 内完成；</li>
 *     <li>HTTP 调用与失败兜底在 {@link #doSend(PddDdkApiRequest)} 内完成；</li>
 *     <li>类上的 {@link IntegrationLog} 让切面自动把请求参数、响应报文、耗时、成功/失败
 *         异步落库到 {@code hms_integration_log}（监控-集成日志页面可查）。</li>
 * </ul>
 * 请求日志里的 client_id 会脱敏，client_secret 不进入日志。
 * <p>
 * add by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志
 */
@IntegrationLog(title = "拼多多DDK接口", bizType = "PDD_DDK", sender = "HMS", receiver = "拼多多开放平台")
public class PddDdkApiStrategy extends AbstractIntegrationStrategy<PddDdkApiRequest, String> {

    private static final Logger log = LoggerFactory.getLogger(PddDdkApiStrategy.class);

    /** 日志里截断响应体的长度 */
    private static final int LOG_BODY_LENGTH = 800;

    /** 单据标识候选参数名（按顺序查找，仅用于集成日志排查） */
    private static final String[] BILL_ID_KEYS = { "goods_sign", "goods_sign_list", "order_sn", "p_id", "pid" };

    @Autowired
    private TbkProperties properties;

    private final RestTemplate restTemplate;

    public PddDdkApiStrategy() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    protected String doSend(PddDdkApiRequest request) {
        TbkProperties.Pdd pdd = properties.getPdd();
        Map<String, String> params = buildParams(request, pdd);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        String body;
        try {
            body = restTemplate.postForObject(pdd.getGateway(), new HttpEntity<>(form, headers), String.class);
        } catch (Exception e) {
            log.error("拼多多DDK接口请求失败 type={}: {}", request.getType(), e.getMessage());
            throw new ServiceException("拼多多转链服务暂时不可用，请稍后重试");
        }
        if (StringUtils.isEmpty(body)) {
            throw new ServiceException("拼多多接口返回为空，请稍后重试");
        }
        log.info("拼多多DDK接口响应 type={} length={} body={}", request.getType(), body.length(), trim(body));
        return body;
    }

    /**
     * 组装 DDK 公共参数 + 业务参数 + 签名
     * <p>
     * 抽成 public 便于单元测试直接校验验签结果，不依赖真实网络。
     *
     * @param request 接口请求
     * @param pdd     拼多多应用配置
     * @return 完整参数（含 sign）
     */
    public Map<String, String> buildParams(PddDdkApiRequest request, TbkProperties.Pdd pdd) {
        if (request == null || StringUtils.isEmpty(request.getType())) {
            throw new ServiceException("拼多多接口请求参数为空");
        }
        if (pdd == null || StringUtils.isEmpty(pdd.getClientId()) || StringUtils.isEmpty(pdd.getClientSecret())) {
            throw new ServiceException("尚未配置拼多多应用，请填写 tbk.pdd.client-id / client-secret");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", request.getType());
        params.put("client_id", pdd.getClientId());
        params.put("timestamp", TbkApiSigner.unixTimestamp());
        params.put("data_type", "JSON");
        params.put("version", "V1");
        if (request.getBiz() != null) {
            params.putAll(request.getBiz());
        }
        params.put("sign", TbkApiSigner.sign(params, pdd.getClientSecret()));
        return params;
    }

    @Override
    public String resolveBillId(PddDdkApiRequest request) {
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
     * bill_id 数据库列只有 100 字符，超长值（如 goods_sign_list 的 JSON 串）直接截断，
     * 避免异步写日志时插入失败
     */
    private String limitBillId(String value) {
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    /**
     * 日志里的请求报文：只保留接口名、业务参数和脱敏后的 client_id，不落 client_secret
     */
    @Override
    public Object resolveSendMessage(PddDdkApiRequest request) {
        if (request == null) {
            return null;
        }
        TbkProperties.Pdd pdd = properties == null ? null : properties.getPdd();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", request.getType());
        message.put("biz", request.getBiz());
        if (pdd != null && StringUtils.isNotEmpty(pdd.getClientId())) {
            message.put("client_id", TbkApiSigner.mask(pdd.getClientId()));
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

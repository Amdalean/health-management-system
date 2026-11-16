package com.hms.main.tbk.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * 淘宝开放平台（TOP）接口请求
 * <p>
 * 只描述「调哪个接口 / 带哪些业务参数 / 用哪个 session」，
 * 公共参数、签名、HTTP 调用和日志统一由 {@link TaobaoTopApiStrategy} 处理。
 * <p>
 * add by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志
 */
@Data
public class TaobaoTopApiRequest {

    /** TOP 接口名，如 taobao.tbk.item.info.get */
    private String method;

    /** 业务参数 */
    private Map<String, String> biz = new LinkedHashMap<>();

    /** 淘宝 OAuth session，可空 */
    private String session;

    /**
     * 单据标识（商品ID / 订单号等），仅用于集成日志排查，可空
     */
    private String billId;

    public TaobaoTopApiRequest() {
    }

    public TaobaoTopApiRequest(String method, Map<String, String> biz, String session) {
        this.method = method;
        this.biz = biz;
        this.session = session;
    }
}

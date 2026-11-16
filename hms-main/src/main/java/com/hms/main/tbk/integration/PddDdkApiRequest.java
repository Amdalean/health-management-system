package com.hms.main.tbk.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * 拼多多「多多进宝」接口请求
 * <p>
 * 只描述「调哪个接口 / 带哪些业务参数」，公共参数、签名、HTTP 调用和日志
 * 统一由 {@link PddDdkApiStrategy} 处理。
 * <p>
 * add by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志
 */
@Data
public class PddDdkApiRequest {

    /** 多多进宝接口名，如 pdd.ddk.goods.detail */
    private String type;

    /** 业务参数 */
    private Map<String, String> biz = new LinkedHashMap<>();

    /**
     * 单据标识（goods_sign / goods_sign_list / 订单号等），仅用于集成日志排查，可空
     */
    private String billId;

    public PddDdkApiRequest() {
    }

    public PddDdkApiRequest(String type, Map<String, String> biz) {
        this.type = type;
        this.biz = biz;
    }
}

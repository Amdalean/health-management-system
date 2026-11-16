package com.hms.main.tbk.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.integration.TaobaoTopApiRequest;
import com.hms.main.tbk.integration.TaobaoTopApiStrategy;
import com.hms.main.tbk.integration.TbkApiSigner;

/**
 * 淘宝开放平台（TOP）网关客户端
 *
 * 签名算法：md5(appSecret + 按参数名排序拼接的 k1v1k2v2... + appSecret) 转大写
 * 已在 AppKey 35490624 上实测通过（taobao.time.get / taobao.tbk.spread.get）。
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 *
 * modify by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志：
 * 真正的公共参数组装、签名、HTTP 调用与集成日志已收敛到 {@link TaobaoTopApiStrategy}，
 * 本类只负责响应解析与错误映射，原有 {@link #execute(String, Map, String)} 调用方式保持不变。
 */
@Component
public class TopClient {

    private static final Logger log = LoggerFactory.getLogger(TopClient.class);

    @Autowired
    private TaobaoTopApiStrategy taobaoTopApiStrategy;

    /**
     * 调用 TOP 接口
     *
     * @param method  接口名，如 taobao.tbk.item.info.get
     * @param biz     业务参数
     * @param session 授权 session，可为 null
     * @return 去掉外层 xxx_response 之后的数据节点
     */
    public JSONObject execute(String method, Map<String, String> biz, String session) {
        String body;
        try {
            body = taobaoTopApiStrategy.execute(new TaobaoTopApiRequest(method, biz, session));
        } catch (TopApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("淘宝TOP接口调用失败 method={}: {}", method, e.getMessage());
            throw new TopApiException("-1", "", "淘宝接口调用失败，请稍后重试");
        }

        JSONObject root;
        try {
            root = JSON.parseObject(body);
        } catch (Exception e) {
            log.error("淘宝TOP接口返回解析失败 method={}: {}", method, e.getMessage());
            throw new TopApiException("-1", "", "淘宝接口返回格式异常");
        }
        if (root == null) {
            throw new TopApiException("-1", "", "淘宝接口返回为空");
        }
        JSONObject error = root.getJSONObject("error_response");
        if (error != null) {
            String code = error.getString("code");
            String subCode = error.getString("sub_code");
            String msg = StringUtils.isNotEmpty(error.getString("sub_msg"))
                    ? error.getString("sub_msg") : error.getString("msg");
            throw new TopApiException(code, subCode, msg);
        }

        // TOP 返回的外层节点会去掉命名空间前缀：
        // taobao.tbk.item.info.get -> tbk_item_info_get_response
        String shortName = method.startsWith("taobao.") ? method.substring("taobao.".length()) : method;
        JSONObject response = root.getJSONObject(shortName.replace('.', '_') + "_response");
        if (response == null) {
            response = root.getJSONObject(method.replace('.', '_') + "_response");
        }
        return response == null ? root : response;
    }

    /**
     * TOP 标准签名
     *
     * 保留静态方法兼容老调用与单元测试，实际实现已收敛到 {@link TbkApiSigner}。
     */
    public static String sign(Map<String, String> params, String secret) {
        return TbkApiSigner.sign(params, secret);
    }

    /**
     * TOP 网关 query string
     *
     * 保留静态方法兼容老调用与单元测试，实际实现已收敛到 {@link TbkApiSigner}。
     */
    public static String toQueryString(Map<String, String> params) {
        return TbkApiSigner.toQueryString(params);
    }
}

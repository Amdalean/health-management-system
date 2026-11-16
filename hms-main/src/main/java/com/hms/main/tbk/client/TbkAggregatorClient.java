package com.hms.main.tbk.client;

import com.hms.main.tbk.domain.TbkConvertResult;
import com.hms.main.tbk.domain.TbkPickItem;
import java.util.Collections;
import java.util.List;

/**
 * 淘宝客聚合平台转链通道
 *
 * 新增通道（大淘客/好单库等）只需实现本接口并加 @ConditionalOnProperty 即可。
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
public interface TbkAggregatorClient {

    /**
     * 转链
     *
     * @param material 链接 / 淘口令 / 商品ID
     * @param itemId   已解析出的商品ID，可能为 null
     * @return 转链结果
     */
    TbkConvertResult convert(String material, String itemId);

    /**
     * 转链（带分享文案里的标题）
     *
     * 官方通道在"商品ID不能直接用"的场景下可以靠标题做物料搜索，
     * 其他通道忽略标题即可。
     *
     * add by CYQ 2026-11-03
     *
     * @param material 链接 / 淘口令 / 商品ID
     * @param itemId   已解析出的商品ID，可能为 null
     * @param title    从分享文案里提取的商品标题，可能为 null
     * @return 转链结果
     */
    default TbkConvertResult convert(String material, String itemId, String title) {
        return convert(material, itemId);
    }

    /**
     * 通道名称
     */
    String providerName();

    /**
     * 按关键词搜索商品（AI 选品用）
     *
     * 默认返回空列表：未实现搜索能力的通道（mock / ztk）不影响选品流程，
     * 由调用方按平台分别降级。
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     *
     * @param keyword 平台搜索词
     * @param limit   最大返回条数
     * @return 候选商品，无结果或通道不支持时返回空列表
     */
    default List<TbkPickItem> search(String keyword, int limit) {
        return Collections.emptyList();
    }
}

package com.hms.main.tbk.mapper;

import java.util.List;
import java.util.Map;
import com.hms.main.tbk.domain.TbkOrder;

/**
 * 联盟订单 Mapper
 *
 * add by CYQ 2026-11-09 联盟收益 PC 看板
 */
public interface TbkOrderMapper {

    /**
     * 幂等写入：按 平台 + 订单号 唯一键 upsert
     */
    int upsertTbkOrder(TbkOrder order);

    /**
     * 订单列表
     */
    List<TbkOrder> selectTbkOrderList(TbkOrder query);

    /**
     * 汇总统计（今日/本月/累计）
     */
    Map<String, Object> selectTbkOrderStats(TbkOrder query);

    /**
     * 按平台汇总
     */
    List<Map<String, Object>> selectPlatformStats(TbkOrder query);
}

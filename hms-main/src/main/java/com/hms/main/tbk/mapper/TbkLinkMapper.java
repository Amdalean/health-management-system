package com.hms.main.tbk.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.hms.main.tbk.domain.TbkLink;
import com.hms.main.tbk.domain.TbkOrder;

/**
 * 推广链接台账 Mapper
 *
 * add by CYQ 2026-11-09 链接台账与返现估算
 */
public interface TbkLinkMapper {

    /**
     * 记录一条链接
     */
    int insertTbkLink(TbkLink link);

    /**
     * 查询订单付款时间之前、时间窗内、同商品的链接（按生成时间倒序）
     */
    List<TbkLink> selectCandidates(@Param("order") TbkOrder order, @Param("windowDays") int windowDays);
}

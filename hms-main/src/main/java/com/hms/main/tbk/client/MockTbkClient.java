package com.hms.main.tbk.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.hms.common.exception.ServiceException;
import com.hms.main.tbk.domain.TbkConvertResult;

/**
 * 演示通道：没有聚合平台密钥时使用，返回模拟的券后价数据。
 *
 * 仅用于跑通前后端链路；接入真实密钥后把 tbk.provider 改成 ztk 即可切换。
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Component
@ConditionalOnProperty(prefix = "tbk", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockTbkClient implements TbkAggregatorClient {

    @Override
    public TbkConvertResult convert(String material, String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            throw new ServiceException("演示模式只能识别商品链接或商品ID，接入真实转链通道后即可支持淘口令和短链");
        }

        int hash = Math.abs(itemId.hashCode());
        BigDecimal originalPrice = BigDecimal.valueOf(39 + hash % 260)
                .add(BigDecimal.valueOf(hash % 100).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal couponAmount = BigDecimal.valueOf(3 + hash % 27);
        BigDecimal finalPrice = originalPrice.subtract(couponAmount).max(BigDecimal.ONE);
        BigDecimal commissionRate = BigDecimal.valueOf(5 + hash % 16);

        TbkConvertResult result = new TbkConvertResult();
        result.setItemId(itemId);
        result.setTitle("演示商品 · 淘宝ID " + itemId);
        result.setPicUrl("");
        result.setOriginalPrice(originalPrice);
        result.setCouponAmount(couponAmount);
        result.setFinalPrice(finalPrice);
        result.setCommissionRate(commissionRate);
        result.setCommission(finalPrice.multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        result.setItemUrl("https://item.taobao.com/item.htm?id=" + itemId);
        result.setShortUrl(result.getItemUrl());
        result.setTpwd("￥DEMO" + itemId.substring(Math.max(0, itemId.length() - 4)) + "￥");
        result.setCouponInfo("满" + originalPrice + "元减" + couponAmount + "元");
        result.setDemo(true);
        result.setProvider("mock");
        result.setNotice("当前为演示模式，数据为模拟生成；配置聚合平台密钥后自动切换真实转链");
        return result;
    }

    @Override
    public String providerName() {
        return "mock";
    }
}

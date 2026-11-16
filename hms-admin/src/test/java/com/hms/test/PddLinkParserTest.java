package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.hms.main.tbk.client.PddLinkParser;

/**
 * 拼多多链接识别测试
 *
 * add by CYQ 2026-11-03 转链模块支持拼多多
 */
public class PddLinkParserTest {

    @Test
    public void testIsPddMaterial() {
        assertTrue(PddLinkParser.isPddMaterial("https://p.pinduoduo.com/AbCdEf12"));
        assertTrue(PddLinkParser.isPddMaterial("https://mobile.yangkeduo.com/goods.html?goods_id=123456789"));
        assertTrue(PddLinkParser.isPddMaterial("https://yangkeduo.com/duo_coupon_landing.html?goods_id=123456789"));
        assertFalse(PddLinkParser.isPddMaterial("https://item.taobao.com/item.htm?id=123456789"));
        assertFalse(PddLinkParser.isPddMaterial("123456789"));
        assertFalse(PddLinkParser.isPddMaterial(null));
    }

    @Test
    public void testExtractGoodsIdFromUrl() {
        PddLinkParser.PddGoodsRef ref =
                PddLinkParser.fromText("https://mobile.yangkeduo.com/goods.html?goods_id=123456789&refer_page_name=goods");
        assertNotNull(ref);
        assertEquals("123456789", ref.getGoodsId());
        assertTrue(ref.isNumericId());
    }

    @Test
    public void testExtractGoodsSign() {
        PddLinkParser.PddGoodsRef ref =
                PddLinkParser.fromText("https://mobile.yangkeduo.com/goods.html?goods_sign=abcDEF1234567890_-xyz");
        assertNotNull(ref);
        assertEquals("abcDEF1234567890_-xyz", ref.getGoodsSign());
        assertFalse(ref.isNumericId());
    }

    @Test
    public void testPreferGoodsSignWhenBothPresent() {
        PddLinkParser.PddGoodsRef ref = PddLinkParser.fromText(
                "https://mobile.yangkeduo.com/goods.html?goods_id=787482530251&goods_sign=E9X2FTPTL19tUeAVwfvBVy-BhTyxKejJ");
        assertNotNull(ref);
        assertEquals("E9X2FTPTL19tUeAVwfvBVy-BhTyxKejJ", ref.getGoodsSign());
        assertEquals("787482530251", ref.getGoodsId());
    }

    @Test
    public void testExtractFromPageHtml() {
        PddLinkParser.PddGoodsRef ref = PddLinkParser.fromText(
                "<script>window.rawData={\"goods\":{\"goodsID\":\"987654321\",\"goodsName\":\"测试\"}}</script>");
        assertNotNull(ref);
        assertEquals("987654321", ref.getGoodsId());
    }

    @Test
    public void testNotPdd() {
        assertNull(PddLinkParser.fromText("https://item.taobao.com/item.htm?id=123456789"));
        assertNull(PddLinkParser.fromText("今天天气不错"));
        assertNull(PddLinkParser.fromText(""));
    }
}

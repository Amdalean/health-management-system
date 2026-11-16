package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import com.hms.main.tbk.client.TbkLinkParser;
import com.hms.main.tbk.domain.TbkMaterial;

/**
 * 淘宝链接/淘口令识别测试
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
public class TbkLinkParserTest {

    @Test
    public void testParseItemUrl() {
        TbkMaterial material = TbkLinkParser.parse("https://item.taobao.com/item.htm?id=623456789012");
        assertNotNull(material);
        assertEquals("url", material.getType());
        assertEquals("623456789012", material.getItemId());
    }

    @Test
    public void testParseUrlWithTextAround() {
        TbkMaterial material = TbkLinkParser.parse(
                "【淘宝】超值好物 https://detail.tmall.com/item.htm?id=698765432109&spm=a1z10 点击链接直接打开");
        assertNotNull(material);
        assertEquals("https://detail.tmall.com/item.htm?id=698765432109&spm=a1z10", material.getMaterial());
        assertEquals("698765432109", material.getItemId());
    }

    @Test
    public void testParseTpwd() {
        TbkMaterial material = TbkLinkParser.parse("复制这条信息，￥AbCdEf1234￥，打开手机淘宝即可");
        assertNotNull(material);
        assertEquals("tkl", material.getType());
        assertEquals("￥AbCdEf1234￥", material.getMaterial());
    }

    @Test
    public void testParseShortUrl() {
        TbkMaterial material = TbkLinkParser.parse("https://m.tb.cn/h.5xAbCdE");
        assertNotNull(material);
        assertEquals("url", material.getType());
        assertNull(material.getItemId());
    }

    /**
     * e.tb.cn 新版分享短链：链接里没有商品ID，需要落地页解析
     * add by CYQ 2026-11-03
     */
    @Test
    public void testParseNewShareShortUrl() {
        TbkMaterial material = TbkLinkParser.parse(
                "【淘宝】https://e.tb.cn/h.jSWxQ20GToKijKx?tk=jIfP6ZM3eqC CZ009 「测试商品」 点击链接直接打开");
        assertNotNull(material);
        assertEquals("url", material.getType());
        assertEquals("https://e.tb.cn/h.jSWxQ20GToKijKx?tk=jIfP6ZM3eqC", material.getMaterial());
        assertNull(material.getItemId());
    }

    @Test
    public void testParsePureId() {
        TbkMaterial material = TbkLinkParser.parse("623456789012");
        assertNotNull(material);
        assertEquals("id", material.getType());
        assertEquals("623456789012", material.getItemId());
    }

    @Test
    public void testParseInvalid() {
        assertNull(TbkLinkParser.parse("今天天气不错"));
        assertNull(TbkLinkParser.parse(""));
        assertNull(TbkLinkParser.parse(null));
    }
}

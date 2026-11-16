package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import com.hms.main.tbk.client.TbkShortLinkResolver;

/**
 * 短链落地页商品ID解析测试
 *
 * add by CYQ 2026-11-03 修复 e.tb.cn 短链解析不到商品ID
 */
public class TbkShortLinkResolverTest {

    /**
     * e.tb.cn 短链返回 200 HTML，真实地址写在 var url 里
     */
    @Test
    public void testExtractFromETbCnHtml() {
        String html = "<html><head><script>\n"
                + "  var bizCode=\"tbshare\";\n"
                + "  var shortName = \"h.jSWxQ20GToKijKx\";\n"
                + "  var url = 'https://item.taobao.com/item.htm?id=572209212242&price=310"
                + "&spm=a2159r.13376460.0.0&short_name=h.jSWxQ20GToKijKx';\n"
                + "  var invalidUrl = \"http://m.tb.cn/scanError.htm\";\n"
                + "</script></head><body></body></html>";
        assertEquals("572209212242", TbkShortLinkResolver.extractItemIdFromHtml(html));
    }

    /**
     * 天猫详情页 / 老版手机页
     */
    @Test
    public void testExtractFromOtherPageTypes() {
        assertEquals("698765432109", TbkShortLinkResolver.extractItemIdFromHtml(
                "<a href=\"https://detail.tmall.com/item.htm?id=698765432109&spm=a1z10\">去购买</a>"));
        assertEquals("572209212242", TbkShortLinkResolver.extractItemIdFromHtml(
                "<script>location.href='https://a.m.taobao.com/i572209212242.htm'</script>"));
    }

    /**
     * JSON 字段兜底
     */
    @Test
    public void testExtractFromJsonField() {
        assertEquals("572209212242", TbkShortLinkResolver.extractItemIdFromHtml(
                "{\"itemId\":\"572209212242\",\"title\":\"测试\"}"));
    }

    /**
     * 无关页面不应误判
     */
    @Test
    public void testExtractFromIrrelevantHtml() {
        assertNull(TbkShortLinkResolver.extractItemIdFromHtml(
                "<script src=\"//g.alicdn.com/mtb/lib-callapp/1.4.8/combo.js\"></script>"));
        assertNull(TbkShortLinkResolver.extractItemIdFromHtml(""));
        assertNull(TbkShortLinkResolver.extractItemIdFromHtml(null));
    }
}

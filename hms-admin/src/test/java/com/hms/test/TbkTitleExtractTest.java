package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import com.hms.main.tbk.service.TbkConvertService;

/**
 * 分享文案标题提取测试
 *
 * 回归：标题里带「」时（如 香港「设计师」Oho Ou …），不能只截到「设计师」。
 *
 * add by CYQ 2026-11-04
 */
public class TbkTitleExtractTest {

    @Test
    public void testNestedBracketTitle() throws Exception {
        String content = "【淘宝】https://e.tb.cn/h.jgxtQo0lgcdfqiU?tk=IEKN60PP4Ia MF168 "
                + "「香港「设计师」Oho Ou 草莓猫系列首饰套装项链手链耳钉生日礼物」\n点击链接直接打开";
        assertEquals("香港「设计师」Oho Ou 草莓猫系列首饰套装项链手链耳钉生日礼物",
                extract(content));
    }

    @Test
    public void testSimpleTitle() throws Exception {
        assertEquals("祖玛珑维他命E唇蜜",
                extract("【淘宝】https://e.tb.cn/h.xxx 「祖玛珑维他命E唇蜜」 点击链接直接打开"));
    }

    @Test
    public void testPddTitle() throws Exception {
        assertEquals("【到手三箱】面包新语组合装牛乳厚切吐司",
                extract("【拼多多】【到手三箱】面包新语组合装牛乳厚切吐司 https://mobile.yangkeduo.com/goods.html?goods_id=1"));
    }

    @Test
    public void testNoTitle() throws Exception {
        assertNull(extract("https://item.taobao.com/item.htm?id=123456789"));
    }

    private String extract(String content) throws Exception {
        TbkConvertService service = new TbkConvertService();
        Method method = TbkConvertService.class.getDeclaredMethod("extractTitleFromContent", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, content);
    }
}

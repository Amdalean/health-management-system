package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import com.hms.main.tbk.config.TbkProperties;

/**
 * 推广位 pid 解析测试
 *
 * pid 实际是 mm_用户ID_网站ID_推广位ID 四段，之前按三段取导致把 site_id 当 adzone_id。
 *
 * add by CYQ 2026-11-03
 */
public class TbkPropertiesTest {

    @Test
    public void testResolveFourSegmentPid() {
        TbkProperties props = new TbkProperties();
        props.setPid("mm_7743884050_3443100481_116339950386");
        assertEquals("116339950386", props.resolveAdzoneId());
        assertEquals("3443100481", props.resolveSiteId());
    }

    @Test
    public void testResolveThreeSegmentPid() {
        TbkProperties props = new TbkProperties();
        props.setPid("mm_123456_789012");
        assertEquals("789012", props.resolveAdzoneId());
        assertEquals("123456", props.resolveSiteId());
    }

    @Test
    public void testExplicitIdWins() {
        TbkProperties props = new TbkProperties();
        props.setPid("mm_7743884050_3443100481_116339950386");
        props.getTaobao().setAdzoneId("111111");
        props.getTaobao().setSiteId("222222");
        assertEquals("111111", props.resolveAdzoneId());
        assertEquals("222222", props.resolveSiteId());
    }

    @Test
    public void testEmptyPid() {
        TbkProperties props = new TbkProperties();
        assertNull(props.resolveAdzoneId());
        assertNull(props.resolveSiteId());
    }
}

package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkLink;
import com.hms.main.tbk.domain.TbkOrder;
import com.hms.main.tbk.mapper.TbkLinkMapper;
import com.hms.main.tbk.service.TbkLinkService;

/**
 * 订单返现估算测试：未结算订单要用预估佣金，不能因为结算佣金是 0 就判成「已结算」
 *
 * add by CYQ 2026-11-10 修复返现状态误判
 */
public class TbkOrderAttributionTest {

    @Test
    public void testPaidOrderIsEstimatedNotSettled() {
        TbkOrder order = new TbkOrder();
        order.setPlatform("taobao");
        order.setPayTime(new Date());
        order.setStatus("12");
        order.setEstimatedCommission(new BigDecimal("0.33"));
        // 大淘客未结算订单 pub_share_fee 是 0.00，映射后应为 null
        order.setSettledCommission(null);

        TbkLinkService service = buildService();
        service.attribute(order);

        assertEquals("approx", order.getMatchType());
        assertEquals(Long.valueOf(101), order.getUserId());
        assertEquals("estimated", order.getCashbackStatus());
        assertEquals(new BigDecimal("0.33"), order.getCashbackAmount());
    }

    @Test
    public void testSettledOrderUsesSettledCommission() {
        TbkOrder order = new TbkOrder();
        order.setPlatform("taobao");
        order.setPayTime(new Date());
        order.setStatus("3");
        order.setEstimatedCommission(new BigDecimal("0.33"));
        order.setSettledCommission(new BigDecimal("1.20"));

        TbkLinkService service = buildService();
        service.attribute(order);

        assertEquals("settled", order.getCashbackStatus());
        assertEquals(new BigDecimal("1.20"), order.getCashbackAmount());
    }

    @Test
    public void testRefundTagClearsCashback() {
        TbkOrder order = new TbkOrder();
        order.setPlatform("taobao");
        order.setPayTime(new Date());
        order.setStatus("12");
        order.setRefundTag("1");
        order.setEstimatedCommission(new BigDecimal("0.33"));

        TbkLinkService service = buildService();
        service.attribute(order);

        assertEquals("invalid", order.getCashbackStatus());
        assertEquals(0, order.getCashbackAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testNoLinkMeansUnattributed() {
        TbkProperties properties = new TbkProperties();
        properties.setCashbackRate(new BigDecimal("100"));
        properties.setAttributionWindowDays(7);
        TbkLinkMapper mapper = mock(TbkLinkMapper.class);
        when(mapper.selectCandidates(any(TbkOrder.class), eq(7))).thenReturn(Collections.emptyList());
        TbkLinkService service = new TbkLinkService();
        ReflectionTestUtils.setField(service, "linkMapper", mapper);
        ReflectionTestUtils.setField(service, "properties", properties);

        TbkOrder order = new TbkOrder();
        order.setPlatform("taobao");
        order.setPayTime(new Date());
        order.setStatus("12");
        order.setEstimatedCommission(new BigDecimal("0.33"));
        service.attribute(order);

        assertEquals("none", order.getMatchType());
        assertEquals("unattributed", order.getCashbackStatus());
        assertNotNull(order.getCashbackAmount());
        // 未归因订单仍会算出金额（只是标记为未归因，不参与打款）
        assertEquals(0, order.getCashbackAmount().compareTo(new BigDecimal("0.33")));
        assertNull(order.getUserId());
    }

    @Test
    public void testPddOrderStatusIsNotRefundTag() {
        // 拼多多把 order_status 存进 refund_tag 列（1=已成团），不能当成淘宝的维权标记
        TbkOrder order = new TbkOrder();
        order.setPlatform("pdd");
        order.setPayTime(new Date());
        order.setStatus("1");
        order.setRefundTag("1");
        order.setEstimatedCommission(new BigDecimal("0.12"));

        TbkLinkService service = buildService();
        service.attribute(order);

        assertEquals("estimated", order.getCashbackStatus());
        assertEquals(0, order.getCashbackAmount().compareTo(new BigDecimal("0.12")));
    }

    private TbkLinkService buildService() {
        TbkProperties properties = new TbkProperties();
        properties.setCashbackRate(new BigDecimal("100"));
        properties.setAttributionWindowDays(7);

        TbkLink link = new TbkLink();
        link.setId(29L);
        link.setUserId(101L);
        link.setPlatform("taobao");

        TbkLinkMapper mapper = mock(TbkLinkMapper.class);
        when(mapper.selectCandidates(any(TbkOrder.class), eq(7)))
                .thenReturn(Collections.singletonList(link));

        TbkLinkService service = new TbkLinkService();
        ReflectionTestUtils.setField(service, "linkMapper", mapper);
        ReflectionTestUtils.setField(service, "properties", properties);
        return service;
    }
}

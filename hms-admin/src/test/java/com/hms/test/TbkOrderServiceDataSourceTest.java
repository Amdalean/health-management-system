package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.hms.main.tbk.client.DataokeOrderClient;
import com.hms.main.tbk.client.PddTbkClient;
import com.hms.main.tbk.client.TopApiException;
import com.hms.main.tbk.client.TopClient;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkOrder;
import com.hms.main.tbk.mapper.TbkOrderMapper;
import com.hms.main.tbk.service.TbkLinkService;
import com.hms.main.tbk.service.TbkOrderService;

/**
 * 订单同步数据源切换测试：默认走大淘客，配置 taobao 时走联盟官方接口
 *
 * add by CYQ 2026-11-10 淘宝订单数据源改为大淘客
 */
public class TbkOrderServiceDataSourceTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testSyncRecentUsesDataokeByUpdateTime() {
        TbkProperties properties = new TbkProperties();
        assertEquals("dataoke", properties.getOrderSyncTaobaoProvider());

        TbkOrderMapper mapper = mock(TbkOrderMapper.class);
        DataokeOrderClient dataoke = mock(DataokeOrderClient.class);
        when(mapper.upsertTbkOrder(any(TbkOrder.class))).thenReturn(1);
        TbkOrder paid = new TbkOrder();
        paid.setPlatform("taobao");
        paid.setOrderId("3317017297020047451");
        paid.setStatus("12");
        when(dataoke.orderList(any(Date.class), any(Date.class), eq(DataokeOrderClient.QUERY_BY_UPDATE),
                anyInt(), anyInt())).thenReturn(Collections.singletonList(paid));

        TbkOrderService service = buildService(properties, mapper, dataoke, mockTop());

        Map<String, Object> result = service.syncRecent();

        verify(dataoke).orderList(any(Date.class), any(Date.class), eq(DataokeOrderClient.QUERY_BY_UPDATE),
                anyInt(), anyInt());
        verify(mapper).upsertTbkOrder(paid);
        assertEquals("dataoke", result.get("taobaoSource"));
        Map<String, Object> taobao = (Map<String, Object>) result.get("taobao");
        assertEquals(1, taobao.get("saved"));
        assertNull(taobao.get("error"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSyncDaysUsesPayTime() {
        TbkProperties properties = new TbkProperties();
        TbkOrderMapper mapper = mock(TbkOrderMapper.class);
        DataokeOrderClient dataoke = mock(DataokeOrderClient.class);
        when(mapper.upsertTbkOrder(any(TbkOrder.class))).thenReturn(1);
        when(dataoke.orderList(any(Date.class), any(Date.class), eq(DataokeOrderClient.QUERY_BY_PAY),
                anyInt(), anyInt())).thenReturn(Collections.emptyList());

        TbkOrderService service = buildService(properties, mapper, dataoke, mockTop());
        service.syncDays(1);

        // 1 天按 3 小时切片 → 8 次查询，全部按付款时间
        verify(dataoke, atLeastOnce()).orderList(any(Date.class), any(Date.class),
                eq(DataokeOrderClient.QUERY_BY_PAY), anyInt(), anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOfficialProviderSwitch() {
        TbkProperties properties = new TbkProperties();
        properties.setOrderSyncTaobaoProvider("taobao");
        properties.getTaobao().setAppKey("appkey");
        properties.getTaobao().setAppSecret("secret");

        TbkOrderMapper mapper = mock(TbkOrderMapper.class);
        DataokeOrderClient dataoke = mock(DataokeOrderClient.class);
        TopClient top = mock(TopClient.class);
        when(top.execute(anyString(), anyMap(), any())).thenThrow(
                new TopApiException("11", "isv.permission-api-package-limit", "Insufficient isv permissions"));

        TbkOrderService service = buildService(properties, mapper, dataoke, top);
        Map<String, Object> result = service.syncRecent();

        verify(dataoke, never()).orderList(any(Date.class), any(Date.class), anyString(), anyInt(), anyInt());
        Map<String, Object> taobao = (Map<String, Object>) result.get("taobao");
        assertNotNull(taobao.get("error"));
        assertTrue(String.valueOf(taobao.get("error")).contains("权限未开通"));
    }

    private TopClient mockTop() {
        TopClient top = mock(TopClient.class);
        when(top.execute(anyString(), anyMap(), any())).thenReturn(null);
        return top;
    }

    private TbkOrderService buildService(TbkProperties properties, TbkOrderMapper mapper,
            DataokeOrderClient dataoke, TopClient top) {
        TbkOrderService service = new TbkOrderService();
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "orderMapper", mapper);
        ReflectionTestUtils.setField(service, "dataokeOrderClient", dataoke);
        ReflectionTestUtils.setField(service, "topClient", top);
        PddTbkClient pdd = mock(PddTbkClient.class);
        when(pdd.isConfigured()).thenReturn(false);
        ReflectionTestUtils.setField(service, "pddClient", pdd);
        ReflectionTestUtils.setField(service, "linkService", mock(TbkLinkService.class));
        return service;
    }
}

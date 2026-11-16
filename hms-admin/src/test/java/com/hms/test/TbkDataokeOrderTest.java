package com.hms.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.DateUtils;
import com.hms.main.tbk.client.DataokeOrderClient;
import com.hms.main.tbk.domain.TbkOrder;

/**
 * 大淘客订单查询通道：报文映射与状态码测试
 *
 * 报文取自 2026-11-10 实测（大淘客 get-order-details 接口返回的真实订单）。
 *
 * add by CYQ 2026-11-10 淘宝订单数据源改为大淘客
 */
public class TbkDataokeOrderTest {

    /** 主商品子订单（天猫，实付 73，预估佣金 0.33） */
    private static final String MAIN_ORDER = "{"
            + "\"adzone_id\":116339950386,"
            + "\"adzone_name\":\"爱分享\","
            + "\"alipay_total_price\":\"73.00\","
            + "\"click_time\":\"2026-11-10 20:41:21\","
            + "\"income_rate\":\"0.45\","
            + "\"item_id\":\"P0tnU6PAVN2YWSDwRHPt6-v695kqoDT8Wy8KgyZFd4\","
            + "\"item_img\":\"//img.alicdn.com/tfscom/i3/2335305051/O1CN01JYrRGn1nBPfyGFcHc_!!2335305051.jpg\","
            + "\"item_num\":1,"
            + "\"item_price\":\"499.00\","
            + "\"item_title\":\"卫仕化毛球片猫多维生素营养猫毛化毛膏宠物猫咪猫草片卫士化猫用\","
            + "\"modified_time\":\"2026-11-10 20:42:20\","
            + "\"order_type\":\"天猫\","
            + "\"pub_share_fee\":\"0.00\","
            + "\"pub_share_pre_fee\":\"0.33\","
            + "\"refund_tag\":0,"
            + "\"seller_nick\":\"八公叔叔宠物用品旗舰店\","
            + "\"seller_shop_title\":\"八公叔叔宠物用品旗舰店\","
            + "\"tb_paid_time\":\"2026-11-10 20:42:05\","
            + "\"tk_paid_time\":\"2026-11-10 20:42:20\","
            + "\"tk_status\":12,"
            + "\"tk_total_rate\":\"0.45\","
            + "\"trade_id\":\"3317017297020047451\","
            + "\"trade_parent_id\":\"3317017297020038269\""
            + "}";

    /** 赠品子订单（0 元、0 佣金） */
    private static final String GIFT_ORDER = "{"
            + "\"adzone_id\":116339950386,"
            + "\"alipay_total_price\":\"0.00\","
            + "\"income_rate\":\"1.35\","
            + "\"item_id\":\"gkt2U2v7wko5Xf7v7uatr-KqNbRvkdIn5Wbddppsn\","
            + "\"item_title\":\"【赠品】化毛球片10片（退货需退回）卫仕营养品专享1\","
            + "\"pub_share_fee\":\"0.00\","
            + "\"pub_share_pre_fee\":\"0.00\","
            + "\"refund_tag\":0,"
            + "\"seller_shop_title\":\"八公叔叔宠物用品旗舰店\","
            + "\"tb_paid_time\":\"2026-11-10 20:42:05\","
            + "\"tk_paid_time\":\"2026-11-10 20:42:20\","
            + "\"tk_status\":12,"
            + "\"trade_id\":\"3317017297020065097\""
            + "}";

    @Test
    public void testMapPaidOrder() {
        TbkOrder order = DataokeOrderClient.toOrder(JSON.parseObject(MAIN_ORDER));

        assertNotNull(order);
        assertEquals("taobao", order.getPlatform());
        assertEquals("3317017297020047451", order.getOrderId());
        assertEquals("P0tnU6PAVN2YWSDwRHPt6-v695kqoDT8Wy8KgyZFd4", order.getItemId());
        assertEquals("卫仕化毛球片猫多维生素营养猫毛化毛膏宠物猫咪猫草片卫士化猫用", order.getItemTitle());
        assertEquals("八公叔叔宠物用品旗舰店", order.getShopName());
        assertEquals("116339950386", order.getPid());
        assertEquals("12", order.getStatus());
        assertEquals("已付款", order.getStatusDesc());
        assertEquals(new BigDecimal("73.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("73.00"), order.getPayAmount());
        assertEquals(new BigDecimal("0.33"), order.getEstimatedCommission());
        // 未结算订单 pub_share_fee=0.00，映射后结算佣金必须为空，否则会顶掉预估佣金
        assertNull(order.getSettledCommission());
        assertEquals(new BigDecimal("0.45"), order.getCommissionRate());
        assertEquals("0", order.getRefundTag());
        assertNotNull(order.getPayTime());
        assertEquals("2026-11-10 20:42:20", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", order.getPayTime()));
        // 未结算的订单没有结算时间
        assertNull(order.getSettleTime());
        assertNotNull(order.getRawData());
    }

    @Test
    public void testMapGiftOrder() {
        TbkOrder order = DataokeOrderClient.toOrder(JSON.parseObject(GIFT_ORDER));

        assertNotNull(order);
        assertEquals("3317017297020065097", order.getOrderId());
        assertEquals(new BigDecimal("0.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("0.00"), order.getEstimatedCommission());
        assertEquals("已付款", order.getStatusDesc());
    }

    @Test
    public void testStatusDesc() {
        assertEquals("拍下未付款", DataokeOrderClient.statusDesc("11"));
        assertEquals("已付款", DataokeOrderClient.statusDesc("12"));
        assertEquals("已关闭/失效", DataokeOrderClient.statusDesc("13"));
        assertEquals("已确认收货", DataokeOrderClient.statusDesc("14"));
        assertEquals("已结算", DataokeOrderClient.statusDesc("3"));
        assertEquals("状态99", DataokeOrderClient.statusDesc("99"));
        assertNull(DataokeOrderClient.statusDesc(null));
    }

    @Test
    public void testSettledOrderKeepsSettledCommission() {
        JSONObject node = JSON.parseObject("{\"trade_id\":\"2\",\"tk_status\":3,"
                + "\"pub_share_pre_fee\":\"0.33\",\"pub_share_fee\":\"1.20\"}");
        TbkOrder order = DataokeOrderClient.toOrder(node);
        assertNotNull(order);
        assertEquals("已结算", order.getStatusDesc());
        assertEquals(new BigDecimal("1.20"), order.getSettledCommission());
    }

    @Test
    public void testUnsettledZeroFeeBecomesNull() {
        // 状态 12（已付款）即使带结算佣金字段也应按未结算处理
        JSONObject node = JSON.parseObject("{\"trade_id\":\"3\",\"tk_status\":12,"
                + "\"pub_share_pre_fee\":\"0.33\",\"pub_share_fee\":\"0.00\"}");
        assertNull(DataokeOrderClient.toOrder(node).getSettledCommission());
    }

    @Test
    public void testNullNode() {
        assertNull(DataokeOrderClient.toOrder(null));
    }

    @Test
    public void testUnknownFieldsFallback() {
        // 兼容数字型字段（adzone_id/tk_status 在报文里是数字）与缺失字段
        JSONObject node = JSON.parseObject("{\"trade_id\":\"1\",\"adzone_id\":123,\"tk_status\":14}");
        TbkOrder order = DataokeOrderClient.toOrder(node);
        assertNotNull(order);
        assertEquals("123", order.getPid());
        assertEquals("已确认收货", order.getStatusDesc());
        assertNull(order.getTotalAmount());
    }
}

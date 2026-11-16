package com.hms.test;

import com.hms.common.core.domain.entity.SysRole;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.main.stock.domain.StockBacktestTradeVO;
import com.hms.main.stock.service.IStockAdvisorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组合级回测闭环测试 —— 校验 Python 写入的逐笔流水与组合结果经 Java 读端透出后自洽。
 * <p>
 * 重点是那条**从库内流水独立反推的持仓链**：它能同时抓住 run_id 过滤漏、strategy_type
 * 混档、ORDER BY 破坏时序、JSON 列解析失败这四类问题，而这些光看前端数值看不出来。
 * <p>
 * 模拟 admin 身份（userId=1）。库里无组合回测记录时按假设跳过（不视为失败），
 * 因为该表只在执行过【回测LIVE】后才有数据。
 * add by CYQ 2026-09-06
 */
@SpringBootTest(properties = {"arthas.enabled=false", "arthas.telnet-port=0", "arthas.http-port=0"})
@DisplayName("组合级回测闭环测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockPortfolioBacktestTest {

    private static final Logger log = LoggerFactory.getLogger(StockPortfolioBacktestTest.class);

    @Autowired
    private IStockAdvisorService stockAdvisorService;

    @BeforeEach
    void mockAdmin() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        SysRole role = new SysRole();
        role.setRoleKey("admin");
        user.setRoles(Collections.singletonList(role));

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUser(user);
        loginUser.setPermissions(Collections.singleton("*:*:*"));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null,
                        Collections.singletonList(new SimpleGrantedAuthority("admin")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("组合结果与逐笔流水自洽（持仓链/净值/费用/会计恒等式）")
    void testPortfolioClosure() {
        Map<String, Object> pf = stockAdvisorService.getPortfolioBacktest(null);
        Assumptions.assumeTrue(Boolean.TRUE.equals(pf.get("exists")),
                "库内暂无组合回测结果，跳过闭环断言（请先在【系统监控-定时任务】执行 回测LIVE）");

        double initial = dv(pf.get("initialCapital"));
        double finalValue = dv(pf.get("finalValue"));
        double finalCash = dv(pf.get("finalCash"));
        double finalMv = dv(pf.get("finalMarketValue"));

        // 1) 期末三项恒等：总资产 = 现金 + 市值
        assertClose(finalValue, finalCash + finalMv, 0.01, "期末总资产 ≠ 现金 + 市值");

        // 2) 净值曲线长度 = 交易日数，且末点 = 期末总资产
        List<?> curve = (List<?>) pf.get("navCurve");
        assertNotNull(curve, "navCurve 为空，equity_curve_json 解析失败？");
        assertClose(curve.size(), dv(pf.get("tradingDays")), 0.001, "净值点数 ≠ tradingDays");
        Map<?, ?> last = (Map<?, ?>) curve.get(curve.size() - 1);
        assertClose(dv(last.get("nav")), finalValue, 0.01, "净值末点 ≠ finalValue");
        assertClose(dv(last.get("cash")), finalCash, 0.01, "净值末点现金 ≠ finalCash");

        // 3) 峰值并发持仓数与曲线一致
        double maxPos = 0;
        for (Object o : curve) {
            maxPos = Math.max(maxPos, dv(((Map<?, ?>) o).get("pos")));
        }
        assertClose(maxPos, dv(pf.get("maxConcurrentPositions")), 0.001, "峰值并发持仓数与净值曲线不符");

        // 4) 逐笔流水：条数、口径、批次、时序，以及最关键的持仓链反推
        List<StockBacktestTradeVO> trades = stockAdvisorService.getPortfolioTrades(null);
        assertClose(trades.size(), dv(pf.get("tradeCount")), 0.001, "逐笔条数 ≠ tradeCount");
        assertNotNull(trades.get(0).getTradeDate(), "tradeDate 为空");

        long runId = lv(pf.get("runId"));
        Map<String, Integer> chain = new HashMap<>();
        double buyFee = 0, sellFee = 0, realized = 0;
        int buys = 0, sells = 0;
        java.util.Date prevDate = null;
        for (StockBacktestTradeVO t : trades) {
            assertClose(dv(t.getRunId()), runId, 0.001, "逐笔混入了其它批次: " + t);
            Assertions.assertEquals("PORTFOLIO", t.getStrategyType(),
                    "组合逐笔里混进了非 PORTFOLIO 口径: " + t);
            if (prevDate != null) {
                Assertions.assertFalse(t.getTradeDate().before(prevDate), "逐笔未按日期升序，持仓链无法反推");
            }
            prevDate = t.getTradeDate();

            int held = chain.getOrDefault(t.getSymbol(), 0);
            int next = "买入".equals(t.getAction()) ? held + t.getVolume() : held - t.getVolume();
            Assertions.assertTrue(next >= 0, "持仓出现负数（卖出超过持有）: " + t);
            Assertions.assertEquals(next, t.getPositionAfter().intValue(),
                    "position_after 与流水反推不符（说明撮合与落库不同源）: " + t);
            chain.put(t.getSymbol(), next);

            if ("买入".equals(t.getAction())) {
                buys++;
                Assertions.assertNull(t.getRealizedPnl(), "买入不应有已实现盈亏");
                buyFee += t.getFee();
            } else {
                sells++;
                Assertions.assertNotNull(t.getRealizedPnl(), "卖出缺少已实现盈亏");
                sellFee += t.getFee();
                realized += t.getRealizedPnl();
            }
        }
        assertClose(buys, dv(pf.get("buyCount")), 0.001, "买入笔数与流水不符");
        assertClose(sells, dv(pf.get("sellCount")), 0.001, "卖出笔数与流水不符");
        assertClose(buyFee + sellFee, dv(pf.get("buyFee")) + dv(pf.get("sellFee")), 0.05, "费用与流水不符");
        assertClose(realized, dv(pf.get("realizedPnl")), 0.05, "已实现盈亏与流水不符");

        // 5) 期末持仓表与持仓链末端一致（已平仓标的在持仓表里缺席，链上值为 0）
        List<?> positions = (List<?>) pf.get("positions");
        assertNotNull(positions, "positions 为空");
        Map<String, Integer> endShares = new HashMap<>();
        for (Object o : positions) {
            Map<?, ?> p = (Map<?, ?>) o;
            endShares.put(String.valueOf(p.get("symbol")), (int) lv(p.get("shares")));
        }
        int openInChain = 0;
        for (Map.Entry<String, Integer> e : chain.entrySet()) {
            int actual = endShares.getOrDefault(e.getKey(), 0);
            Assertions.assertEquals(e.getValue().intValue(), actual,
                    "标的 " + e.getKey() + " 期末股数与流水反推不符（流水=" + e.getValue() + "，持仓表=" + actual + "）");
            if (e.getValue() > 0) {
                openInChain++;
            }
        }
        Assertions.assertEquals(endShares.size(), openInChain,
                "持仓表笔数与链上未平仓标的数不符（链上有未平仓却不在持仓表，或多出）");
        for (Integer shares : endShares.values()) {
            Assertions.assertTrue(shares > 0, "持仓表里出现 0 股记录: " + shares);
        }

        // 6) 会计恒等式：期末 = 初始 + 已实现 - 买入费用 + 未实现
        //    avg_cost 落库保留 4 位小数，未实现项按持仓数放大误差，容差取 1 元（约 8bp@12万）
        double unrealized = 0;
        for (Object o : positions) {
            Map<?, ?> p = (Map<?, ?>) o;
            unrealized += dv(p.get("market_value")) - lv(p.get("shares")) * dv(p.get("avg_cost"));
        }
        assertClose(initial + realized - buyFee + unrealized, finalValue, 1.0,
                "资金不守恒：期末 ≠ 初始 + 已实现 - 买入费用 + 未实现");

        log.info("组合回测闭环通过：run_id={} 标的{}只 交易日{}天 成交{}笔 初始{} → 期末{}（{}{}%）",
                runId, dv(pf.get("symbolsCount")), curve.size(), trades.size(),
                initial, finalValue, finalValue >= initial ? "+" : "", dv(pf.get("totalReturnPct")));
    }

    @Test
    @DisplayName("批次列表可用且首项即最新一次")
    void testListPortfolioRuns() {
        List<Map<String, Object>> runs = stockAdvisorService.listPortfolioRuns();
        Map<String, Object> pf = stockAdvisorService.getPortfolioBacktest(null);
        Assumptions.assumeTrue(Boolean.TRUE.equals(pf.get("exists")), "库内暂无组合回测结果，跳过");
        Assertions.assertFalse(runs.isEmpty(), "exists 为真但批次列表为空");
        Assertions.assertEquals(lv(pf.get("runId")), lv(runs.get(0).get("runId")),
                "批次列表首项与详情不一致（都应按 run_id 倒序）");
    }

    // ---------------- 断言小工具（本模块内联，避免为三个测试引入新依赖） ----------------

    private static void assertNotNull(Object o, String msg) {
        Assertions.assertNotNull(o, msg);
    }

    private static void assertClose(double a, double b, double tol, String msg) {
        Assertions.assertTrue(Math.abs(a - b) <= tol, msg + "（" + a + " vs " + b + "，容差 " + tol + "）");
    }

    private static double dv(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0d;
    }

    private static long lv(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : -1L;
    }
}

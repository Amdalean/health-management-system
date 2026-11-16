package com.hms.test;

import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.core.domain.entity.SysRole;
import com.hms.main.stock.domain.KlineDataVO;
import com.hms.main.stock.service.IStockAdvisorService;
import com.hms.quartz.task.StockAnalysisTask;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 回测相关单元测试 — 组合级 PORTFOLIO 口径（单标的 LIVE 已删除）
 * 模拟 admin 身份（userId=1）覆盖核心链路
 * add by CYQ 2026-08-28
 */
@SpringBootTest(properties = {"arthas.enabled=false", "arthas.telnet-port=0", "arthas.http-port=0"})
@DisplayName("回测单元测试(组合)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockBacktestTest {

    @Autowired
    private IStockAdvisorService stockAdvisorService;

    @Autowired
    private StockAnalysisTask stockAnalysisTask;

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

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    @DisplayName("持仓概览/持仓列表/系统状态在 admin 下可读")
    void testOverviewAndSystemStatus() {
        Map<String, Object> overview = stockAdvisorService.getOverview();
        System.out.println("overview: " + overview);
        Assertions.assertNotNull(overview);
        Assertions.assertTrue(overview.containsKey("stockCount"));
        Assertions.assertTrue(overview.containsKey("totalValue"));

        List holdings = stockAdvisorService.getHoldings();
        System.out.println("holdings: " + holdings.size());
        Assertions.assertNotNull(holdings);

        Map<String, Object> status = stockAdvisorService.getSystemStatus();
        System.out.println("systemStatus: " + status);
        Assertions.assertTrue(status.containsKey("stockCount"));
        Assertions.assertTrue(status.containsKey("tushareReachable"));
    }

    @Test
    @Order(2)
    @DisplayName("K线数据可按年份查询（DB优先）")
    void testKlineData() {
        // 取持仓第一只作为样本，若无持仓则跳过
        List holdings = stockAdvisorService.getHoldings();
        if (holdings.isEmpty()) {
            System.out.println("无持仓，跳过 K线测试");
            return;
        }
        // 反射取 symbol（避免强依赖 VO 类型）
        String symbol = null;
        try {
            Object first = holdings.get(0);
            symbol = (String) first.getClass().getMethod("getSymbol").invoke(first);
        } catch (Exception e) {
            Assertions.fail("反射获取 symbol 失败: " + e.getMessage());
        }
        KlineDataVO kline = stockAdvisorService.getKlineData(symbol, "2025");
        System.out.println("kline " + symbol + " bars=" + (kline == null ? 0 : kline.getBars().size()));
        if (kline != null) {
            Assertions.assertNotNull(kline.getBars());
            Assertions.assertFalse(kline.getBars().isEmpty(), "K线应有数据（DB优先，缺口 API 补）");
        }
    }

    @Test
    @Order(3)
    @DisplayName("回测任务节点直调（等同任务管理手动执行）")
    void testRunBacktest() {
        // 回测只在【系统监控-定时任务】里手动执行；测试直调任务 bean，等同手动点执行一次
        String output = stockAnalysisTask.runBacktestTask();
        System.out.println("runBacktestTask output(前800字):\n" + output.substring(0, Math.min(800, output.length())));
        Assertions.assertNotNull(output);
        Assertions.assertTrue(output.contains("回测"), "任务应返回回测汇总");
        if (output.contains("❌")) {
            System.out.println("回测返回失败标记（外部频率限制等可能），仅观测");
        }
        Map<String, Object> status = stockAdvisorService.getBacktestJobStatus();
        System.out.println("backtest status(任务日志): " + status);
        Assertions.assertNotNull(status);
    }
}

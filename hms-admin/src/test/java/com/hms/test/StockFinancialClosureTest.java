package com.hms.test;

import com.hms.common.core.domain.entity.SysRole;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.main.stock.domain.StockFinancialVO;
import com.hms.main.stock.domain.StockRecommendationVO;
import com.hms.main.stock.service.IStockAdvisorService;
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
 * 财务闭环收敛测试 — sync-fin 接入 + evidence_class 透出 + 扫描只读
 * 模拟 admin 身份（userId=1）；外部依赖（Python/baostock/财务行）缺失时降级为观测不断言失败
 * add by CYQ 2026-09-06
 */
@SpringBootTest(properties = {"arthas.enabled=false", "arthas.telnet-port=0", "arthas.http-port=0"})
@DisplayName("财务闭环收敛测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockFinancialClosureTest {

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

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    @DisplayName("建议列表透出 evidenceClass（gate 标签）")
    void testRecommendationsExposeEvidenceClass() {
        List<StockRecommendationVO> list = stockAdvisorService.getRecommendations();
        Assertions.assertNotNull(list, "getRecommendations 不应抛异常（evidence_class 列需存在）");
        System.out.println("recommendations: " + list.size());
        for (StockRecommendationVO vo : list) {
            System.out.printf("  id=%d %s %s evidence=%s conf=%s%n",
                    vo.getId(), vo.getSymbol(), vo.getAction(), vo.getEvidenceClass(), vo.getConfidence());
            Assertions.assertNotNull(vo.getEvidenceClass(), "evidenceClass 应透出（历史行由列默认值回填）");
        }
        Map<String, Object> page = stockAdvisorService.getRecommendations(1, 20);
        Assertions.assertNotNull(page.get("rows"));
        @SuppressWarnings("unchecked")
        List<StockRecommendationVO> rows = (List<StockRecommendationVO>) page.get("rows");
        for (StockRecommendationVO vo : rows) {
            Assertions.assertNotNull(vo.getEvidenceClass(), "分页查询同样透出 evidenceClass");
        }
    }

    @Test
    @Order(2)
    @DisplayName("财务读端可查（PIT 近8期，无行则观测）")
    void testGetFinancials() {
        List<StockFinancialVO> list = stockAdvisorService.getFinancials("600588");
        Assertions.assertNotNull(list, "getFinancials 不应抛异常");
        System.out.println("financials(600588): " + list.size());
        for (StockFinancialVO vo : list) {
            System.out.printf("  %s Q%d eps=%.4f roe=%.2f lev=%.3f cfo/np=%.2f pub=%s%n",
                    vo.getStatDate(), vo.getQuarter(), vo.getEpsTtm(), vo.getRoeAvg(),
                    vo.getLiabilityToAsset(), vo.getCfoToNp(), vo.getPubDate());
            Assertions.assertNotNull(vo.getStatDate());
        }
        if (list.isEmpty()) {
            System.out.println("财务表暂无行（sync-fin 尚未在该环境跑过），仅观测");
        } else {
            Assertions.assertTrue(list.size() <= 8, "最多返回近8期");
        }
    }

    @Test
    @Order(3)
    @DisplayName("扫描结果只读可查（双写收敛后）")
    void testScanResultsReadable() {
        List<?> results = stockAdvisorService.getScanResults();
        Assertions.assertNotNull(results);
        System.out.println("scanResults: " + results.size());
    }

    @Test
    @Order(4)
    @DisplayName("回测任务状态结构可查")
    void testBacktestJobStatus() {
        Map<String, Object> status = stockAdvisorService.getBacktestJobStatus();
        Assertions.assertNotNull(status);
        Assertions.assertTrue(status.containsKey("running"), "状态应含 running");
        Assertions.assertTrue(status.containsKey("lastOk"), "状态应含 lastOk");
        System.out.println("backtestJobStatus: " + status);
    }

    @Test
    @Order(5)
    @DisplayName("触发财务同步（外部依赖缺失则观测）")
    void testRunSyncFinancial() {
        String output = stockAdvisorService.runSyncFinancial();
        Assertions.assertNotNull(output);
        System.out.println("runSyncFinancial output(前500字):\n" + output.substring(0, Math.min(500, output.length())));
        Assertions.assertFalse(output.contains("找不到 veighNa"), "veighNa 目录应可解析（../veighna-quant）");
        if (output.startsWith("❌")) {
            System.out.println("财务同步返回失败标记（缺解释器/baostock/网络均可能），仅观测不断言");
        } else {
            Assertions.assertTrue(output.contains("sync-fin") || output.contains("财务") || output.contains("quarters"),
                    "成功时应含 sync-fin 相关输出");
        }
    }
}

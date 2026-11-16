package com.hms.quartz.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.main.stock.service.IStockAdvisorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 股票每日持仓分析定时任务
 *
 * 每天收盘后运行一次，分析持仓并生成操作建议。
 * Python 子进程通过 __RESULT_JSON__ 标记输出结构化 JSON，Java 侧解析后生成精简 job_message。
 *
 * add by CYQ 2026-06-05
 */
@Component("StockAnalysisTask")
public class StockAnalysisTask {

    private static final Logger log = LoggerFactory.getLogger(StockAnalysisTask.class);

    @Autowired
    private IStockAdvisorService stockAdvisorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Quartz 系统任务使用 admin 身份，绕过 HTTP 登录上下文。add by CYQ 2026-08-18 */
    private static final Long SYSTEM_USER_ID = 1L;

    /**
     * 设置 Quartz 定时任务的 SecurityContext（admin 身份），使 currentUserId() 不再抛异常。
     * admin 的 userScope() 返回 "1=1"，可访问所有用户数据。
     * add by CYQ 2026-08-18 修复 t24 多用户改造后定时任务无登录上下文的问题
     */
    private void setupSecurityContext() {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(SYSTEM_USER_ID);
        sysUser.setUserName("admin");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(SYSTEM_USER_ID);
        loginUser.setUser(sysUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.info("Quartz SecurityContext 已设置: user_id={} (admin)", SYSTEM_USER_ID);
    }

    /**
     * 清除 Quartz 定时任务的 SecurityContext。
     * add by CYQ 2026-08-18
     */
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** 量化交易专用角色 key，按角色决定推送对象，不再按持仓。add by CYQ 2026-08-21 */
    private static final String QUANT_ROLE_KEY = "quant";

    /**
     * 查询需要接收量化交易消息的用户 ID 列表（按角色 quant）。
     * 替代旧逻辑“有持仓的用户”，实现按角色推送。
     * add by CYQ 2026-08-18 多用户定时任务逐用户分析
     * update by CYQ 2026-08-21 改为角色驱动（sys_role.role_key='quant'）
     */
    private List<Long> getActiveStockUserIds() {
        List<Long> userIds = jdbcTemplate.queryForList(
            "SELECT DISTINCT ur.user_id FROM sys_user_role ur " +
            "INNER JOIN sys_role r ON r.role_id = ur.role_id " +
            "INNER JOIN sys_user u ON u.user_id = ur.user_id " +
            "WHERE r.role_key = ? AND r.status = '0' AND r.del_flag = '0' " +
            "AND u.status = '0' AND u.del_flag = '0'",
            Long.class, QUANT_ROLE_KEY);
        log.info("量化交易用户(角色 {}): {}", QUANT_ROLE_KEY, userIds);
        return userIds;
    }

    /**
     * 从 Python 标准输出中提取 __RESULT_JSON__ ... __RESULT_JSON_END__ 之间的 JSON 对象。
     * 解析失败返回 null。
     * add by CYQ 2026-08-18
     */
    private JSONObject extractResultJson(String output) {
        if (output == null) return null;
        int start = output.indexOf("__RESULT_JSON__");
        int end = output.indexOf("__RESULT_JSON_END__");
        if (start < 0 || end < 0 || end <= start) return null;
        String jsonStr = output.substring(start + "__RESULT_JSON__".length(), end).trim();
        try {
            return JSONObject.parseObject(jsonStr);
        } catch (Exception e) {
            log.warn("解析 __RESULT_JSON__ 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 Python 返回的结构化 JSON 格式化为精简的 job_message 行。
     * 解析失败时降级为 "✅" 或 "❌" 标记。
     * add by CYQ 2026-08-18
     */
    private String formatResult(String label, String output) {
        JSONObject json = extractResultJson(output);
        if (json == null) {
            // 降级：无结构化数据时简单标记
            boolean hasError = output != null && (output.contains("Error") || output.contains("Exception"));
            return label + (hasError ? " ❌ 无结构化结果" : " ✅");
        }
        if ("error".equals(json.getString("status"))) {
            return label + " ❌ " + json.getString("error");
        }
        String command = json.getString("command");
        long elapsedMs = json.getLongValue("elapsed_ms");
        StringBuilder sb = new StringBuilder(label).append(" ✅ ").append(elapsedMs).append("ms");
        if ("scheduler".equals(command)) {
            int analyzed = json.getIntValue("analyzed");
            int recs = json.getJSONArray("recommendations") != null ? json.getJSONArray("recommendations").size() : 0;
            sb.append(" 分析").append(analyzed).append("只");
            if (recs > 0) sb.append(" 生成").append(recs).append("条建议");
        } else if ("scanner".equals(command)) {
            sb.append(" 候选").append(json.getIntValue("candidates")).append("只");
        } else if ("sync".equals(command)) {
            int rows = safeInt(json, "rows_inserted");
            int errors = safeInt(json, "errors");
            sb.append(" 写入").append(rows).append("行");
            if (errors > 0) sb.append(" ").append(errors).append("个错误");
        }
        return sb.toString();
    }

    /**
     * 安全读取 JSON 数值字段，兼容 JSONArray / null / 非数值类型。
     * add by CYQ 2026-08-19
     */
    private int safeInt(JSONObject json, String key) {
        Object val = json.get(key);
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof JSONArray) return ((JSONArray) val).size();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }

    /**
     * 执行股票分析。方法名需与 Quartz 配置中的 methodName 一致。
     * <p>
     * 流程: 持仓分析 → 信号通知 → 止损信号处理（自动过期+紧急通知）
     */
    public String execute() {
        setupSecurityContext();
        long t0 = System.currentTimeMillis();
        try {
            log.info("========== 股票持仓分析任务开始 ==========");
            StringBuilder summary = new StringBuilder();
            for (Long uid : getActiveStockUserIds()) {
                String r = stockAdvisorService.runAnalysisForUser(uid);
                log.info("[user {}] 分析详细输出:\n{}", uid, r);
                summary.append(formatResult("user " + uid, r)).append("\n");
            }
            String notifyReport = stockAdvisorService.sendSignalNotification("持仓分析");
            summary.append("通知: ").append(notifyReport).append("\n");
            stockAdvisorService.processPendingStopSignals();
            long elapsed = System.currentTimeMillis() - t0;
            String msg = "持仓分析 耗时" + elapsed + "ms\n" + summary;
            log.info("========== 股票持仓分析任务结束 ==========\n{}", msg);
            return msg;
        } catch (Exception e) {
            log.error("股票分析任务异常", e);
            return "持仓分析 ❌ " + e.getMessage();
        } finally {
            clearSecurityContext();
        }
    }

    /**
     * 执行全市场扫描。在 sys_job 中配置 invokeTarget = "StockAnalysisTask.scanMarket()"。
     */
    public String scanMarket() {
        setupSecurityContext();
        long t0 = System.currentTimeMillis();
        try {
            log.info("========== 全市场扫描任务开始 ==========");
            StringBuilder summary = new StringBuilder();
            // 多用户一次扫描：全市场分析只做一遍，Python 侧按用户分别分配/落库。
            // 改前是逐用户各跑一次完整扫描（日任务实测两次扫描各 ~103s，其中 ~90s 是重复劳动）。
            // add by CYQ 2026-10-14 跨用户扫描去重
            List<Long> userIds = getActiveStockUserIds();
            String r = stockAdvisorService.runScannerForUsers(userIds);
            log.info("[users {}] 扫描详细输出:\n{}", userIds, r);
            summary.append(formatResult("扫描 users=" + userIds, r)).append("\n");
            String notifyReport = stockAdvisorService.sendSignalNotification("全市场扫描");
            summary.append("通知: ").append(notifyReport).append("\n");
            long elapsed = System.currentTimeMillis() - t0;
            String msg = "全市场扫描 耗时" + elapsed + "ms\n" + summary;
            log.info("========== 全市场扫描任务结束 ==========\n{}", msg);
            return msg;
        } catch (Exception e) {
            log.error("全市场扫描任务异常", e);
            return "全市场扫描 ❌ " + e.getMessage();
        } finally {
            clearSecurityContext();
        }
    }

    /**
     * 【合并任务】完整股票分析流程：财务同步 → 行情同步 → 持仓分析 → 全市场扫描 → 通知 → 止损信号处理。
     * <p>
     * 替代原来的 execute() + scanMarket() 两个独立任务，
     * 由同一个定时任务统一管理所有逻辑，保证数据新鲜度。
     * <p>
     * 在 sys_job 中配置:
     *   invokeTarget = "StockAnalysisTask.fullAnalysis()"
     *   cron = "0 30 15 * * ?"（每天 15:30 收盘后执行）
     * <p>
     * add by CYQ 2026-07-13
     */
    public String fullAnalysis() {
        setupSecurityContext();
        try {
            return doFullAnalysis();
        } finally {
            clearSecurityContext();
        }
    }

    private String doFullAnalysis() {
        long t0 = System.currentTimeMillis();
        log.info("========== 完整股票分析流程开始 ==========");
        StringBuilder summary = new StringBuilder();
        boolean hasError = false;

        List<Long> userIds = getActiveStockUserIds();

        // 第零步：财务同步（Baostock 季频，增量，已有季度跳过；add by CYQ 2026-09-06）
        try {
            log.info(">>> step 0/5: 财务数据同步");
            String finResult = stockAdvisorService.runSyncFinancial();
            log.info("财务同步详细输出:\n{}", finResult);
            summary.append("【财务同步】✅\n");
        } catch (Exception e) {
            log.error("财务同步失败", e);
            summary.append("【财务同步】❌ ").append(e.getMessage()).append("\n");
            hasError = true;
        }

        // 第一步：行情同步
        try {
            log.info(">>> step 1/5: 行情数据同步");
            String syncResult = stockAdvisorService.runSyncMarketData();
            log.info("行情同步详细输出:\n{}", syncResult);
            summary.append(formatResult("【行情同步】", syncResult)).append("\n");
        } catch (Exception e) {
            log.error("行情同步失败", e);
            summary.append("【行情同步】❌ ").append(e.getMessage()).append("\n");
            hasError = true;
        }

        // 第二步：持仓分析（逐用户）
        for (Long uid : userIds) {
            try {
                log.info(">>> step 2/5: 持仓分析 user_id={}", uid);
                String r = stockAdvisorService.runAnalysisForUser(uid);
                log.info("[user {}] 分析详细输出:\n{}", uid, r);
                summary.append(formatResult("【分析 user=" + uid + "】", r)).append("\n");
            } catch (Exception e) {
                log.error("持仓分析失败 user_id={}", uid, e);
                summary.append("【分析 user=").append(uid).append("】❌ ").append(e.getMessage()).append("\n");
                hasError = true;
            }
        }

        // 第三步：全市场扫描（多用户一次分析，避免重复劳动）
        try {
            log.info(">>> step 3/5: 全市场扫描 users={}", userIds);
            String r = stockAdvisorService.runScannerForUsers(userIds);
            log.info("[users {}] 扫描详细输出:\n{}", userIds, r);
            summary.append(formatResult("【扫描 users=" + userIds + "】", r)).append("\n");
        } catch (Exception e) {
            log.error("全市场扫描失败 users={}", userIds, e);
            summary.append("【扫描 users=").append(userIds).append("】❌ ").append(e.getMessage()).append("\n");
            hasError = true;
        }

        // 第四步：止损信号处理
        try {
            log.info(">>> step 4/5: 止损信号处理");
            String stopReport = stockAdvisorService.processPendingStopSignals();
            log.info("止损信号处理详情:\n{}", stopReport);
            summary.append("【止损】").append(stopReport.isEmpty() ? "无待处理" : stopReport.trim()).append("\n");
        } catch (Exception e) {
            log.error("止损信号处理失败", e);
            summary.append("【止损】❌ ").append(e.getMessage()).append("\n");
        }

        // 第五步：通知
        try {
            log.info(">>> 发送统一通知");
            String notifyReport = stockAdvisorService.sendSignalNotification("完整分析报告");
            summary.append("【通知】").append(notifyReport).append("\n");
            log.info("通知结果: {}", notifyReport);
        } catch (Exception e) {
            log.error("发送通知失败", e);
            summary.append("【通知】❌ ").append(e.getMessage()).append("\n");
        }

        long elapsed = System.currentTimeMillis() - t0;
        String msg = "完整分析 耗时" + elapsed + "ms"
            + (hasError ? " ⚠️部分异常" : " ✅")
            + "\n" + summary;
        log.info("========== 完整股票分析流程结束 ==========\n{}", msg);
        return msg;
    }

    /**
     * 回测任务节点（sys_job 111，已暂停自动调度，仅手动触发，开发调试用）。
     * <p>
     * 在 sys_job 中配置（yearParam 可选，不填走 config 默认年；
     * capitalParam 可选，不填走 config.defaults.capital）:
     *   invokeTarget = "StockAnalysisTask.runBacktestTask()"
     *   invokeTarget = "StockAnalysisTask.runBacktestTask('2024')"
     *   invokeTarget = "StockAnalysisTask.runBacktestTask('2024-2026')"
     *   invokeTarget = "StockAnalysisTask.runBacktestTask('2024,2026')"
     *   invokeTarget = "StockAnalysisTask.runBacktestTask('2024-2026', 200000)"
     *   invokeTarget = "StockAnalysisTask.runBacktestTask('2024-2026', '200000')"
     * <p>
     * 组合口径产出：单一共享现金账户撮合
     * （stock_backtest_portfolio + trade strategy_type='PORTFOLIO'，
     * 指定期间时一次取全、单次连贯撮合、单个 run_id 落库）。
     * （单标的 LIVE 口径已删除，2026-09-10。）
     * <p>
     * add by CYQ 2026-09-06；传参加 by CYQ 2026-09-10；连贯区间改 by CYQ 2026-09-10
     * 初始资金参数 by CYQ 2026-09-24
     */
    public String runBacktestTask() {
        return doRunBacktest(null, null);
    }

    public String runBacktestTask(String yearParam) {
        return doRunBacktest(yearParam, null);
    }

    /** 资金以整数/长整/浮点/字符串形式传入（Quartz 按字面量类型解析）。 */
    public String runBacktestTask(String yearParam, Integer capitalParam) {
        return doRunBacktest(yearParam, toCapital(capitalParam));
    }

    public String runBacktestTask(String yearParam, Long capitalParam) {
        return doRunBacktest(yearParam, toCapital(capitalParam));
    }

    public String runBacktestTask(String yearParam, Double capitalParam) {
        return doRunBacktest(yearParam, capitalParam);
    }

    public String runBacktestTask(String yearParam, String capitalParam) {
        if (capitalParam == null || capitalParam.trim().isEmpty()) {
            return doRunBacktest(yearParam, null);
        }
        try {
            return doRunBacktest(yearParam, Double.valueOf(capitalParam.trim()));
        } catch (NumberFormatException e) {
            return "回测 ❌ 资金参数非法：" + capitalParam + "（示例：200000 或 '200000'）";
        }
    }

    private static Double toCapital(Number n) {
        return n == null ? null : n.doubleValue();
    }

    /**
     * A 股因子评估定时任务。
     * <p>
     * 在 sys_job 中配置: invokeTarget = "StockAnalysisTask.factorEval()"，
     * cron 建议 "0 0 2 * * ?"（每日 02:00，休市且 Tushare 闲时）。
     * 只写 stock_factor_* 研究表，不影响持仓/建议主链。
     * add by CYQ 2026-11-13（P2a）
     */
    public String factorEval() {
        setupSecurityContext();
        long t0 = System.currentTimeMillis();
        try {
            log.info("========== A股因子评估开始 ==========");
            String r = stockAdvisorService.runFactorEval();
            String msg = "因子评估 耗时" + (System.currentTimeMillis() - t0) + "ms\n" + r;
            log.info("========== A股因子评估结束 ==========\n{}", msg);
            return msg;
        } catch (Exception e) {
            log.error("因子评估任务异常", e);
            return "因子评估 ❌ " + e.getMessage();
        } finally {
            clearSecurityContext();
        }
    }

    /**
     * 回测执行主体：解析年份/校验资金 → 调 Python portfolio → 拼紧凑摘要。
     * @param capital 初始资金；null=走 config.defaults.capital。合法区间 1000~100000000。
     */
    private String doRunBacktest(String yearParam, Double capital) {
        List<Integer> years;
        try {
            years = parseBacktestYears(yearParam);
        } catch (IllegalArgumentException e) {
            return "回测 ❌ 年份参数非法：" + e.getMessage()
                + "（示例：'2024' / '2024-2026' / '2024,2026'，空=默认年）";
        }
        if (capital != null && (capital < 1000 || capital > 100000000)) {
            return "回测 ❌ 资金参数超出区间 1000~100000000：" + capital;
        }
        setupSecurityContext();
        long t0 = System.currentTimeMillis();
        try {
            log.info("========== 回测任务开始 ========== years={} capital={}",
                     years.isEmpty() ? "默认年" : years, capital == null ? "默认" : capital);
            StringBuilder summary = new StringBuilder();
            // 回测为全局口径（期初空仓的策略验证），与用户无关，只跑一次
            if (years.isEmpty()) {
                String pr = stockAdvisorService.runPortfolioBacktestForUser(null, null, null, capital);
                log.info("组合回测输出:\n{}", pr);
                summary.append("组合: ")
                    .append(pickLines(pr, "行情池", "期末总资产", "区间 ", "证据", "基准", "入库", "❌")).append("\n");
            } else {
                // 连贯区间：取起止一次跑完（单个 run_id）
                int start = java.util.Collections.min(years);
                int end = java.util.Collections.max(years);
                String pr = stockAdvisorService.runPortfolioBacktestForUser(null, start, end, capital);
                log.info("[{}-{}] 组合回测输出:\n{}", start, end, pr);
                summary.append("组合 ").append(start).append("-").append(end).append(": ")
                    .append(pickLines(pr, "行情池", "期末总资产", "区间 ", "证据", "基准", "入库", "❌")).append("\n");
            }
            long elapsed = System.currentTimeMillis() - t0;
            String msg = "回测 耗时" + elapsed + "ms\n" + summary;
            log.info("========== 回测任务结束 ==========\n{}", msg);
            return msg;
        } catch (Exception e) {
            log.error("回测任务异常", e);
            return "回测 ❌ " + e.getMessage();
        } finally {
            clearSecurityContext();
        }
    }

    /**
     * 解析回测年份参数：空=默认年（返回空列表）；"2024"=单年；
     * "2024-2026"=闭区间；"2024,2026"=多选（逗号分隔，可与区间混用）。
     * 年份限 2000~2100，总数限 10 个，防误配跑飞。
     * add by CYQ 2026-09-10
     */
    static List<Integer> parseBacktestYears(String yearParam) {
        List<Integer> years = new java.util.ArrayList<>();
        if (yearParam == null || yearParam.trim().isEmpty()) {
            return years;
        }
        java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>();
        for (String token : yearParam.split(",")) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.contains("-")) {
                String[] se = token.split("-", -1);
                if (se.length != 2) {
                    throw new IllegalArgumentException("区间格式应为 起始年-结束年：" + token);
                }
                int start = parseYear(se[0].trim());
                int end = parseYear(se[1].trim());
                if (start > end) {
                    throw new IllegalArgumentException("起始年大于结束年：" + token);
                }
                for (int y = start; y <= end; y++) {
                    set.add(y);
                }
            } else {
                set.add(parseYear(token));
            }
        }
        if (set.isEmpty()) {
            throw new IllegalArgumentException("未解析出有效年份");
        }
        if (set.size() > 10) {
            throw new IllegalArgumentException("年份超过 10 个（" + set.size() + "），请分批执行");
        }
        years.addAll(set);
        java.util.Collections.sort(years);
        return years;
    }

    /** 4 位年份校验（2000~2100）。add by CYQ 2026-09-10 */
    private static int parseYear(String s) {
        if (!s.matches("\\d{4}")) {
            throw new IllegalArgumentException("年份应为4位数字：" + s);
        }
        int y = Integer.parseInt(s);
        if (y < 2000 || y > 2100) {
            throw new IllegalArgumentException("年份超出 2000~2100：" + s);
        }
        return y;
    }

    /**
     * 从 Python 输出里挑出含指定关键字的行，拼成紧凑摘要。
     * sys_job_log.job_message 超 2000 字符会被截断（AbstractQuartzJob），故不回传全文。
     * add by CYQ 2026-09-06
     */
    private String pickLines(String out, String... keys) {
        if (out == null || out.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : out.split("\\r?\\n")) {
            for (String k : keys) {
                if (line.contains(k)) {
                    sb.append(line.trim()).append(" | ");
                    break;
                }
            }
        }
        return sb.length() == 0 ? out.replaceAll("\\s+", " ").trim() : sb.toString();
    }

    /**
     * 同步日线行情数据到 MySQL。增量模式，只补充缺失日期。
     * <p>
     * 在 sys_job 中配置:
     *   invokeTarget = "StockAnalysisTask.syncMarketData()"
     *   cron = "0 30 15 * * 1-5"（每天收盘后）
     * <p>
     * 支持参数:
     *   --holdings     只同步持仓股票（默认）
     *   --top 500      按成交额取前 N 只
     *   --symbols XXX  指定股票列表
     * <p>
     * add by CYQ 2026-06-25
     */
    public String syncMarketData() {
        long t0 = System.currentTimeMillis();
        log.info("========== 行情数据同步任务开始 ==========");
        try {
            String result = stockAdvisorService.runSyncMarketData();
            log.info("行情同步详细输出:\n{}", result);
            long elapsed = System.currentTimeMillis() - t0;
            String msg = formatResult("行情同步", result) + " 耗时" + elapsed + "ms";
            log.info("========== 行情数据同步任务结束 ==========\n{}", msg);
            return msg;
        } catch (Exception e) {
            log.error("行情同步任务异常", e);
            return "行情同步 ❌ " + e.getMessage();
        }
    }
}

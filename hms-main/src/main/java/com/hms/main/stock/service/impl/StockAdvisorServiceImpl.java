package com.hms.main.stock.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.main.stock.domain.KlineDataVO;
import com.hms.main.stock.domain.KlineDataVO.KlineBar;
import com.hms.main.stock.domain.KlineDataVO.SignalMarker;
import com.hms.main.stock.domain.PerformancePointVO;
import com.hms.main.stock.domain.StockBacktestTradeVO;
import com.hms.main.stock.domain.StockFinancialVO;
import com.hms.main.stock.domain.StockHoldingVO;
import com.hms.main.stock.domain.StockRecommendationVO;
import com.hms.main.stock.domain.StockScanResultVO;
import com.hms.main.stock.domain.StockTradeLogVO;
import com.hms.main.stock.service.IStockAdvisorService;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.hms.common.config.MailConfig;
import com.hms.main.integration.email.EmailIntegrationStrategy;
import com.hms.main.integration.email.EmailSendRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 股票顾问服务实现
 * add by CYQ 2026-06-05
 */
@Service
public class StockAdvisorServiceImpl implements IStockAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(StockAdvisorServiceImpl.class);

    /** 交易手续费率（万2.5）add by CYQ 2026-08-13 */
    private static final BigDecimal FEE_RATE = new BigDecimal("0.00025");

    /** 百分比换算常量 add by CYQ 2026-08-13 */
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * 现金流水 note 中的交易格式，例：600089 ADD 100股 / 000703 BUY 100股。
     * add by CYQ 2026-10-20 组合收益按交易流水重建
     */
    private static final Pattern TRADE_NOTE_PATTERN =
        Pattern.compile("^(\\d{5,6})\\s+(BUY|SELL|ADD|REDUCE)\\s+(\\d+)股");

    /**
     * 操作建议可排序字段白名单（前端 prop → SQL 列），防止 ORDER BY 注入。
     * add by CYQ 2026-10-20 持仓顾问表格排序
     */
    private static final Map<String, String> RECOMMENDATION_SORT_COLUMNS = new HashMap<>();
    static {
        RECOMMENDATION_SORT_COLUMNS.put("id", "r.id");
        RECOMMENDATION_SORT_COLUMNS.put("createdTime", "r.created_time");
        RECOMMENDATION_SORT_COLUMNS.put("symbol", "r.symbol");
        RECOMMENDATION_SORT_COLUMNS.put("name", "name");
        RECOMMENDATION_SORT_COLUMNS.put("ownerName", "owner_name");
        RECOMMENDATION_SORT_COLUMNS.put("action", "r.action");
        RECOMMENDATION_SORT_COLUMNS.put("priceHint", "r.price_hint");
        RECOMMENDATION_SORT_COLUMNS.put("suggestedShares", "r.suggested_shares");
        RECOMMENDATION_SORT_COLUMNS.put("suggestedAmount", "r.suggested_amount");
        RECOMMENDATION_SORT_COLUMNS.put("actualPrice", "r.actual_price");
        RECOMMENDATION_SORT_COLUMNS.put("actualShares", "r.actual_shares");
        RECOMMENDATION_SORT_COLUMNS.put("reason", "r.reason");
        RECOMMENDATION_SORT_COLUMNS.put("confidence", "r.confidence");
        RECOMMENDATION_SORT_COLUMNS.put("status", "r.status");
    }

    /**
     * veighNa 目录绝对路径。从 application.yml veighNa.path 读取，
     * 留空则自动探测（目录名 veighna-quant / veighNa 两种都试）。
     * add by CYQ 2026-06-29
     */
    @Value("${veighNa.path:}")
    private String configuredVeighNaPath;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailIntegrationStrategy emailIntegrationStrategy;

    /**
     * 解析 veighNa 目录，返回绝对路径。优先使用配置的 veighNa.path，
     * 其次自动探测（相对层级 ./..../.. × 目录名 veighna-quant/veighNa）。
     * add by CYQ 2026-06-29
     */
    private File resolveVeighNaDir() {
        // 1. 优先使用配置路径
        if (configuredVeighNaPath != null && !configuredVeighNaPath.trim().isEmpty()) {
            File configuredDir = new File(configuredVeighNaPath.trim());
            if (configuredDir.exists() && configuredDir.isDirectory()) {
                log.info("veighNa 目录: {} (来自配置 veighNa.path)", configuredDir.getAbsolutePath());
                return configuredDir;
            }
            log.warn("veighNa.path 配置的路径 {} 不存在，回退到自动探测", configuredVeighNaPath);
        }

        // 2. 自动探测：目录名与相对层级的笛卡尔积。两个名字都要试——
        // 本地开发曾用 veighNa/，独立仓库实际名为 veighna-quant/
        String userDir = System.getProperty("user.dir");
        log.debug("System.getProperty(\"user.dir\") = {}", userDir);

        String[] dirNames = {"veighna-quant", "veighNa"};
        String[] relatives = {"", "../", "../../"};
        StringBuilder tried = new StringBuilder();
        for (String relative : relatives) {
            for (String dirName : dirNames) {
                File candidate = new File(userDir, relative + dirName);
                tried.append(candidate.getPath()).append(" -> ");
                if (candidate.exists() && candidate.isDirectory()) {
                    log.info("veighNa 目录: {}", candidate.getAbsolutePath());
                    return candidate;
                }
            }
        }

        log.error("veighNa 目录探测失败！尝试了: {}", tried);
        return null;
    }

    /**
     * 返回 veighNa 虚拟环境中的 Python 解释器绝对路径。
     * <p>
     * 按运行平台返回：
     *   - Windows: 优先 <dir>/.venv/Scripts/python.exe，其次 <dir>/venv/Scripts/python.exe
     *   - Linux/macOS: 优先 <dir>/venv/bin/python，其次 <dir>/.venv/bin/python
     * <p>
     * 调用方需保证 veighNaDir 已存在；若所有候选都不存在，返回 Windows/Linux 对应主候选
     * 交由 ProcessBuilder 启动时报错（Java 侧记录），避免静默失败。
     * add by integrator 2026-08-16 (task t8)
     */
    private String resolvePythonExe(File veighNaDir) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String base = windows ? "/Scripts/python.exe" : "/bin/python";
        String[] envDirs = windows
            ? new String[]{"/.venv", "/venv"}
            : new String[]{"/venv", "/.venv"};

        for (String env : envDirs) {
            String candidate = veighNaDir.getAbsolutePath() + env + base;
            if (new File(candidate).exists()) {
                log.info("Python 解释器: {} ({})", candidate, env.substring(1));
                return candidate;
            }
        }

        // 都不存在 → 返回主候选，由 ProcessBuilder 抛错提示（不在此静默）
        String primary = veighNaDir.getAbsolutePath() + envDirs[0] + base;
        log.warn("未见 Python 解释器，将尝试主候选: {}（若失败请确认虚拟环境已重建）", primary);
        return primary;
    }

    /**
     * 当前登录用户 user_id；未登录/会话失效抛异常（写操作 / 需归属校验时调用）。
     * add by integrator 2026-08-16 (task t24 多用户改造)
     */
    private Long currentUserId() {
        Long uid = SecurityUtils.getUserId();
        if (uid == null) {
            throw new ServiceException("未登录或会话失效，无法操作股票数据");
        }
        return uid;
    }

    /**
     * 当前用户是否 admin（读操作 admin 可看全部）。add by integrator 2026-08-16 (task t24)
     */
    private boolean isAdminUser() {
        Long uid = SecurityUtils.getUserId();
        return uid != null && SecurityUtils.isAdmin(uid);
    }

    /**
     * 用户数据表的读作用户域过滤器：
     *   admin → "1=1"（看全部）；普通用户 → "user_id = <uid>"（只看自己）。
     * add by integrator 2026-08-16 (task t24)
     */
    private String userScope() {
        // 单表查询用（无歧义）
        return userScope(null);
    }

    /**
     * 返回"归属当前登录用户(user_id)"的条件子句。
     * @param alias 表别名（多表 JOIN 时传入主表别名避免 user_id 歧义；单表可传 null/空）
     */
    private String userScope(String alias) {
        if (isAdminUser()) {
            return "1=1";
        }
        String col = alias == null || alias.isEmpty() ? "user_id" : alias + ".user_id";
        return col + " = " + currentUserId();
    }

    /**
     * 回测为全局口径（user_id=0，admin 级共享资产），所有用户看到同一份结果；admin 可见全部。
     * add by CYQ 2026-09-23 回测挂到 admin/全局
     */
    private String backtestScope() {
        return isAdminUser() ? "1=1" : "user_id = 0";
    }

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        // 汇总（多用户：admin看全部，普通看自己）add by integrator t24
        Map<String, Object> summary = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as stockCount, COALESCE(SUM(market_value),0) as totalValue, " +
            "COALESCE(SUM(profit_loss),0) as totalPl " +
            "FROM stock_holding WHERE " + userScope()
        );
        result.put("stockCount", summary.get("stockCount"));
        BigDecimal totalValue = toBigDecimal(summary.get("totalValue"));
        BigDecimal totalPl = toBigDecimal(summary.get("totalPl"));
        result.put("totalValue", totalValue);
        result.put("totalPl", totalPl);

        // 现金余额（各用户最新一笔）
        BigDecimal availableCash = BigDecimal.ZERO;
        try {
            Map<String, Object> cash = jdbcTemplate.queryForMap(
                "SELECT balance FROM account_cash WHERE " + userScope() +
                " ORDER BY id DESC LIMIT 1"
            );
            availableCash = toBigDecimal(cash.get("balance"));
        } catch (Exception ignored) {}
        result.put("availableCash", availableCash);

        // 总资产 = 持仓市值 + 现金
        BigDecimal totalAssets = totalValue.add(availableCash);
        result.put("totalAssets", totalAssets);
        // 权益占比（仓位）与上限：让前台能直接看出"为什么买不进"（如已达现金底线）
        // add by CYQ 2026-10-15 仓位显示
        double equityPct = totalAssets.signum() > 0
            ? totalValue.doubleValue() / totalAssets.doubleValue() * 100.0 : 0.0;
        result.put("equityPct", BigDecimal.valueOf(equityPct).setScale(1, RoundingMode.HALF_UP));
        Object maxEquityPct = null;
        try {
            Object pf = getAdvisorParams().get("portfolio");
            if (pf instanceof Map) {
                maxEquityPct = ((Map<?, ?>) pf).get("max_equity_pct");
            }
        } catch (Exception ignored) {}
        result.put("maxEquityPct", maxEquityPct);

        // 待处理建议数
        try {
            Map<String, Object> pending = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) as cnt FROM stock_recommendation WHERE status = 'pending' " +
                "AND action != 'HOLD' AND " + userScope()
            );
            result.put("pendingCount", pending.get("cnt"));
        } catch (Exception e) {
            result.put("pendingCount", 0);
        }

        // 最后分析时间 add by CYQ 2026-06-15
        try {
            Map<String, Object> lastAnalyze = jdbcTemplate.queryForMap(
                "SELECT MAX(created_time) as last_time FROM stock_recommendation WHERE " + userScope()
            );
            result.put("lastAnalyzeTime", lastAnalyze.get("last_time"));
        } catch (Exception e) {
            result.put("lastAnalyzeTime", null);
        }

        // 最后扫描时间 add by CYQ 2026-06-15
        try {
            Map<String, Object> lastScan = jdbcTemplate.queryForMap(
                "SELECT MAX(scan_time) as last_time FROM stock_scan_result WHERE " + userScope()
            );
            result.put("lastScanTime", lastScan.get("last_time"));
        } catch (Exception e) {
            result.put("lastScanTime", null);
        }

        return result;
    }

    /**
     * 手工调整现金（入金/出金）。
     * 约定：amount 正数=入金、负数=出金；出金后余额不得为负。
     * 落一条 account_cash 流水，scanner/scheduler 读取最新余额作为可用现金。
     * add by CYQ 2026-09-10 现金管理
     */
    @Override
    public Map<String, Object> adjustCash(BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("调整金额不能为 0");
        }
        Long ownerId = currentUserId();

        BigDecimal cash = BigDecimal.ZERO;
        try {
            Map<String, Object> cashRow = jdbcTemplate.queryForMap(
                "SELECT balance FROM account_cash WHERE user_id = ? ORDER BY id DESC LIMIT 1", ownerId);
            cash = toBigDecimal(cashRow.get("balance"));
        } catch (Exception ignored) {}

        BigDecimal newBalance = cash.add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("现金余额不足：当前 ¥"
                + cash.setScale(2, RoundingMode.HALF_UP) + "，本次出金 ¥"
                + amount.abs().setScale(2, RoundingMode.HALF_UP));
        }

        boolean deposit = amount.compareTo(BigDecimal.ZERO) > 0;
        String type = deposit ? "deposit" : "withdraw";
        String noteText = (note == null || note.trim().isEmpty())
            ? (deposit ? "手工入金" : "手工出金") : note.trim();
        jdbcTemplate.update(
            "INSERT INTO account_cash (user_id, balance, change_amount, change_type, note) VALUES (?, ?, ?, ?, ?)",
            ownerId, newBalance, amount, type, noteText);

        Map<String, Object> result = new HashMap<>();
        result.put("balance", newBalance);
        result.put("changeAmount", amount);
        result.put("changeType", type);
        return result;
    }

    /**
     * 持仓顾问生效参数（读 veighNa/config.json，与 Python 顾问同源）。
     * add by CYQ 2026-09-24 顾问参数
     */
    @Override
    public Map<String, Object> getAdvisorParams() {
        Map<String, Object> out = new LinkedHashMap<>();
        File dir = resolveVeighNaDir();
        if (dir == null) {
            out.put("exists", false);
            out.put("message", "找不到 veighNa 目录");
            return out;
        }
        File cfgFile = new File(dir, "config.json");
        if (!cfgFile.exists()) {
            out.put("exists", false);
            out.put("message", "config.json 不存在");
            return out;
        }
        try {
            String text = new String(Files.readAllBytes(cfgFile.toPath()), StandardCharsets.UTF_8);
            JSONObject cfg = JSON.parseObject(text);
            out.put("exists", true);
            for (String k : new String[]{"portfolio", "cost_model", "risk", "valuation",
                                         "sell_discipline", "evaluation", "benchmark"}) {
                Object v = cfg.get(k);
                if (v != null) {
                    out.put(k, v);
                }
            }
        } catch (Exception e) {
            log.warn("读取 config.json 失败: {}", e.getMessage());
            out.put("exists", false);
            out.put("message", "读取 config.json 失败：" + e.getMessage());
        }
        return out;
    }

    /** 现金流水（近 N 条，含入/出金与成交引起的变动）。add by CYQ 2026-09-10 */
    @Override
    public List<Map<String, Object>> getCashLog(int limit) {
        int n = limit <= 0 ? 50 : Math.min(limit, 200);
        return jdbcTemplate.queryForList(
            "SELECT id, balance, change_amount, change_type, note, create_time " +
            "FROM account_cash WHERE " + userScope() + " ORDER BY id DESC LIMIT " + n);
    }

    /**
     * 安全转为 BigDecimal，避免 double 浮点精度丢失
     * add by CYQ 2026-08-13
     */
    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(v.toString());
    }

    @Override
    public List<StockHoldingVO> getHoldings() {
        // 多用户：admin看全部，普通看自己；holding 与 strategy 均按 user_id 归属 add by integrator t24
        String scope = userScope("h");   // h = stock_holding 主表，避免与 stock_strategy s 的 user_id 歧义(CW bug)
        String sql = "SELECT h.symbol, h.name, h.shares, h.cost_price, h.current_price, " +
            "h.market_value, h.profit_loss, h.profit_loss_pct, " +
            "s.strategy_name as assigned_strategy, " +
            "JSON_UNQUOTE(JSON_EXTRACT(s.params_json, '$._regime')) as regime, " +
            "COALESCE(u.nick_name, u.user_name) as owner_name " +
            "FROM stock_holding h " +
            "LEFT JOIN stock_strategy s ON h.symbol = s.symbol AND s.user_id = h.user_id AND s.active = 1 " +
            "LEFT JOIN sys_user u ON u.user_id = h.user_id " +
            "WHERE " + scope + " " +
            "ORDER BY h.market_value DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            StockHoldingVO vo = new StockHoldingVO();
            vo.setSymbol(rs.getString("symbol"));
            vo.setName(rs.getString("name"));
            vo.setShares(rs.getInt("shares"));
            vo.setCostPrice(rs.getDouble("cost_price"));
            vo.setCurrentPrice(rs.getDouble("current_price"));
            vo.setMarketValue(rs.getDouble("market_value"));
            vo.setProfitLoss(rs.getDouble("profit_loss"));
            vo.setProfitLossPct(rs.getDouble("profit_loss_pct"));
            vo.setAssignedStrategy(rs.getString("assigned_strategy"));
            vo.setRegime(rs.getString("regime"));
            vo.setOwnerName(rs.getString("owner_name"));
            return vo;
        });
    }

    @Override
    public List<StockRecommendationVO> getRecommendations() {
        return queryRecommendations(
            "SELECT r.id, r.symbol, COALESCE(r.name, h.name) as name, r.strategy_name, r.action, r.price_hint, " +
            "r.suggested_shares, r.suggested_amount, r.actual_price, r.actual_shares, r.reason, r.confidence, r.status, r.evidence_class, r.created_time, " +
            "COALESCE(u.nick_name, u.user_name) as owner_name " +
            "FROM stock_recommendation r " +
            "LEFT JOIN stock_holding h ON r.symbol = h.symbol AND r.user_id = h.user_id " +
            "LEFT JOIN sys_user u ON u.user_id = r.user_id " +
            "WHERE " + userScope("r") + " " +
            "ORDER BY r.created_time DESC LIMIT 50"
        );
    }

    @Override
    public Map<String, Object> getRecommendations(int pageNum, int pageSize,
                                                  String orderByColumn, String isAsc) {
        int total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM stock_recommendation WHERE " + userScope(), Integer.class);
        String orderColumn = RECOMMENDATION_SORT_COLUMNS.getOrDefault(orderByColumn, "r.created_time");
        String direction = ("asc".equalsIgnoreCase(isAsc) || "ascending".equalsIgnoreCase(isAsc))
                ? "ASC" : "DESC";
        List<StockRecommendationVO> rows = queryRecommendations(
            "SELECT r.id, r.symbol, COALESCE(r.name, h.name) as name, r.strategy_name, r.action, r.price_hint, " +
            "r.suggested_shares, r.suggested_amount, r.actual_price, r.actual_shares, r.reason, r.confidence, r.status, r.evidence_class, r.created_time, " +
            "COALESCE(u.nick_name, u.user_name) as owner_name " +
            "FROM stock_recommendation r " +
            "LEFT JOIN stock_holding h ON r.symbol = h.symbol AND r.user_id = h.user_id " +
            "LEFT JOIN sys_user u ON u.user_id = r.user_id " +
            "WHERE " + userScope("r") + " " +
            "ORDER BY " + orderColumn + " " + direction + ", r.id DESC LIMIT ? OFFSET ?",
            pageSize, (pageNum - 1) * pageSize
        );
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    private List<StockRecommendationVO> queryRecommendations(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            StockRecommendationVO vo = new StockRecommendationVO();
            vo.setId(rs.getLong("id"));
            vo.setSymbol(rs.getString("symbol"));
            vo.setName(rs.getString("name"));
            vo.setStrategyName(rs.getString("strategy_name"));
            vo.setAction(rs.getString("action"));
            vo.setPriceHint(rs.getDouble("price_hint"));
            vo.setSuggestedShares(rs.getInt("suggested_shares"));
            vo.setSuggestedAmount(rs.getDouble("suggested_amount"));
            vo.setActualPrice(rs.getObject("actual_price") != null ? rs.getDouble("actual_price") : null);
            vo.setActualShares(rs.getObject("actual_shares") != null ? rs.getInt("actual_shares") : null);
            vo.setReason(rs.getString("reason"));
            vo.setConfidence(rs.getString("confidence"));
            vo.setStatus(rs.getString("status"));
            try { vo.setEvidenceClass(rs.getString("evidence_class")); } catch (Exception ignored) {}
            try { vo.setOwnerName(rs.getString("owner_name")); } catch (Exception ignored) {}
            vo.setCreatedTime(rs.getTimestamp("created_time"));
            return vo;
        }, args);
    }

    @Override
    public String runAnalysis() {
        return runAnalysisForUser(currentUserId());
    }

    @Override
    public String runAnalysisForUser(Long userId) {
        try {
            File veighNaDir = resolveVeighNaDir();
            if (veighNaDir == null) {
                return "❌ 找不到 veighNa 目录";
            }

            List<String> cmd = new ArrayList<>(Arrays.asList(
                resolvePythonExe(veighNaDir),
                "-X", "utf8",
                "main.py",
                "scheduler"
            ));
            if (userId != null) {
                cmd.addAll(Arrays.asList("--user-id", String.valueOf(userId)));
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            log.info("runAnalysisForUser 调度：main.py scheduler {}，veighNaDir={}",
                userId != null ? "--user-id " + userId : "(all users)", veighNaDir.getAbsolutePath());

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
            log.info("runAnalysisForUser 完成，veighNaDir={}", veighNaDir.getAbsolutePath());
            return output.toString();

        } catch (Exception e) {
            log.error("分析执行失败", e);
            return "❌ 执行失败: " + e.getMessage();
        }
    }

    @Override
    @Transactional
    public String runScanner() {
        return runScannerForUser(currentUserId());
    }

    @Override
    @Transactional
    public String runScannerForUser(Long userId) {
        try {
            File veighNaDir = resolveVeighNaDir();
            if (veighNaDir == null) {
                return "❌ 找不到 veighNa 目录";
            }

            List<String> cmd = new ArrayList<>(Arrays.asList(
                resolvePythonExe(veighNaDir),
                "-X", "utf8",
                "main.py",
                "scanner"
            ));
            if (userId != null) {
                cmd.addAll(Arrays.asList("--user-id", String.valueOf(userId)));
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            log.info("runScannerForUser 调度：main.py scanner {}，veighNaDir={}",
                userId != null ? "--user-id " + userId : "(all users)", veighNaDir.getAbsolutePath());

            // 双写收敛：Python run_scan 已直写 stock_scan_result/stock_recommendation，Java 只读返回
            // add by CYQ 2026-09-06
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
            log.info("扫描完成（结果由 Python 直写入库）");
            return output.toString();

        } catch (Exception e) {
            log.error("扫描失败", e);
            return "❌ 扫描失败: " + e.getMessage();
        }
    }

    @Override
    @Transactional
    public String runScannerForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return runScannerForUser(null);
        }
        if (userIds.size() == 1) {
            return runScannerForUser(userIds.get(0));
        }
        try {
            File veighNaDir = resolveVeighNaDir();
            if (veighNaDir == null) {
                return "❌ 找不到 veighNa 目录";
            }
            StringBuilder ids = new StringBuilder();
            for (Long uid : userIds) {
                if (ids.length() > 0) ids.append(',');
                ids.append(uid);
            }
            List<String> cmd = new ArrayList<>(Arrays.asList(
                resolvePythonExe(veighNaDir),
                "-X", "utf8",
                "main.py",
                "scanner",
                "--user-ids", ids.toString()
            ));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            log.info("runScannerForUsers 调度：main.py scanner --user-ids {}，veighNaDir={}",
                ids, veighNaDir.getAbsolutePath());

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
            log.info("多用户扫描完成（结果由 Python 直写入库）");
            return output.toString();
        } catch (Exception e) {
            log.error("多用户扫描失败", e);
            return "❌ 扫描失败: " + e.getMessage();
        }
    }

    /**
     * 执行一次因子评估（Quartz 调用；同步等待 Python，通常 5~15 分钟）。
     * 不放在 HTTP 接口里，避免占用 Tomcat 线程与数据库连接。
     * add by CYQ 2026-11-13（P2a）
     */
    @Override
    public String runFactorEval() {
        File veighNaDir = resolveVeighNaDir();
        if (veighNaDir == null) {
            return "❌ 找不到 veighNa 目录";
        }
        try {
            Integer running = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_factor_run " +
                "WHERE status = 'running' AND heartbeat > DATE_SUB(NOW(), INTERVAL 30 MINUTE)",
                Integer.class);
            if (running != null && running > 0) {
                return "因子评估已在运行中（心跳 30 分钟内），跳过本次触发";
            }
        } catch (Exception e) {
            log.warn("检查因子运行状态失败（继续触发）: {}", e.getMessage());
        }
        String runId = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000));
        List<String> cmd = new ArrayList<>(Arrays.asList(
            resolvePythonExe(veighNaDir), "-X", "utf8", "main.py", "factor-eval",
            "--run-id", runId));
        log.info("runFactorEval 调度：run_id={}，veighNaDir={}", runId, veighNaDir.getAbsolutePath());
        return execPython(veighNaDir, cmd, "factor-eval");
    }

    @Override
    @Transactional
    public void markExecuted(Long recommendationId, Double actualPrice, Integer actualShares) {
        // 多用户改造 t24：全程按 user_id 归属 + 越权防护。
        Long uid = currentUserId();
        // 1. 读取建议（含归属 user_id，用于越权校验与写作用户域）
        Map<String, Object> rec;
        try {
            rec = jdbcTemplate.queryForMap(
                "SELECT symbol, action, suggested_shares, suggested_amount, price_hint, name, strategy_name, " +
                "indicators_json, user_id " +
                "FROM stock_recommendation WHERE id = ?", recommendationId
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new ServiceException("建议不存在或已被处理");
        }
        String symbol = (String) rec.get("symbol");
        String action = (String) rec.get("action");
        String strategyName = (String) rec.get("strategy_name");
        Long recUserId = rec.get("user_id") != null
            ? ((Number) rec.get("user_id")).longValue() : null;

        // 越权校验：普通用户只能执行自己的建议；admin 可代执行但写入其归属用户域。
        if (!isAdminUser() && (recUserId == null || !recUserId.equals(uid))) {
            throw new ServiceException("无权执行他人的建议");
        }
        // recUserId 为 null（历史/无主）时回退到当前用户
        Long ownerId = (recUserId != null && recUserId > 0) ? recUserId : uid;

        // 实际成交价/股数：优先使用实际值，未提供时回退到建议值
        boolean useActual = actualPrice != null && actualPrice > 0
            && actualShares != null && actualShares > 0;
        BigDecimal price = useActual ? toBigDecimal(actualPrice) : toBigDecimal(rec.get("price_hint"));
        int shares = useActual ? actualShares
            : (rec.get("suggested_shares") != null ? ((Number) rec.get("suggested_shares")).intValue() : 0);
        BigDecimal amount = price.multiply(BigDecimal.valueOf(shares));
        if (!useActual && rec.get("suggested_amount") != null) {
            amount = toBigDecimal(rec.get("suggested_amount"));
        }

        if (shares <= 0 || "HOLD".equals(action)) {
            // 资金不足·待入金：建议股数为 0（不可直接执行），提示先入金等下一轮信号
            // add by CYQ 2026-10-29 资金拒买转待处理
            Object ind = rec.get("indicators_json");
            if (shares <= 0 && ind != null && ind.toString().contains("funding_block")) {
                throw new ServiceException("该建议为「资金不足·待入金」：建议股数为 0，"
                    + "请先入金或卖出其他持仓，下一轮扫描会生成可执行建议");
            }
            return;
        }

        // 2. 读取该用户的当前现金（最新一笔）
        BigDecimal cash = BigDecimal.ZERO;
        try {
            Map<String, Object> cashRow = jdbcTemplate.queryForMap(
                "SELECT balance FROM account_cash WHERE user_id = ? ORDER BY id DESC LIMIT 1", ownerId);
            cash = toBigDecimal(cashRow.get("balance"));
        } catch (Exception ignored) {}

        // 3. 读取该用户的持仓
        Map<String, Object> holding = null;
        try {
            holding = jdbcTemplate.queryForMap(
                "SELECT id, shares, cost_price, name FROM stock_holding WHERE user_id = ? AND symbol = ?",
                ownerId, symbol);
        } catch (Exception ignored) {}

        BigDecimal newCash = cash;
        String name = holding != null ? (String) holding.get("name")
            : (rec.get("name") != null ? (String) rec.get("name") : symbol);
        Long holdingId = holding != null ? ((Number) holding.get("id")).longValue() : null;

        BigDecimal fee = amount.multiply(FEE_RATE);
        if ("BUY".equals(action) || "ADD".equals(action)) {
            newCash = cash.subtract(amount).subtract(fee);
            if (holding != null) {
                int oldShares = ((Number) holding.get("shares")).intValue();
                BigDecimal oldCost = toBigDecimal(holding.get("cost_price"));
                int newShares = oldShares + shares;
                BigDecimal newCost = oldCost.multiply(BigDecimal.valueOf(oldShares))
                    .add(price.multiply(BigDecimal.valueOf(shares)))
                    .divide(BigDecimal.valueOf(newShares), 4, RoundingMode.HALF_UP);
                BigDecimal marketValue = price.multiply(BigDecimal.valueOf(newShares));
                BigDecimal profitLoss = price.subtract(newCost).multiply(BigDecimal.valueOf(newShares));
                BigDecimal profitLossPct = price.subtract(newCost)
                    .divide(newCost, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);
                jdbcTemplate.update(
                    "UPDATE stock_holding SET shares = ?, available_shares = ?, cost_price = ?, " +
                    "market_value = ?, profit_loss = ?, profit_loss_pct = ? WHERE user_id = ? AND symbol = ?",
                    newShares, newShares, newCost, marketValue, profitLoss, profitLossPct, ownerId, symbol);
            } else {
                // 新开仓：直接建仓（带 user_id）
                BigDecimal marketValue = price.multiply(BigDecimal.valueOf(shares));
                jdbcTemplate.update(
                    "INSERT INTO stock_holding (user_id, symbol, name, shares, available_shares, cost_price, " +
                    "current_price, market_value, profit_loss, profit_loss_pct, source) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'advisor')",
                    ownerId, symbol, name, shares, shares, price, price, marketValue
                );
                holdingId = jdbcTemplate.queryForObject(
                    "SELECT id FROM stock_holding WHERE user_id = ? AND symbol = ?", Long.class,
                    ownerId, symbol);
            }
            // 双均线趋势仓开始 ATR 尾随追踪；高水位随持仓跨任务持久化。
            if ("double_ma_trend".equals(strategyName) && holdingId != null) {
                jdbcTemplate.update(
                    "INSERT INTO stock_holding_atr_state (holding_id, user_id, symbol, active, peak_close) " +
                    "VALUES (?, ?, ?, 1, ?) ON DUPLICATE KEY UPDATE active = 1, " +
                    "peak_close = GREATEST(COALESCE(peak_close, 0), VALUES(peak_close))",
                    holdingId, ownerId, symbol, price);
            }
        } else if ("SELL".equals(action) || "REDUCE".equals(action)) {
            newCash = cash.add(amount).subtract(fee);
            if (holding != null) {
                int oldShares = ((Number) holding.get("shares")).intValue();
                int newShares = oldShares - shares;
                if (newShares <= 0) {
                    if (holdingId != null) {
                        jdbcTemplate.update("DELETE FROM stock_holding_atr_state WHERE holding_id = ?", holdingId);
                    }
                    jdbcTemplate.update("DELETE FROM stock_holding WHERE user_id = ? AND symbol = ?", ownerId, symbol);
                } else {
                    BigDecimal cost = toBigDecimal(holding.get("cost_price"));
                    BigDecimal marketValue = price.multiply(BigDecimal.valueOf(newShares));
                    BigDecimal profitLoss = price.subtract(cost).multiply(BigDecimal.valueOf(newShares));
                    BigDecimal profitLossPct = price.subtract(cost)
                        .divide(cost, 6, RoundingMode.HALF_UP)
                        .multiply(HUNDRED);
                    jdbcTemplate.update(
                        "UPDATE stock_holding SET shares = ?, available_shares = ?, " +
                        "market_value = ?, profit_loss = ?, profit_loss_pct = ? WHERE user_id = ? AND symbol = ?",
                        newShares, newShares, marketValue, profitLoss, profitLossPct, ownerId, symbol);
                }
            }
        }

        // 4. 更新现金（带 user_id 记账）
        jdbcTemplate.update(
            "INSERT INTO account_cash (user_id, balance, change_amount, change_type, note) VALUES (?, ?, ?, ?, ?)",
            ownerId, newCash, newCash.subtract(cash), action.toLowerCase(), symbol + " " + action + " " + shares + "股"
        );

        // 5. 记录交易（带 user_id）
        jdbcTemplate.update(
            "INSERT INTO stock_trade_log (user_id, symbol, name, action, price, shares, amount, fee, recommendation_id, trade_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
            ownerId, symbol, name, action, price, shares, amount, fee, recommendationId
        );

        // 6. 更新建议状态（限定归属 user_id，防越权）
        if (useActual) {
            jdbcTemplate.update(
                "UPDATE stock_recommendation SET status = 'executed', executed_time = NOW(), " +
                "actual_price = ?, actual_shares = ? WHERE id = ? AND user_id = ?",
                price, shares, recommendationId, ownerId
            );
        } else {
            jdbcTemplate.update(
                "UPDATE stock_recommendation SET status = 'executed', executed_time = NOW() WHERE id = ? AND user_id = ?",
                recommendationId, ownerId
            );
        }
    }

    @Override
    @Transactional
    public void markExpired(Long recommendationId) {
        // 多用户：限定本人建议才可过期（t24）
        int rows = jdbcTemplate.update(
            "UPDATE stock_recommendation SET status = 'expired' WHERE id = ? AND status = 'pending' AND user_id = ?",
            recommendationId, currentUserId()
        );
        if (rows == 0) {
            throw new ServiceException("建议不存在或无权限操作");
        }
    }

    @Override
    public List<StockScanResultVO> getScanResults() {
        // 多用户：只返回当前用户（admin 全部）的扫描结果（t24）
        return jdbcTemplate.query(
            "SELECT symbol, name, action, price, suggested_shares, suggested_amount, " +
            "strategy, regime, confidence, score, reason, pct_chg, amount " +
            "FROM stock_scan_result WHERE " + userScope() + " ORDER BY score DESC",
            (rs, rowNum) -> {
                StockScanResultVO vo = new StockScanResultVO();
                vo.setSymbol(rs.getString("symbol"));
                vo.setName(rs.getString("name"));
                vo.setAction(rs.getString("action"));
                vo.setPrice(rs.getDouble("price"));
                vo.setSuggestedShares(rs.getInt("suggested_shares"));
                vo.setSuggestedAmount(rs.getDouble("suggested_amount"));
                vo.setStrategy(rs.getString("strategy"));
                vo.setRegime(rs.getString("regime"));
                vo.setConfidence(rs.getString("confidence"));
                vo.setScore(rs.getInt("score"));
                vo.setReason(rs.getString("reason"));
                vo.setPctChg(rs.getDouble("pct_chg"));
                vo.setAmount(rs.getDouble("amount"));
                return vo;
            }
        );
    }

    @Override
    public String runSyncMarketData() {
        try {
            File veighNaDir = resolveVeighNaDir();
            if (veighNaDir == null) {
                return "❌ 找不到 veighNa 目录";
            }

            ProcessBuilder pb = new ProcessBuilder(
                resolvePythonExe(veighNaDir),
                "-X", "utf8",
                "main.py",
                "sync",
                "--holdings-first"
            );
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
            return output.toString();

        } catch (Exception e) {
            return "❌ 行情同步失败: " + e.getMessage();
        }
    }

    @Override
    public String runSyncFinancial() {
        try {
            File veighNaDir = resolveVeighNaDir();
            if (veighNaDir == null) {
                return "❌ 找不到 veighNa 目录";
            }

            ProcessBuilder pb = new ProcessBuilder(
                resolvePythonExe(veighNaDir),
                "-X", "utf8",
                "main.py",
                "sync-fin",
                "--holdings"
            );
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
            return output.toString();

        } catch (Exception e) {
            return "❌ 财务同步失败: " + e.getMessage();
        }
    }

    @Override
    public List<StockFinancialVO> getFinancials(String symbol) {
        // PIT 口径展示：只取已披露（pub_date<=今日），按报告期倒序近8期
        // add by CYQ 2026-09-06 财务层闭环
        String sql = "SELECT symbol, stat_date, pub_date, year, quarter, roe_avg, net_profit_wan, " +
            "eps_ttm, revenue_wan, liability_to_asset, ebit_to_interest, cfo_to_np, yoy_ni " +
            "FROM stock_financial_quarter WHERE symbol = ? AND pub_date <= CURDATE() " +
            "ORDER BY stat_date DESC LIMIT 8";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            StockFinancialVO vo = new StockFinancialVO();
            vo.setSymbol(rs.getString("symbol"));
            vo.setStatDate(rs.getDate("stat_date"));
            vo.setPubDate(rs.getDate("pub_date"));
            vo.setYear(rs.getInt("year"));
            vo.setQuarter(rs.getInt("quarter"));
            vo.setRoeAvg(rs.getObject("roe_avg") != null ? rs.getDouble("roe_avg") : null);
            vo.setNetProfitWan(rs.getObject("net_profit_wan") != null ? rs.getDouble("net_profit_wan") : null);
            vo.setEpsTtm(rs.getObject("eps_ttm") != null ? rs.getDouble("eps_ttm") : null);
            vo.setRevenueWan(rs.getObject("revenue_wan") != null ? rs.getDouble("revenue_wan") : null);
            vo.setLiabilityToAsset(rs.getObject("liability_to_asset") != null ? rs.getDouble("liability_to_asset") : null);
            vo.setEbitToInterest(rs.getObject("ebit_to_interest") != null ? rs.getDouble("ebit_to_interest") : null);
            vo.setCfoToNp(rs.getObject("cfo_to_np") != null ? rs.getDouble("cfo_to_np") : null);
            vo.setYoyNi(rs.getObject("yoy_ni") != null ? rs.getDouble("yoy_ni") : null);
            return vo;
        }, symbol);
    }

    @Override
    public List<StockTradeLogVO> getTradeLog() {
        // 多用户：只返回当前用户（admin 全部）的交易记录（t24）
        String sql = "SELECT id, symbol, name, action, price, shares, amount, fee, recommendation_id, note, trade_time " +
            "FROM stock_trade_log WHERE " + userScope() + " ORDER BY trade_time DESC LIMIT 100";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            StockTradeLogVO vo = new StockTradeLogVO();
            vo.setId(rs.getLong("id"));
            vo.setSymbol(rs.getString("symbol"));
            vo.setName(rs.getString("name"));
            vo.setAction(rs.getString("action"));
            vo.setPrice(rs.getDouble("price"));
            vo.setShares(rs.getInt("shares"));
            vo.setAmount(rs.getDouble("amount"));
            vo.setFee(rs.getDouble("fee"));
            vo.setRecommendationId(rs.getLong("recommendation_id"));
            vo.setNote(rs.getString("note"));
            vo.setTradeTime(rs.getTimestamp("trade_time"));
            return vo;
        });
    }

    /**
     * 组合权益曲线：按真实交易流水重建每日持仓与现金，时间加权（TWR）计算累计收益率。
     *
     * 与原实现的区别（add by CYQ 2026-10-20）：
     * 1) 不再用“当前持仓 × 历史收盘价”，而是从当前持仓倒推每日持仓，买卖以现金流水 note 为准
     *    （note 与现金变动同源，例：000001 BUY 100股），trade_log 仅补充 note 缺失的交易；
     * 2) 现金按 account_cash 每日余额还原，不再整段使用当前现金；
     * 3) 累计收益率采用时间加权：r_t = (NAV_t - NAV_{t-1} - 当日净流入) / NAV_{t-1}，
     *    入金/出金不计入收益，避免“入金推高收益率”；
     * 4) 起点为账户第一笔资金/交易/持仓日期，不再回溯到最早行情日。
     */
    @Override
    public List<PerformancePointVO> getPortfolioPerformance() {
        // 1. 现金流水（含买卖；note 中带标的/方向/股数，与现金变动同源）
        List<Map<String, Object>> cashRows = jdbcTemplate.queryForList(
            "SELECT user_id, balance, change_amount, change_type, note, create_time " +
            "FROM account_cash WHERE " + userScope() + " ORDER BY user_id, id"
        );

        // 2. 当前持仓（作为持仓重建的终态）
        List<Map<String, Object>> holdingRows = jdbcTemplate.queryForList(
            "SELECT user_id, symbol, shares, cost_price, create_time " +
            "FROM stock_holding WHERE shares > 0 AND " + userScope()
        );
        if (holdingRows.isEmpty()) return new ArrayList<>();

        // 3. 结构化交易流水（用于补充现金 note 未覆盖的交易）
        List<Map<String, Object>> tradeRows = jdbcTemplate.queryForList(
            "SELECT user_id, symbol, action, shares, trade_time " +
            "FROM stock_trade_log WHERE " + userScope() + " ORDER BY user_id, trade_time, id"
        );

        // 4. 按用户归集账本（admin 查看全部时也能按用户分别重建后再汇总）
        Map<Long, PortfolioBook> books = new LinkedHashMap<>();
        for (Map<String, Object> row : cashRows) {
            PortfolioBook book = books.computeIfAbsent(toLong(row.get("user_id")), PortfolioBook::new);
            book.cash.add(new CashEvent(
                toLocalDateTime(row.get("create_time")),
                toDouble(row.get("balance")),
                toDouble(row.get("change_amount")),
                str(row.get("change_type")),
                str(row.get("note"))));
        }
        for (Map<String, Object> row : holdingRows) {
            PortfolioBook book = books.computeIfAbsent(toLong(row.get("user_id")), PortfolioBook::new);
            String symbol = str(row.get("symbol"));
            book.currentShares.put(symbol, ((Number) row.get("shares")).intValue());
            book.cost.put(symbol, toDouble(row.get("cost_price")));
            LocalDate createDate = toLocalDate(row.get("create_time"));
            if (createDate != null) book.createDates.put(symbol, createDate);
        }
        for (Map<String, Object> row : tradeRows) {
            PortfolioBook book = books.computeIfAbsent(toLong(row.get("user_id")), PortfolioBook::new);
            book.rawTrades.add(new TradeLogRow(
                toLocalDateTime(row.get("trade_time")),
                str(row.get("symbol")),
                str(row.get("action")),
                ((Number) row.get("shares")).intValue()));
        }
        books.values().removeIf(b -> b.currentShares.isEmpty() && b.rawTrades.isEmpty() && b.cash.isEmpty());
        if (books.isEmpty()) return new ArrayList<>();

        // 5. 以现金流水 note 为主重建交易；trade_log 中同一笔（同标的/方向/时间相近）不重复计入
        for (PortfolioBook book : books.values()) {
            Set<CashEvent> noteTradeEvents = new HashSet<>();
            for (CashEvent c : book.cash) {
                if (c.time == null || !isTradeType(c.type)) continue;
                Matcher m = TRADE_NOTE_PATTERN.matcher(c.note == null ? "" : c.note.trim());
                if (!m.find()) continue;
                noteTradeEvents.add(c);
                book.trades.add(new TradeEvent(c.time, m.group(1), signedShares(m.group(2), Integer.parseInt(m.group(3)))));
            }
            for (TradeLogRow row : book.rawTrades) {
                if (row.time == null) continue;
                boolean duplicated = false;
                for (CashEvent c : noteTradeEvents) {
                    if (c.time == null) continue;
                    if (!c.time.toLocalDate().equals(row.time.toLocalDate())) continue;
                    if (Math.abs(Duration.between(c.time, row.time).getSeconds()) > 120) continue;
                    Matcher m = TRADE_NOTE_PATTERN.matcher(c.note == null ? "" : c.note.trim());
                    if (m.find() && m.group(1).equals(row.symbol)
                        && m.group(2).equalsIgnoreCase(row.action)) {
                        duplicated = true;
                        break;
                    }
                }
                if (!duplicated) {
                    book.trades.add(new TradeEvent(row.time, row.symbol, signedShares(row.action, row.shares)));
                }
            }
            book.trades.sort(Comparator.comparing((TradeEvent t) -> t.time,
                Comparator.nullsFirst(Comparator.naturalOrder())));
            for (TradeEvent t : book.trades) book.tradedSymbols.add(t.symbol);

            LocalDate start = null;
            for (CashEvent c : book.cash) start = earlier(start, c.time == null ? null : c.time.toLocalDate());
            if (start == null) for (TradeEvent t : book.trades) start = earlier(start, t.time == null ? null : t.time.toLocalDate());
            if (start == null) for (LocalDate d : book.createDates.values()) start = earlier(start, d);
            book.start = start;
            if (book.start == null) book.start = LocalDate.now();
        }

        // 6. 统一行情序列（取所有账本涉及标的，从最早账户起点前 40 天开始，便于取到起点前收盘价）
        Set<String> symbols = new HashSet<>();
        LocalDate minStart = null;
        for (PortfolioBook book : books.values()) {
            symbols.addAll(book.currentShares.keySet());
            symbols.addAll(book.tradedSymbols);
            minStart = earlier(minStart, book.start);
        }
        if (symbols.isEmpty() || minStart == null) return new ArrayList<>();

        String placeholders = String.join(",", Collections.nCopies(symbols.size(), "?"));
        List<Object> quoteArgs = new ArrayList<>(symbols);
        quoteArgs.add(java.sql.Date.valueOf(minStart.minusDays(40)));
        List<Map<String, Object>> quoteRows = jdbcTemplate.queryForList(
            "SELECT symbol, trade_date, close FROM stock_daily_quote " +
            "WHERE symbol IN (" + placeholders + ") AND trade_date >= ? ORDER BY trade_date",
            quoteArgs.toArray()
        );

        Map<String, TreeMap<LocalDate, Double>> prices = new HashMap<>();
        TreeSet<LocalDate> allDates = new TreeSet<>();
        for (Map<String, Object> row : quoteRows) {
            LocalDate d = toLocalDate(row.get("trade_date"));
            if (d == null || row.get("close") == null) continue;
            prices.computeIfAbsent(str(row.get("symbol")), k -> new TreeMap<>())
                  .put(d, toDouble(row.get("close")));
            if (!d.isBefore(minStart)) allDates.add(d);
        }
        if (allDates.isEmpty()) return new ArrayList<>();

        // 7. 逐用户重建每日 NAV / 资金流，再按日期汇总
        Map<LocalDate, double[]> aggregate = new TreeMap<>(); // [nav, stockValue, cash, externalFlow]
        for (PortfolioBook book : books.values()) {
            Map<LocalDate, Map<String, Integer>> posByDate = buildDailyPositions(book, allDates);
            TreeMap<LocalDate, Double> cashByDate = new TreeMap<>();
            for (CashEvent c : book.cash) {
                if (c.time != null) cashByDate.put(c.time.toLocalDate(), c.balance);
            }

            // 外部资金流：入金/出金/余额修正；手工新增持仓按“期初资本”计入，避免 NAV 跳升被算成收益
            TreeMap<LocalDate, Double> flowByDate = new TreeMap<>();
            for (CashEvent c : book.cash) {
                if (c.time == null || !c.time.toLocalDate().isAfter(book.start)) continue;
                if (isExternalFlowType(c.type)) {
                    flowByDate.merge(c.time.toLocalDate(), c.amount, Double::sum);
                }
            }
            for (Map.Entry<String, Integer> e : book.currentShares.entrySet()) {
                String symbol = e.getKey();
                if (book.tradedSymbols.contains(symbol)) continue;
                LocalDate createDate = book.createDates.getOrDefault(symbol, book.start);
                if (createDate.isAfter(book.start)) {
                    double price = priceAt(prices, book.cost, symbol, createDate);
                    flowByDate.merge(createDate, e.getValue() * price, Double::sum);
                }
            }

            Iterator<Map.Entry<LocalDate, Double>> flowIt = flowByDate.entrySet().iterator();
            Map.Entry<LocalDate, Double> nextFlow = flowIt.hasNext() ? flowIt.next() : null;
            double pendingFlow = 0;
            for (LocalDate d : allDates) {
                if (d.isBefore(book.start)) continue;
                while (nextFlow != null && !nextFlow.getKey().isAfter(d)) {
                    pendingFlow += nextFlow.getValue();
                    nextFlow = flowIt.hasNext() ? flowIt.next() : null;
                }
                Map<String, Integer> positions = posByDate.get(d);
                if (positions == null) continue;

                double stockValue = 0;
                for (Map.Entry<String, Integer> e : positions.entrySet()) {
                    int shares = e.getValue();
                    if (shares == 0) continue;
                    stockValue += shares * priceAt(prices, book.cost, e.getKey(), d);
                }
                Map.Entry<LocalDate, Double> cashEntry = cashByDate.floorEntry(d);
                double cash = cashEntry == null ? 0 : cashEntry.getValue();
                double nav = cash + stockValue;
                if (nav == 0 && stockValue == 0) continue;

                double[] agg = aggregate.computeIfAbsent(d, k -> new double[4]);
                agg[0] += nav;
                agg[1] += stockValue;
                agg[2] += cash;
                agg[3] += pendingFlow;
                pendingFlow = 0;
            }
        }

        // 8. 时间加权累计收益率（剔除入金/出金影响）
        List<PerformancePointVO> result = new ArrayList<>();
        double prevNav = -1;
        double cumulative = 0;
        for (Map.Entry<LocalDate, double[]> entry : aggregate.entrySet()) {
            double[] agg = entry.getValue();
            double nav = agg[0];
            if (prevNav < 0) {
                cumulative = 0;
            } else if (prevNav > 0) {
                double dailyReturn = (nav - prevNav - agg[3]) / prevNav;
                cumulative = (1 + cumulative) * (1 + dailyReturn) - 1;
            }

            PerformancePointVO point = new PerformancePointVO();
            point.setDate(entry.getKey().toString());
            point.setPortfolioValue(round2(nav));
            point.setStockValue(round2(agg[1]));
            point.setCash(round2(agg[2]));
            point.setTotalReturnPct(round2(cumulative * 100));
            result.add(point);
            prevNav = nav;
        }
        return result;
    }

    /** 从当前持仓倒推每日持仓：pos(d) = 当前持仓 - 日期 d 之后的所有买卖。 */
    private Map<LocalDate, Map<String, Integer>> buildDailyPositions(PortfolioBook book, TreeSet<LocalDate> allDates) {
        Map<LocalDate, Map<String, Integer>> result = new HashMap<>();
        Map<String, Integer> pos = new HashMap<>(book.currentShares);
        int tradeIndex = book.trades.size() - 1;
        List<LocalDate> dates = new ArrayList<>(allDates);
        for (int i = dates.size() - 1; i >= 0; i--) {
            LocalDate d = dates.get(i);
            while (tradeIndex >= 0) {
                TradeEvent t = book.trades.get(tradeIndex);
                if (t.time != null && t.time.toLocalDate().isAfter(d)) {
                    int value = pos.getOrDefault(t.symbol, 0) - t.signedShares;
                    pos.put(t.symbol, Math.max(value, 0));
                    tradeIndex--;
                } else {
                    break;
                }
            }
            // 无交易记录的手工持仓：按 create_time 作为建仓日，之前视为未持有
            for (Map.Entry<String, Integer> e : book.currentShares.entrySet()) {
                if (book.tradedSymbols.contains(e.getKey())) continue;
                LocalDate createDate = book.createDates.getOrDefault(e.getKey(), book.start);
                pos.put(e.getKey(), d.isBefore(createDate) ? 0 : e.getValue());
            }
            if (!d.isBefore(book.start)) {
                result.put(d, new HashMap<>(pos));
            }
        }
        return result;
    }

    private double priceAt(Map<String, TreeMap<LocalDate, Double>> prices,
                           Map<String, Double> cost, String symbol, LocalDate date) {
        TreeMap<LocalDate, Double> series = prices.get(symbol);
        if (series != null) {
            Map.Entry<LocalDate, Double> floor = series.floorEntry(date);
            if (floor != null && floor.getValue() != null) return floor.getValue();
        }
        Double c = cost.get(symbol);
        if (c != null && c > 0) return c;
        if (series != null) {
            Map.Entry<LocalDate, Double> ceiling = series.ceilingEntry(date);
            if (ceiling != null && ceiling.getValue() != null) return ceiling.getValue();
        }
        return 0;
    }

    private static boolean isTradeType(String type) {
        if (type == null) return false;
        String t = type.toLowerCase(Locale.ROOT);
        return "buy".equals(t) || "sell".equals(t) || "add".equals(t) || "reduce".equals(t);
    }

    private static boolean isExternalFlowType(String type) {
        if (type == null) return false;
        String t = type.toLowerCase(Locale.ROOT);
        return "deposit".equals(t) || "withdraw".equals(t) || "adjust".equals(t);
    }

    private static int signedShares(String action, int shares) {
        String a = action == null ? "" : action.toUpperCase(Locale.ROOT);
        return ("BUY".equals(a) || "ADD".equals(a)) ? shares : -shares;
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private static double toDouble(Object v) {
        return v == null ? 0.0 : ((Number) v).doubleValue();
    }

    private static String str(Object v) {
        return v == null ? "" : v.toString();
    }

    private static LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Date) return ((java.sql.Date) v).toLocalDate();
        if (v instanceof java.sql.Timestamp) return ((java.sql.Timestamp) v).toLocalDateTime().toLocalDate();
        if (v instanceof java.util.Date) {
            return ((java.util.Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (v instanceof LocalDate) return (LocalDate) v;
        String s = v.toString();
        return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
    }

    private static LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Timestamp) return ((java.sql.Timestamp) v).toLocalDateTime();
        if (v instanceof java.util.Date) {
            return ((java.util.Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        String s = v.toString().trim().replace('T', ' ');
        if (s.length() >= 19) return LocalDateTime.parse(s.substring(0, 19).replace(' ', 'T'));
        if (s.length() >= 10) return LocalDate.parse(s.substring(0, 10)).atStartOfDay();
        return LocalDateTime.MIN;
    }

    /** 现金流水事件（含买卖；type/note 用于识别交易）。 */
    private static class CashEvent {
        final LocalDateTime time;
        final double balance;
        final double amount;
        final String type;
        final String note;

        CashEvent(LocalDateTime time, double balance, double amount, String type, String note) {
            this.time = time;
            this.balance = balance;
            this.amount = amount;
            this.type = type;
            this.note = note;
        }
    }

    /** 重建后的交易事件（signedShares：买入为正，卖出为负）。 */
    private static class TradeEvent {
        final LocalDateTime time;
        final String symbol;
        final int signedShares;

        TradeEvent(LocalDateTime time, String symbol, int signedShares) {
            this.time = time;
            this.symbol = symbol;
            this.signedShares = signedShares;
        }
    }

    /** stock_trade_log 原始行（用于与现金 note 去重）。 */
    private static class TradeLogRow {
        final LocalDateTime time;
        final String symbol;
        final String action;
        final int shares;

        TradeLogRow(LocalDateTime time, String symbol, String action, int shares) {
            this.time = time;
            this.symbol = symbol;
            this.action = action;
            this.shares = shares;
        }
    }

    /** 单个用户的组合账本。 */
    private static class PortfolioBook {
        final Long userId;
        LocalDate start;
        final Map<String, Integer> currentShares = new HashMap<>();
        final Map<String, Double> cost = new HashMap<>();
        final Map<String, LocalDate> createDates = new HashMap<>();
        final List<CashEvent> cash = new ArrayList<>();
        final List<TradeLogRow> rawTrades = new ArrayList<>();
        final List<TradeEvent> trades = new ArrayList<>();
        final Set<String> tradedSymbols = new HashSet<>();

        PortfolioBook(Long userId) {
            this.userId = userId;
        }
    }

    @Override
    public KlineDataVO getKlineData(String symbol, String year) {
        // 1. 查询 K 线数据（含前一年数据用于 MA 预热）
        int yearInt = year != null ? Integer.parseInt(year) : java.time.LocalDate.now().getYear();
        String warmupStart = (yearInt - 1) + "0901";
        String yearStart = yearInt + "0101";
        List<Map<String, Object>> allRows = jdbcTemplate.queryForList(
            "SELECT trade_date, open, high, low, close, volume FROM stock_daily_quote " +
            "WHERE symbol = ? AND trade_date >= ? ORDER BY trade_date",
            symbol, warmupStart
        );

        if (allRows.isEmpty()) return null;

        // 2. 构建所有 bars 用于 MA 计算
        List<Double> allCloses = new ArrayList<>();
        List<String> allDates = new ArrayList<>();
        for (Map<String, Object> row : allRows) {
            allDates.add(row.get("trade_date").toString());
            allCloses.add(((Number) row.get("close")).doubleValue());
        }

        // 3. 找到当年数据的起始索引
        int yearStartIdx = 0;
        for (int i = 0; i < allDates.size(); i++) {
            if (allDates.get(i).compareTo(yearStart) >= 0) {
                yearStartIdx = i;
                break;
            }
        }

        // 4. 只构建当年的 bars，但 MA 用全部数据计算
        List<KlineBar> bars = new ArrayList<>();
        List<SignalMarker> signals = new ArrayList<>();

        for (int i = yearStartIdx; i < allDates.size(); i++) {
            Map<String, Object> row = allRows.get(i);
            KlineBar bar = new KlineBar();
            bar.setDate(allDates.get(i));
            bar.setOpen(((Number) row.get("open")).doubleValue());
            bar.setHigh(((Number) row.get("high")).doubleValue());
            bar.setLow(((Number) row.get("low")).doubleValue());
            bar.setClose(allCloses.get(i));
            Object vol = row.get("volume");
            bar.setVolume(vol != null ? ((Number) vol).longValue() : 0L);

            // MA10: 用 i 位置往前 10 个数据计算
            if (i >= 9) {
                double sum10 = 0;
                for (int j = i - 9; j <= i; j++) sum10 += allCloses.get(j);
                bar.setMa10(Math.round(sum10 / 10 * 100.0) / 100.0);
            }
            // MA30: 用 i 位置往前 30 个数据计算
            if (i >= 29) {
                double sum30 = 0;
                for (int j = i - 29; j <= i; j++) sum30 += allCloses.get(j);
                bar.setMa30(Math.round(sum30 / 30 * 100.0) / 100.0);
            }

            bars.add(bar);
        }

        // 4. 检测金叉/死叉信号
        for (int i = 30; i < bars.size(); i++) {
            KlineBar bar = bars.get(i);
            KlineBar prev = bars.get(i - 1);
            if (bar.getMa10() != null && prev.getMa10() != null
                && bar.getMa30() != null && prev.getMa30() != null) {
                boolean prevAbove = prev.getMa10() > prev.getMa30();
                boolean nowAbove = bar.getMa10() > bar.getMa30();
                if (!prevAbove && nowAbove) {
                    SignalMarker sig = new SignalMarker();
                    sig.setDate(bar.getDate());
                    sig.setType("golden_cross");
                    sig.setPrice(bar.getClose());
                    sig.setLabel("金叉");
                    signals.add(sig);
                } else if (prevAbove && !nowAbove) {
                    SignalMarker sig = new SignalMarker();
                    sig.setDate(bar.getDate());
                    sig.setType("death_cross");
                    sig.setPrice(bar.getClose());
                    sig.setLabel("死叉");
                    signals.add(sig);
                }
            }
        }

        // 5. 获取名称（优先持仓，其次 stock_basic_cache，行情共享 t24）
        String name = symbol;
        try {
            Map<String, Object> h = jdbcTemplate.queryForMap(
                "SELECT name FROM stock_holding WHERE " + userScope() + " AND symbol = ?", symbol);
            name = (String) h.get("name");
        } catch (Exception ignored) {
            try {
                Map<String, Object> b = jdbcTemplate.queryForMap(
                    "SELECT name FROM stock_basic_cache WHERE SUBSTRING(ts_code,1,6) = ? LIMIT 1", symbol);
                Object n = b.get("name");
                if (n != null && !n.toString().trim().isEmpty()) name = n.toString();
            } catch (Exception ignored2) {}
        }

        KlineDataVO vo = new KlineDataVO();
        vo.setSymbol(symbol);
        vo.setName(name);
        vo.setBars(bars);
        vo.setSignals(signals);
        return vo;
    }

    @Override
    public List<Map<String, Object>> searchDownloadedSymbols(String keyword, int limit) {
        if (keyword == null) keyword = "";
        String kw = keyword.trim();
        if (kw.isEmpty()) return new ArrayList<>();
        // 防止 limit 滥用
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String like = "%" + kw + "%";
        // 使用去重后的 symbol 派生表 + cache 名称，避免全表扫描过大
        // 匹配 symbol 或名称（大小写不敏感 LIKE）
        String sql = "SELECT d.symbol, COALESCE(b.name, d.symbol) as name " +
            "FROM (SELECT DISTINCT symbol FROM stock_daily_quote) d " +
            "LEFT JOIN stock_basic_cache b ON SUBSTRING(b.ts_code,1,6)=d.symbol " +
            "WHERE d.symbol LIKE ? OR b.name LIKE ? " +
            "ORDER BY d.symbol LIMIT ?";
        return jdbcTemplate.queryForList(sql, like, like, safeLimit);
    }

    /**
     * 全 A 股代码/名称互查（stock_basic_cache，5546 只），精确匹配排前。
     * add by CYQ 2026-10-08 新增持仓自动带出
     */
    @Override
    public List<Map<String, Object>> lookupSymbols(String keyword, int limit) {
        if (keyword == null) keyword = "";
        String kw = keyword.trim();
        if (kw.isEmpty()) return new ArrayList<>();
        int safeLimit = Math.max(1, Math.min(limit, 20));
        String like = "%" + kw + "%";
        String sql = "SELECT SUBSTRING(ts_code,1,6) AS symbol, name FROM stock_basic_cache "
            + "WHERE ts_code LIKE ? OR name LIKE ? "
            + "ORDER BY CASE WHEN SUBSTRING(ts_code,1,6)=? OR name=? THEN 0 ELSE 1 END, ts_code "
            + "LIMIT ?";
        return jdbcTemplate.queryForList(sql, like, like, kw, kw, safeLimit);
    }

    /**
     * 因子任务状态：最新 run + 运行中判定 + 数据新鲜度 + 因子结论计数。
     * 只读 stock_factor_* 研究表，不触碰用户持仓/建议表。
     * add by CYQ 2026-11-13（因子层 P2a）
     */
    @Override
    public Map<String, Object> getFactorStatus() {
        Map<String, Object> out = new LinkedHashMap<>();
        String latestRunId = null;
        try {
            Map<String, Object> run = jdbcTemplate.queryForMap(
                "SELECT run_id, trigger_type, stage, status, progress, start_time, end_time, " +
                "heartbeat, data_max_date, git_head, config_hash, error_msg " +
                "FROM stock_factor_run ORDER BY start_time DESC, run_id DESC LIMIT 1");
            out.put("run", run);
            latestRunId = str(run.get("run_id"));
            boolean running = "running".equals(str(run.get("status")));
            if (running && run.get("heartbeat") != null) {
                LocalDateTime hb = toLocalDateTime(run.get("heartbeat"));
                running = hb != null && Duration.between(hb, LocalDateTime.now()).toMinutes() <= 5;
            }
            out.put("running", running);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            out.put("run", null);
            out.put("running", false);
        }

        try {
            Object d = jdbcTemplate.queryForObject(
                "SELECT MAX(trade_date) FROM stock_daily_quote", Object.class);
            out.put("latestQuoteDate", d == null ? null : str(d));
        } catch (Exception e) {
            out.put("latestQuoteDate", null);
        }

        Map<String, Object> verdicts = new LinkedHashMap<>();
        if (latestRunId != null) {
            try {
                for (Map<String, Object> r : jdbcTemplate.queryForList(
                        "SELECT verdict, COUNT(*) AS cnt FROM stock_factor_eval " +
                        "WHERE run_id = ? GROUP BY verdict", latestRunId)) {
                    verdicts.put(str(r.get("verdict")), r.get("cnt"));
                }
            } catch (Exception ignored) {
            }
            try {
                Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM stock_factor_report WHERE run_id = ?",
                    Integer.class, latestRunId);
                out.put("reportCount", n == null ? 0 : n);
            } catch (Exception e) {
                out.put("reportCount", 0);
            }
        }
        out.put("verdictCounts", verdicts);
        out.put("latestRunId", latestRunId);

        try {
            Map<String, Object> log = jdbcTemplate.queryForMap(
                "SELECT job_message, status, start_time, stop_time FROM sys_job_log " +
                "WHERE invoke_target LIKE 'StockAnalysisTask.factorEval%' " +
                "ORDER BY start_time DESC LIMIT 1");
            out.put("job", log);
        } catch (Exception e) {
            out.put("job", null);
        }
        return out;
    }

    /** 因子结论列表（runId 为空取最新一次）。add by CYQ 2026-11-13 */
    @Override
    public List<Map<String, Object>> getFactorEvals(String runId) {
        String rid = runId;
        if (rid == null || rid.trim().isEmpty()) {
            try {
                rid = jdbcTemplate.queryForObject(
                    "SELECT run_id FROM stock_factor_run ORDER BY start_time DESC LIMIT 1",
                    String.class);
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        try {
            return jdbcTemplate.queryForList(
                "SELECT factor_name, universe, start_date, end_date, n_days, coverage, " +
                "ic_mean, ic_std, icir, ic_win_rate, ic_t_stat, fdr_p, ls_annual, ls_sharpe, " +
                "monotonicity, turnover, verdict, reasons_json, data_max_date, git_head " +
                "FROM stock_factor_eval WHERE run_id = ? " +
                "ORDER BY FIELD(verdict,'PASS','FAIL','INSUFFICIENT'), factor_name", rid);
        } catch (Exception e) {
            log.warn("读取因子结论失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 因子报告载荷（图表/回测 JSON，原样返回）。add by CYQ 2026-11-13 */
    @Override
    public Map<String, Object> getFactorReport(String runId, String kind) {
        String rid = runId;
        if (rid == null || rid.trim().isEmpty()) {
            try {
                rid = jdbcTemplate.queryForObject(
                    "SELECT run_id FROM stock_factor_run ORDER BY start_time DESC LIMIT 1",
                    String.class);
            } catch (Exception e) {
                return Collections.emptyMap();
            }
        }
        String k = (kind == null || kind.trim().isEmpty()) ? "summary" : kind.trim();
        try {
            String payload = jdbcTemplate.queryForObject(
                "SELECT payload_json FROM stock_factor_report WHERE run_id = ? AND kind = ?",
                String.class, rid, k);
            JSONObject obj = JSON.parseObject(payload);
            return obj == null ? Collections.emptyMap() : new LinkedHashMap<>(obj);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /** 因子运行历史（最近 N 次）。add by CYQ 2026-11-13 */
    @Override
    public List<Map<String, Object>> getFactorRuns(int limit) {
        int n = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 50));
        try {
            return jdbcTemplate.queryForList(
                "SELECT run_id, trigger_type, stage, status, progress, start_time, end_time, " +
                "data_max_date, git_head, config_hash, error_msg " +
                "FROM stock_factor_run ORDER BY start_time DESC LIMIT ?", n);
        } catch (Exception e) {
            log.warn("读取因子运行历史失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getBacktestJobStatus() {
        // 回测在【系统监控-定时任务】里手动执行（节点：回测LIVE），此处只读最近一次任务日志
        // add by CYQ 2026-09-06（前台不再直调 Quartz，hms-main 不依赖 hms-quartz）
        Map<String, Object> m = new HashMap<>();
        try {
            Map<String, Object> log = jdbcTemplate.queryForMap(
                "SELECT job_message, status, start_time, stop_time FROM sys_job_log " +
                "WHERE invoke_target LIKE 'StockAnalysisTask.runBacktestTask%' " +
                "ORDER BY start_time DESC LIMIT 1");
            Object msg = log.get("job_message");
            String s = msg == null ? "" : msg.toString();
            m.put("running", false);
            m.put("lastStart", String.valueOf(log.get("start_time")));
            m.put("lastEnd", String.valueOf(log.get("stop_time")));
            m.put("lastOk", "0".equals(String.valueOf(log.get("status"))));
            m.put("message", s.length() > 500 ? s.substring(0, 500) + "…" : s);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            m.put("running", false);
            m.put("lastStart", null);
            m.put("lastEnd", null);
            m.put("lastOk", null);
            m.put("message", "暂无执行记录，请在【系统监控-定时任务】手动执行 回测LIVE");
        }
        return m;
    }

    /** 逐笔成交行映射（PORTFOLIO 组合口径）。add by CYQ 2026-09-06 */
    private StockBacktestTradeVO mapTrade(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        StockBacktestTradeVO vo = new StockBacktestTradeVO();
        vo.setId(rs.getLong("id"));
        vo.setSymbol(rs.getString("symbol"));
        vo.setName(rs.getString("name"));
        vo.setStrategyType(rs.getString("strategy_type"));
        vo.setRunId(rs.getLong("run_id"));
        vo.setAction(rs.getString("action"));
        vo.setPrice(rs.getDouble("price"));
        vo.setVolume(rs.getInt("volume"));
        vo.setReason(rs.getString("reason"));
        vo.setTradeDate(rs.getDate("trade_date"));
        // DECIMAL 列经 JDBC 是 BigDecimal，不能直接强转 Double
        vo.setFee(nullableDouble(rs, "fee"));
        vo.setCashAfter(nullableDouble(rs, "cash_after"));
        vo.setPositionAfter(nullableInt(rs, "position_after"));
        vo.setRealizedPnl(nullableDouble(rs, "realized_pnl"));
        vo.setNav(nullableDouble(rs, "nav"));
        vo.setConfidence(rs.getString("confidence"));
        vo.setRegime(rs.getString("regime"));
        return vo;
    }

    /** 可空数值列 → 包装类型（NULL 留 null，前台显示"—"而非 0）。add by CYQ 2026-09-06 */
    private static Double nullableDouble(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).doubleValue();
    }

    /** add by CYQ 2026-09-06 */
    private static Integer nullableInt(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : ((Number) v).intValue();
    }

    /**
     * 组合级回测结果：runId 为空取用户域内最新一次；JSON 列解成结构化对象透出。
     * add by CYQ 2026-09-06
     */
    @Override
    public Map<String, Object> getPortfolioBacktest(Long runId) {
        String cols = "run_id, backtest_year, symbols_count, trading_days, initial_capital, final_value, "
            + "final_cash, final_market_value, total_return_pct, annual_return_pct, max_drawdown_pct, "
            + "sharpe_ratio, sortino_ratio, calmar_ratio, win_rate, evidence_status, "
            + "benchmark_code, benchmark_name, benchmark_return_pct, excess_return_pct, "
            + "trade_count, buy_count, sell_count, realized_pnl, "
            + "buy_fee, sell_fee, max_concurrent_positions, avg_cash_utilization_pct, "
            + "cash_reject_contended, equity_curve_json, positions_json, params_json, created_at";
        List<Map<String, Object>> rows = (runId == null)
            ? jdbcTemplate.queryForList(
                "SELECT " + cols + " FROM stock_backtest_portfolio WHERE " + backtestScope()
                + " ORDER BY run_id DESC LIMIT 1")
            : jdbcTemplate.queryForList(
                "SELECT " + cols + " FROM stock_backtest_portfolio WHERE run_id = ? AND " + backtestScope()
                + " LIMIT 1", runId);
        if (rows.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("exists", false);
            empty.put("message", "暂无组合回测结果，请在【系统监控-定时任务】手动执行 回测LIVE");
            return empty;
        }
        Map<String, Object> row = rows.get(0);
        Object navJson = row.remove("equity_curve_json");
        Object posJson = row.remove("positions_json");
        Object prmJson = row.remove("params_json");
        Map<String, Object> out = toCamel(row);
        out.put("exists", true);
        out.put("navCurve", parseJson(navJson));
        out.put("positions", parseJson(posJson));
        out.put("params", parseJson(prmJson));
        return out;
    }

    /**
     * 组合级回测历史批次（Python 侧每用户保留最近 10 次，此处同窗口）。
     * add by CYQ 2026-09-06
     */
    @Override
    public List<Map<String, Object>> listPortfolioRuns() {
        List<Map<String, Object>> raw = jdbcTemplate.queryForList(
            "SELECT run_id, backtest_year, symbols_count, trading_days, initial_capital, final_value, "
            + "total_return_pct, max_drawdown_pct, sharpe_ratio, win_rate, trade_count, "
            + "max_concurrent_positions, avg_cash_utilization_pct, created_at "
            + "FROM stock_backtest_portfolio WHERE " + backtestScope() + " ORDER BY run_id DESC LIMIT 10");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : raw) {
            out.add(toCamel(r));
        }
        return out;
    }

    /**
     * 指定批次的组合逐笔成交；runId 为空取最新一次。
     * add by CYQ 2026-09-06
     */
    @Override
    public List<StockBacktestTradeVO> getPortfolioTrades(Long runId) {
        Long rid = runId;
        if (rid == null) {
            List<Map<String, Object>> latest = jdbcTemplate.queryForList(
                "SELECT run_id FROM stock_backtest_trade WHERE strategy_type = 'PORTFOLIO' AND "
                + backtestScope() + " ORDER BY run_id DESC LIMIT 1");
            if (latest.isEmpty()) {
                return new ArrayList<>();
            }
            Object v = latest.get(0).get("run_id");
            rid = v == null ? null : ((Number) v).longValue();
        }
        if (rid == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT id, symbol, name, strategy_type, run_id, action, price, volume, reason, "
            + "trade_date, fee, cash_after, position_after, realized_pnl, nav, confidence, regime "
            + "FROM stock_backtest_trade WHERE strategy_type = 'PORTFOLIO' AND run_id = ? AND "
            + backtestScope() + " ORDER BY trade_date, id";
        return jdbcTemplate.query(sql, this::mapTrade, rid);
    }

    /** 下划线列名 → 小驼峰 key（组合回测列多，逐字段 set 无增益）。add by CYQ 2026-09-06 */
    private Map<String, Object> toCamel(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String k = e.getKey();
            StringBuilder sb = new StringBuilder(k.length());
            for (int i = 0; i < k.length(); i++) {
                char c = k.charAt(i);
                if (c == '_' && i + 1 < k.length()) {
                    sb.append(Character.toUpperCase(k.charAt(++i)));
                } else {
                    sb.append(c);
                }
            }
            m.put(sb.toString(), e.getValue());
        }
        return m;
    }

    /** JSON 文本列 → 结构化对象；解析失败降级为空集合而不是让整个接口 500。add by CYQ 2026-09-06 */
    private Object parseJson(Object v) {
        if (v == null) {
            return new ArrayList<>();
        }
        String s = String.valueOf(v);
        if (s.isEmpty() || "null".equals(s)) {
            return new ArrayList<>();
        }
        try {
            return JSON.parse(s);
        } catch (Exception ex) {
            log.warn("组合回测 JSON 列解析失败，已降级为空: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 触发组合级回测（单一共享现金账户）。Python 侧写 stock_backtest_portfolio 与
     * strategy_type='PORTFOLIO' 的逐笔成交。
     * add by CYQ 2026-09-06
     */
    @Override
    public String runPortfolioBacktestForUser(Long userId) {
        return runPortfolioBacktestForUser(userId, null);
    }

    /**
     * 触发组合级回测（单一共享现金账户）。year 为空走 config 默认年，
     * 否则透传 --year（Python 侧每年独立 run_id 落库，前台 runs 可切换对比）。
     * add by CYQ 2026-09-10
     */
    @Override
    public String runPortfolioBacktestForUser(Long userId, Integer year) {
        return runPortfolioBacktestForUser(userId, year, year);
    }

    /**
     * 触发组合级回测（连贯区间：起止年一次取全行情、单次撮合、单个 run_id 落库）。
     * 起止为空走 config 默认年；backtest_year 存起始年，完整范围进 params_json。
     * add by CYQ 2026-09-10
     */
    @Override
    public String runPortfolioBacktestForUser(Long userId, Integer startYear, Integer endYear) {
        return runPortfolioBacktestForUser(userId, startYear, endYear, null);
    }

    /**
     * 触发组合级回测（连贯区间 + 可选初始资金）。
     * 起止为空走 config 默认年；capital 为空走 config.defaults.capital。
     * add by CYQ 2026-09-24 初始资金
     */
    @Override
    public String runPortfolioBacktestForUser(Long userId, Integer startYear, Integer endYear, Double capital) {
        File veighNaDir = resolveVeighNaDir();
        if (veighNaDir == null) {
            return "❌ 找不到 veighNa 目录";
        }
        List<String> cmd = new ArrayList<>(Arrays.asList(
            resolvePythonExe(veighNaDir), "-X", "utf8", "main.py", "portfolio"));
        // 不传 --top：Python 侧默认取行情表全部标的（需收窄时再传数字取 topN）
        if (startYear != null && startYear >= 2000 && startYear <= 2100) {
            cmd.add("--year");
            cmd.add(String.valueOf(startYear));
            if (endYear != null && endYear > startYear && endYear <= 2100) {
                cmd.add("--end-year");
                cmd.add(String.valueOf(endYear));
            }
        }
        if (capital != null && capital >= 1000 && capital <= 100000000) {
            cmd.add("--capital");
            cmd.add(String.valueOf(capital));
        }
        if (userId != null && !isAdminUser()) {
            cmd.add("--user-id=" + userId);
        }
        return execPython(veighNaDir, cmd, "portfolio");
    }

    /** 运行 veighNa Python 子命令并回收全部输出（含 stderr，-X utf8 防 ¥/emoji 在管道下编码炸）。
     *  优先提取 __RESULT_JSON__ 结构化块（python-java-integration skill），日志只记紧凑摘要，
     *  避免 sys_job_log 2000 字符截断；无结构化块时回退原文。
     *  add by CYQ 2026-09-06 */
    private String execPython(File veighNaDir, List<String> cmd, String tag) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            log.info("{} 调度：{}，veighNaDir={}", tag, String.join(" ", cmd), veighNaDir.getAbsolutePath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            StringBuilder jsonBlock = new StringBuilder();
            boolean inJson = false;
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if ("__RESULT_JSON__".equals(line.trim())) { inJson = true; continue; }
                if ("__RESULT_JSON_END__".equals(line.trim())) { inJson = false; continue; }
                if (inJson) { jsonBlock.append(line); }
            }
            p.waitFor();
            log.info("{} 完成，exit={}", tag, p.exitValue());
            String structured = extractResultJson(jsonBlock.toString());
            if (structured != null) {
                log.info("{} 结构化摘要: {}", tag, structured);
                return structured;
            }
            return output.toString();
        } catch (Exception e) {
            log.error("{} 执行失败", tag, e);
            return "❌ " + tag + " 失败: " + e.getMessage();
        }
    }

    /**
     * 提取 Python __RESULT_JSON__ 块并压成紧凑摘要（Java 侧解析，替代全文截断）。
     * 无效/空块返回 null，调用方回退原文。add by CYQ 2026-09-09
     */
    private String extractResultJson(String block) {
        if (block == null || block.trim().isEmpty()) {
            return null;
        }
        try {
            com.alibaba.fastjson2.JSONObject o = JSON.parseObject(block.trim());
            return o.toJSONString();
        } catch (Exception e) {
            log.debug("RESULT_JSON 解析失败，回退原文: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        // 1. 持仓汇总（t24 多用户）
        try {
            Map<String, Object> summary = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) as stockCount, COALESCE(SUM(market_value),0) as totalValue, " +
                "COALESCE(SUM(profit_loss),0) as totalPl " +
                "FROM stock_holding WHERE shares > 0 AND " + userScope()
            );
            status.put("stockCount", summary.get("stockCount"));
            status.put("totalValue", summary.get("totalValue"));
            status.put("totalPl", summary.get("totalPl"));
        } catch (Exception e) {
            status.put("stockCount", 0);
            status.put("totalValue", 0);
            status.put("totalPl", 0);
        }

        // 2. 待处理建议（t24）
        try {
            Map<String, Object> pending = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) as cnt FROM stock_recommendation WHERE status = 'pending' " +
                "AND action != 'HOLD' AND " + userScope()
            );
            status.put("pendingCount", pending.get("cnt"));
        } catch (Exception e) {
            status.put("pendingCount", 0);
        }

        // 3. 最新行情日期
        try {
            Map<String, Object> latest = jdbcTemplate.queryForMap(
                "SELECT MAX(trade_date) as latestTradeDate FROM stock_daily_quote"
            );
            status.put("latestTradeDate", latest.get("latestTradeDate"));
        } catch (Exception e) {
            status.put("latestTradeDate", null);
        }

        // 4. 最后分析时间（t24）
        try {
            Map<String, Object> lastAnalyze = jdbcTemplate.queryForMap(
                "SELECT MAX(created_time) as lastTime FROM stock_recommendation WHERE " + userScope()
            );
            status.put("lastAnalyzeTime", lastAnalyze.get("lastTime"));
        } catch (Exception e) {
            status.put("lastAnalyzeTime", null);
        }

        // 5. 最后扫描时间（t24）
        try {
            Map<String, Object> lastScan = jdbcTemplate.queryForMap(
                "SELECT MAX(scan_time) as lastTime FROM stock_scan_result WHERE " + userScope()
            );
            status.put("lastScanTime", lastScan.get("lastTime"));
        } catch (Exception e) {
            status.put("lastScanTime", null);
        }

        // 6. 最后行情同步时间
        try {
            Map<String, Object> lastSync = jdbcTemplate.queryForMap(
                "SELECT MAX(exec_time) as lastTime FROM trade_log WHERE note LIKE '%sync_market_data%'"
            );
            status.put("lastSyncTime", lastSync.get("lastTime"));
        } catch (Exception e) {
            status.put("lastSyncTime", null);
        }

        // 7. 邮件配置状态（检查 mail.we 收件人）
        try {
            Map<String, Object> mailCfg = jdbcTemplate.queryForMap(
                "SELECT config_value FROM sys_config WHERE config_key = 'sys.index.mailTo'"
            );
            status.put("mailConfigured", mailCfg.get("config_value") != null);
        } catch (Exception e) {
            // 从 application.yml 读取不到，标记为已配置（默认配置存在）
            status.put("mailConfigured", true);
        }

        // 8. Tushare 连通性（通过最近行情数据判断，3天内有数据视为可达）
        boolean tushareReachable = false;
        try {
            Map<String, Object> tushareCheck = jdbcTemplate.queryForMap(
                "SELECT MAX(trade_date) as latestDate FROM stock_daily_quote"
            );
            Object latestDate = tushareCheck.get("latestDate");
            if (latestDate != null) {
                String dateStr = latestDate.toString();
                // trade_date 格式为 yyyyMMdd，取最近3天
                String threeDaysAgo = java.time.LocalDate.now().minusDays(3)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                tushareReachable = dateStr.compareTo(threeDaysAgo) >= 0;
            }
        } catch (Exception e) {
            tushareReachable = false;
        }
        status.put("tushareReachable", tushareReachable);

        // 9. 最近交易记录（t24）
        try {
            Map<String, Object> lastTrade = jdbcTemplate.queryForMap(
                "SELECT MAX(trade_time) as lastTime FROM stock_trade_log WHERE " + userScope()
            );
            status.put("lastTradeTime", lastTrade.get("lastTime"));
        } catch (Exception e) {
            status.put("lastTradeTime", null);
        }

        return status;
    }

    /**
     * 查询所有未发送邮件通知的 pending 信号并发送（含 SELL/REDUCE 止损信号，统一一封）
     * add by CYQ 2026-07-06 待处理信号邮件通知
     * update by CYQ 2026-08-20 包含所有动作信号，避免止损信号重复发邮件
     * update by CYQ 2026-09-22 以 notified 标志替代 5 分钟时间窗口：整条分析流水线耗时约 30 分钟，
     *                           信号生成到通知步骤执行常超过 5 分钟，导致漏发（如 002050/002241 SELL）。
     */
    @Override
    public String sendSignalNotification(String source) {
        try {
            // 查询所有 pending 且未发过邮件的信号，带归属 user_id 与 sys_user.email（多用户 t24）
            List<Map<String, Object>> signals = jdbcTemplate.queryForList(
                "SELECT r.id, r.user_id, COALESCE(u.email, '') as email, r.symbol, r.name, r.action, " +
                "r.price_hint, r.suggested_shares, r.suggested_amount, r.reason, r.confidence, r.strategy_name, " +
                "COALESCE(h.profit_loss_pct, 0) as profit_loss_pct " +
                "FROM stock_recommendation r " +
                "LEFT JOIN sys_user u ON r.user_id = u.user_id " +
                "LEFT JOIN stock_holding h ON r.symbol = h.symbol AND r.user_id = h.user_id " +
                "WHERE r.status = 'pending' AND r.notified = 0 " +
                "ORDER BY r.user_id, confidence DESC, created_time DESC"
            );

            if (signals.isEmpty()) {
                log.info("无新增待通知信号（pending 且 notified=0 为 0），跳过邮件通知");
                return "无新增待通知信号（历史信号均已通知过）";
            }

            log.info("发现 {} 条未通知信号，按用户分组发送邮件通知", signals.size());

            // 全局兜底收件人（用户 email 为空时使用）
            String fallbackTo = MailConfig.getMe();

            // 按 user_id 分组
            Map<Long, List<Map<String, Object>>> byUser = new LinkedHashMap<>();
            for (Map<String, Object> s : signals) {
                Long uid = s.get("user_id") != null
                    ? ((Number) s.get("user_id")).longValue() : 0L;
                byUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(s);
            }

            int totalSent = 0;
            int skippedNoEmail = 0;
            for (Map.Entry<Long, List<Map<String, Object>>> e : byUser.entrySet()) {
                List<Map<String, Object>> userSignals = e.getValue();
                // 取该用户第一条的 email；为空 → 全局兜底
                String email = (String) userSignals.get(0).get("email");
                String to = (email != null && !email.trim().isEmpty()) ? email.trim() : fallbackTo;
                if (to == null || to.trim().isEmpty()) {
                    log.warn("用户 {} 无邮箱且无全局兜底，跳过邮件（{} 条信号保持未通知，下次仍会重试）",
                             e.getKey(), userSignals.size());
                    skippedNoEmail += userSignals.size();
                    continue;
                }

                // 构建该用户的 HTML 邮件
                // 判断紧急程度：存在 SELL/REDUCE 且持仓亏损 < -25% 为紧急
                boolean hasStop = userSignals.stream().anyMatch(s ->
                    "SELL".equals(s.get("action")) || "REDUCE".equals(s.get("action")));
                boolean isUrgent = userSignals.stream().anyMatch(s -> {
                    Object pct = s.get("profit_loss_pct");
                    return ("SELL".equals(s.get("action")) || "REDUCE".equals(s.get("action")))
                        && pct != null && ((Number) pct).doubleValue() < -25;
                });
                String prefix = isUrgent ? "🚨【紧急止损】" : hasStop ? "⚠️【止损/减仓】" : "📊【信号通知】";

                StringBuilder html = new StringBuilder();
                html.append("<html><body style='font-family: Arial, sans-serif;'>");
                html.append("<h2 style='color: #2c3e50;'>").append(prefix).append(" 待处理信号</h2>");
                html.append("<p><strong>来源：</strong>").append(source)
                    .append("（用户ID ").append(e.getKey()).append("）</p>");
                html.append("<p><strong>信号数量：</strong>").append(userSignals.size()).append(" 条</p>");
                html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; width: 100%;'>");
                html.append("<tr style='background-color: #3498db; color: white;'>");
                html.append("<th>代码</th><th>名称</th><th>操作</th><th>价格</th><th>股数</th><th>金额</th><th>持仓盈亏%</th><th>置信度</th><th>策略</th><th>理由</th>");
                html.append("</tr>");

                for (Map<String, Object> sig : userSignals) {
                    String action = (String) sig.get("action");
                    String actionColor = "BUY".equals(action) ? "#27ae60" :
                                         "SELL".equals(action) ? "#e74c3c" :
                                         "REDUCE".equals(action) ? "#f39c12" : "#3498db";
                    html.append("<tr>");
                    html.append("<td>").append(sig.get("symbol")).append("</td>");
                    html.append("<td>").append(sig.get("name")).append("</td>");
                    html.append("<td style='color: ").append(actionColor).append("; font-weight: bold;'>").append(action).append("</td>");
                    html.append("<td>").append(sig.get("price_hint")).append("</td>");
                    html.append("<td>").append(sig.get("suggested_shares")).append("</td>");
                    html.append("<td>¥").append(sig.get("suggested_amount")).append("</td>");
                    Object pl = sig.get("profit_loss_pct");
                    String plStr = pl != null ? String.format("%.1f%%", ((Number) pl).doubleValue()) : "-";
                    String plColor = pl != null && ((Number) pl).doubleValue() < -25 ? "#e74c3c" : "#7f8c8d";
                    html.append("<td style='color: ").append(plColor).append(";'>").append(plStr).append("</td>");
                    html.append("<td>").append(sig.get("confidence")).append("</td>");
                    html.append("<td>").append(sig.get("strategy_name")).append("</td>");
                    html.append("<td style='font-size: 12px;'>").append(sig.get("reason")).append("</td>");
                    html.append("</tr>");
                }

                html.append("</table>");
                html.append("<p style='color: #7f8c8d; font-size: 12px; margin-top: 20px;'>");
                html.append("请登录系统处理待处理信号：<a href='https://your-domain.com'>HMS 健康管理系统</a>");
                html.append("</p>");
                html.append("</body></html>");

                // 逐用户发送（统一走邮件集成策略，自动记录集成日志）
                String subject = "【HMS】" + source + " - " + userSignals.size() + " 条新待处理信号";
                emailIntegrationStrategy.execute(new EmailSendRequest(
                    to.split(","), subject, html.toString(), "signal-" + e.getKey()));

                // 发送成功后置 notified=1，保证每条信号只发一次（不依赖时间窗口）
                List<Object> ids = new ArrayList<>();
                StringBuilder in = new StringBuilder();
                for (Map<String, Object> sig : userSignals) {
                    ids.add(sig.get("id"));
                    in.append(in.length() == 0 ? "?" : ",?");
                }
                jdbcTemplate.update(
                    "UPDATE stock_recommendation SET notified = 1 WHERE id IN (" + in + ")",
                    ids.toArray());

                totalSent += userSignals.size();
                log.info("邮件通知已发送 {} 条信号 至 {}，信号ID已置为已通知", userSignals.size(), to);
            }

            log.info("邮件分组发送完成：命中 {} 条未通知，实发 {} 条，因无邮箱跳过 {} 条",
                     signals.size(), totalSent, skippedNoEmail);
            StringBuilder rep = new StringBuilder("已发送 ").append(totalSent).append(" 条邮件");
            rep.append("（命中 ").append(signals.size()).append(" 条未通知");
            if (skippedNoEmail > 0) {
                rep.append("；").append(skippedNoEmail).append(" 条因无邮箱未发，仍为未通知");
            }
            rep.append("）");
            return rep.toString();
        } catch (Exception e) {
            log.error("发送信号通知邮件失败", e);
            return "❌ 发送失败: " + e.getMessage();
        }
    }

    /**
     * 新增持仓
     * add by CYQ 2026-07-06 持仓管理
     */
    @Override
    public void addHolding(String symbol, String name, int shares, double costPrice) {
        // 多用户：写入当前用户归属（t24）
        Long uid = currentUserId();
        double marketValue = shares * costPrice;
        jdbcTemplate.update(
            "INSERT INTO stock_holding (user_id, symbol, name, shares, available_shares, cost_price, " +
            "current_price, market_value, profit_loss, profit_loss_pct, source) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'manual')",
            uid, symbol, name, shares, shares, costPrice, costPrice, marketValue
        );
        log.info("新增持仓(user {}): {} {} {}股 成本价{}", uid, symbol, name, shares, costPrice);
        // 手工仓与策略仓同权纳管：建仓即分配策略，当日就进 scheduler 的卖出/止损链路
        // 并占用组合格子（scanner/scheduler 均按 stock_holding 全量统计，不区分 source）。
        // add by CYQ 2026-10-27 手工持仓纳管
        adoptStrategyForHolding(symbol);
    }

    /**
     * 更新持仓
     * add by CYQ 2026-07-06 持仓管理
     */
    @Override
    public void updateHolding(String symbol, Integer shares, Double costPrice) {
        // 多用户：限定当前用户自己的持仓（t24）
        Long uid = currentUserId();
        StringBuilder sql = new StringBuilder("UPDATE stock_holding SET ");
        List<Object> params = new ArrayList<>();

        if (shares != null) {
            sql.append("shares = ?, available_shares = ?, ");
            params.add(shares);
            params.add(shares);
        }
        if (costPrice != null) {
            sql.append("cost_price = ?, ");
            params.add(costPrice);
        }

        // 更新市值
        sql.append("market_value = shares * cost_price ");
        sql.append("WHERE user_id = ? AND symbol = ?");
        params.add(uid);
        params.add(symbol);

        jdbcTemplate.update(sql.toString(), params.toArray());
        log.info("更新持仓(user {}): {} shares={} costPrice={}", uid, symbol, shares, costPrice);
    }

    /**
     * 删除持仓
     * add by CYQ 2026-07-06 持仓管理
     */
    @Override
    public void deleteHolding(String symbol) {
        // 多用户：限定当前用户自己的持仓（t24）
        Long uid = currentUserId();
        jdbcTemplate.update("DELETE FROM stock_holding_atr_state WHERE user_id = ? AND symbol = ?", uid, symbol);
        jdbcTemplate.update("DELETE FROM stock_holding WHERE user_id = ? AND symbol = ?", uid, symbol);
        log.info("删除持仓(user {}): {}", uid, symbol);
    }

    /**
     * 获取孤儿持仓列表（有持仓但无 active 策略记录的标的）
     * add by CYQ 2026-07-07 (task t_456ebc43)
     */
    @Override
    public List<Map<String, Object>> getOrphanHoldings() {
        // 多用户：限定当前用户；strategy 归属同一 user 才算已分配（t24）
        String scope = userScope("h");   // h = stock_holding 主表，避免与子查询 stock_strategy s 的 user_id 歧义
        String sql = "SELECT h.symbol, h.name, h.shares, h.market_value, " +
            "h.profit_loss, h.profit_loss_pct " +
            "FROM stock_holding h " +
            "WHERE h.shares > 0 AND " + scope + " " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM stock_strategy s " +
            "  WHERE s.symbol = h.symbol AND s.user_id = h.user_id AND s.active = 1" +
            ") " +
            "ORDER BY h.market_value DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 处理待执行的止损/减仓信号:
     * 1) 自动过期超 7 天的 pending 信号
     * 2) 查询统计待处理 SELL/REDUCE 信号（邮件由 sendSignalNotification 统一发送）
     * <p>
     * add by CYQ 2026-07-16 (task t_5232f6f2)
     * update by CYQ 2026-08-20 去除邮件发送，避免与 sendSignalNotification 重复发两封
     */
    @Override
    public String processPendingStopSignals() {
        StringBuilder report = new StringBuilder();

        try {
            // 1. Auto-expire pending signals older than 7 days
            int expired = jdbcTemplate.update(
                "UPDATE stock_recommendation SET status = 'expired' " +
                "WHERE status = 'pending' AND created_time < DATE_SUB(NOW(), INTERVAL 7 DAY)"
            );
            if (expired > 0) {
                report.append("自动过期 ").append(expired).append(" 条超期信号 (7天)\n");
                log.info("自动过期 {} 条超期 pending 信号", expired);
            }

            // 2. 统计剩余待处理 SELL/REDUCE 信号（状态计数）与其中"从未通知过"的增量
            //    （只统计不发邮件；发信由 sendSignalNotification 统一负责，避免重复发两封）
            Integer stopCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_recommendation " +
                "WHERE status = 'pending' AND action IN ('SELL', 'REDUCE')",
                Integer.class
            );
            Integer unnotified = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_recommendation " +
                "WHERE status = 'pending' AND action IN ('SELL', 'REDUCE') AND notified = 0",
                Integer.class
            );
            int pending = stopCount == null ? 0 : stopCount;
            int unNotified = unnotified == null ? 0 : unnotified;

            if (pending == 0) {
                report.append("无待处理止损/减仓信号\n");
                log.info("无待处理止损/减仓信号");
                return report.toString();
            }

            // 括注要如实：只有未通知的才会由本轮的统一通知发送；已通知过的不再发。
            report.append("待处理止损/减仓 ").append(pending).append(" 条（其中未通知 ")
                  .append(unNotified).append(" 条将由统一通知发送，已通知 ")
                  .append(pending - unNotified).append(" 条不重复发送）\n");
            log.info("止损信号统计: 待处理 {} 条（未通知 {} / 已通知 {}）, 自动过期 {} 条",
                     pending, unNotified, pending - unNotified, expired);

        } catch (Exception e) {
            log.error("处理止损信号失败", e);
            report.append("处理异常: ").append(e.getMessage()).append("\n");
        }

        return report.toString();
    }

    // ==================== 持仓管理 ====================

    /**
     * 手工建仓后立即分配策略（建仓即纳管）。
     * <p>
     * 手工仓此前要等次日 scheduler 跑完才有 stock_strategy 记录，期间既无止损/卖出信号，
     * 在前台也看不出是否被策略管理。这里复用 advisor.assign_orphans 单标的入口补齐，
     * 失败只记日志、不影响持仓写入（次日 scheduler 仍会兜底分配）。
     * add by CYQ 2026-10-27 手工持仓纳管
     */
    private void adoptStrategyForHolding(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        try {
            File veighNaDir = resolveVeighNaDir();
            if (veighNaDir == null) {
                log.warn("手工建仓 {} 未分配策略：找不到 veighNa 目录（次日 scheduler 兜底）", symbol);
                return;
            }
            Long uid = currentUserId();
            ProcessBuilder pb = new ProcessBuilder(
                resolvePythonExe(veighNaDir),
                "-X", "utf8",
                "-m", "advisor.assign_orphans",
                symbol, "--json",
                "--user-id", String.valueOf(uid)
            );
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("{")) {
                        output.append(line);
                    }
                }
            }
            if (!p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                log.warn("手工建仓 {} 策略分配超时(30s)，次日 scheduler 兜底", symbol);
                return;
            }
            log.info("手工建仓 {} 策略分配结果: {}", symbol,
                     output.length() > 200 ? output.substring(0, 200) : output);
        } catch (Exception e) {
            log.warn("手工建仓 {} 策略分配失败（不影响持仓写入）: {}", symbol, e.getMessage());
        }
    }

    @Override
    public String assignOrphanStrategies() {
        try {
            // 多用户：只处理当前登录用户（t24）
            Long uid = currentUserId();
            File veighNaDir = resolveVeighNaDir();
            if (veighNaDir == null) {
                return "找不到 veighNa 目录";
            }

            ProcessBuilder pb = new ProcessBuilder(
                resolvePythonExe(veighNaDir),
                "-X", "utf8",
                "-m", "advisor.assign_orphans",
                "--auto", "--json",
                "--user-id", String.valueOf(uid)
            );
            pb.directory(veighNaDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过日志行，只输出 JSON 结果
                if (line.startsWith("{") || line.startsWith("[")) {
                    output.append(line);
                }
            }
            p.waitFor();
            log.info("assignOrphanStrategies 完成，veighNaDir={}", veighNaDir.getAbsolutePath());

            if (output.length() == 0) {
                return "{\"status\":\"ok\",\"message\":\"无孤儿持仓或执行完毕\"}";
            }
            return output.toString();

        } catch (Exception e) {
            log.error("孤儿策略分配失败", e);
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}

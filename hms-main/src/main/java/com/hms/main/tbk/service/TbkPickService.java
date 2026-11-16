package com.hms.main.tbk.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;
import com.hms.common.constant.CacheConstants;
import com.hms.common.core.redis.RedisCache;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.client.PddTbkClient;
import com.hms.main.tbk.client.TbkAggregatorClient;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkConvertResult;
import com.hms.main.tbk.domain.TbkPickItem;
import com.hms.main.tbk.domain.TbkPickRequest;
import com.hms.main.tbk.domain.TbkPickResult;

/**
 * AI 选品服务
 *
 * 流程：自然语言需求 → AI 解析搜索词/预算/偏好 → 淘宝 + 拼多多并行搜索
 *      → 规则预排序 → AI 按「需求匹配 / 价格 / 优惠 / 销量 / 风险」打性价比分。
 *
 * AI 不可用时自动降级为规则排序，保证功能可用。
 *
 * add by CYQ 2026-11-04 优惠查询-AI 选品
 */
@Service
public class TbkPickService {

    private static final Logger log = LoggerFactory.getLogger(TbkPickService.class);

    private static final String CACHE_PREFIX = CacheConstants.TBK_PICK_KEY;

    /** 需求描述最大长度 */
    private static final int MAX_CONTENT_LENGTH = 300;

    /** 固定窗口限流：ip -> [窗口开始毫秒, 次数] */
    private final Map<String, long[]> rateWindow = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private TbkProperties properties;

    @Autowired
    private TbkAggregatorClient aggregatorClient;

    @Autowired
    private PddTbkClient pddClient;

    @Autowired
    private TbkConvertService convertService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private TbkLinkService linkService;

    /**
     * 输入标题/商品/需求，跨平台搜索并给出高性价比排序
     *
     * @param request 选品请求
     * @param ip      请求 IP，用于限流
     */
    public TbkPickResult pick(TbkPickRequest request, String ip) {
        return pick(request, ip, null);
    }

    /**
     * 输入标题/商品/需求，跨平台搜索并给出高性价比排序
     *
     * @param request 选品请求
     * @param ip      请求 IP，用于限流
     * @param userKey 用户归因标识（拼多多候选链接会带上）
     */
    public TbkPickResult pick(TbkPickRequest request, String ip, String userKey) {
        long start = System.currentTimeMillis();
        String content = request == null ? null : trim(request.getContent());
        if (StringUtils.isEmpty(content)) {
            throw new ServiceException("请先描述你想买什么，例如：200元以内的降噪蓝牙耳机，通勤用");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException("需求写得太长了，请精简到 300 字以内");
        }
        checkRateLimit(ip);

        String attribution = value(userKey);
        String cacheKey = CACHE_PREFIX + DigestUtil.md5Hex(
                content + "|" + value(request.getBudget()) + "|" + value(request.getMaxItems())
                        + "|" + attribution);
        TbkPickResult cached = getCache(cacheKey);
        if (cached != null) {
            // 返现比例可能被调过，缓存结果按当前配置重算预估返利
            applyCashback(cached.getItems());
            cached.setNotice("来自缓存，价格和券可能已变化，下单前请核对");
            cached.setCostMs(0L);
            return cached;
        }

        List<String> warnings = new CopyOnWriteArrayList<>();
        Intent intent = resolveIntent(content, request.getBudget());

        List<TbkPickItem> candidates = searchAll(intent.getKeyword(), intent.getFallbackKeyword(), warnings, userKey);
        if (candidates.isEmpty()) {
            throw new ServiceException("没有搜到合适的商品，换个更通用的说法试试，比如只写「蓝牙耳机」");
        }

        int maxItems = resolveMaxItems(request.getMaxItems());
        List<TbkPickItem> items = filterByBudget(candidates, intent.getBudget(), warnings);
        for (TbkPickItem item : items) {
            item.setRuleScore(ruleScore(item, intent.getBudget()));
        }
        items.sort(Comparator.comparingInt(
                (TbkPickItem item) -> item.getRuleScore() == null ? 0 : item.getRuleScore()).reversed());
        if (items.size() > maxItems) {
            items = diversify(items, maxItems);
        }

        // 预估返利统一由后端按 tbk.cashback-rate 算好，前端只展示不折算
        // add by CYQ 2026-11-14 平台服务费，返现比例 90%
        applyCashback(items);

        TbkPickResult result = new TbkPickResult();
        result.setInput(content);
        result.setKeyword(intent.getKeyword());
        result.setBudget(intent.getBudget());
        result.setPreferences(intent.getPreferences());
        result.setCategory(intent.getCategory());
        result.setWarnings(warnings);
        result.setItems(items);

        // 记录候选链接台账，供订单近似归因
        linkService.savePickLinks(items, userKey);

        rankByAi(content, intent, items, result, warnings);
        if (result.isAiUsed()) {
            result.setNotice("AI 评分基于平台公开的标题、价格、优惠券和销量，仅供参考；下单前请核对规格与券后价");
        } else {
            result.setNotice("当前为规则排序（AI 评分暂不可用），按券后价、优惠力度和销量综合排序，仅供参考");
        }

        result.setCostMs(System.currentTimeMillis() - start);
        putCache(cacheKey, result);
        return result;
    }

    /**
     * 把选品结果里的某个候选转成推广链接/淘口令（复用现有转链流程）
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    public TbkConvertResult convertCandidate(TbkPickRequest request, String ip) {
        return convertCandidate(request, ip, null);
    }

    /**
     * 把选品结果里的某个候选转成推广链接/淘口令（复用现有转链流程）
     *
     * @param userKey 用户归因标识，拼多多会写入 custom_parameters.uid
     */
    public TbkConvertResult convertCandidate(TbkPickRequest request, String ip, String userKey) {
        if (request == null || StringUtils.isEmpty(request.getPlatform())) {
            throw new ServiceException("缺少平台信息，请重新搜索商品");
        }
        String title = trim(request.getTitle());
        String content;
        if ("pdd".equalsIgnoreCase(request.getPlatform())) {
            if (StringUtils.isEmpty(request.getGoodsSign())) {
                throw new ServiceException("该商品缺少拼多多商品标识，请重新搜索");
            }
            content = withTitle(title, "https://mobile.yangkeduo.com/goods.html?goods_sign="
                    + request.getGoodsSign()
                    + (StringUtils.isNotEmpty(request.getItemId()) ? "&goods_id=" + request.getItemId() : ""));
        } else {
            String itemUrl = trim(request.getItemUrl());
            if (StringUtils.isEmpty(itemUrl)) {
                if (StringUtils.isEmpty(request.getItemId())) {
                    throw new ServiceException("缺少商品链接，请重新搜索");
                }
                itemUrl = "https://item.taobao.com/item.htm?id=" + request.getItemId();
            }
            content = withTitle(title, itemUrl);
        }
        return convertService.convert(content, ip, userKey);
    }

    private String withTitle(String title, String material) {
        return StringUtils.isEmpty(title) ? material : "「" + title + "」 " + material;
    }

    /**
     * 按 tbk.cashback-rate 计算每个候选的预估返利（前端直接展示，不再自己乘比例）
     *
     * 预估返利 = 券后价 × 佣金比例% × 返现比例%
     * add by CYQ 2026-11-14 平台服务费，返现比例 90%
     */
    private void applyCashback(List<TbkPickItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        BigDecimal rate = properties.currentCashbackRate();
        BigDecimal hundred = BigDecimal.valueOf(100);
        for (TbkPickItem item : items) {
            if (item == null) {
                continue;
            }
            item.setCashbackRate(rate);
            BigDecimal price = item.getFinalPrice();
            BigDecimal commissionRate = item.getCommissionRate();
            if (price == null || commissionRate == null
                    || price.compareTo(BigDecimal.ZERO) <= 0
                    || commissionRate.compareTo(BigDecimal.ZERO) <= 0) {
                item.setCashbackAmount(BigDecimal.ZERO);
                continue;
            }
            BigDecimal commission = price.multiply(commissionRate).divide(hundred, 2, RoundingMode.HALF_UP);
            item.setCashbackAmount(commission.multiply(rate).divide(hundred, 2, RoundingMode.HALF_UP));
        }
    }

    // ------------------------------------------------------------------ 需求解析

    /**
     * 用 AI 把自然语言需求拆成搜索词 + 预算 + 偏好；失败时按原文兜底
     */
    private Intent resolveIntent(String content, BigDecimal requestBudget) {
        Intent intent = new Intent();
        JSONObject json = null;
        try {
            json = parseJson(chat(AIChatFactory.StrategyType.SHOPPING_INTENT, content, 20L));
        } catch (Exception e) {
            log.warn("AI 需求解析失败，按原始输入搜索: {}", e.getMessage());
        }
        if (json != null) {
            intent.setKeyword(cleanKeyword(json.getString("keyword")));
            intent.setFallbackKeyword(cleanKeyword(json.getString("fallbackKeyword")));
            intent.setBudget(parseDecimal(json.get("budget")));
            intent.setPreferences(toStringList(json.getJSONArray("preferences")));
            intent.setCategory(json.getString("category"));
        }
        if (StringUtils.isEmpty(intent.getKeyword())) {
            intent.setKeyword(cleanKeyword(content));
        }
        if (StringUtils.isEmpty(intent.getKeyword())) {
            intent.setKeyword(content.length() > 30 ? content.substring(0, 30) : content);
        }
        // 用户显式填写的预算优先
        if (requestBudget != null && requestBudget.compareTo(BigDecimal.ZERO) > 0) {
            intent.setBudget(requestBudget);
        }
        return intent;
    }

    /**
     * 去掉口语词和价格描述，得到可直接搜索的商品词
     */
    private String cleanKeyword(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String text = value
                .replaceAll("帮我|请|麻烦|推荐|想买|想要|买一个|买|求购|求|找|有没有|一下|吧|呢|啊", " ")
                .replaceAll("\\d+(?:\\.\\d+)?\\s*(?:元|块钱|块)(?:以内|以下|左右|上下)?", " ")
                .replaceAll("预算\\s*\\d+(?:\\.\\d+)?", " ")
                .replaceAll("[，。！？、,.!?;；:：\"'“”‘’()（）\\[\\]【】]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() > 30) {
            text = text.substring(0, 30).trim();
        }
        return text.isEmpty() ? null : text;
    }

    // ------------------------------------------------------------------ 跨平台搜索

    private List<TbkPickItem> searchAll(String keyword, String fallbackKeyword, List<String> warnings, String userKey) {
        int size = properties.getPickSearchSize();
        CompletableFuture<List<TbkPickItem>> taobaoFuture = CompletableFuture.supplyAsync(
                () -> safeSearch(() -> searchPlatform(true, keyword, fallbackKeyword, size, userKey),
                        "淘宝", keyword, warnings));
        CompletableFuture<List<TbkPickItem>> pddFuture = CompletableFuture.supplyAsync(
                () -> safeSearch(() -> searchPlatform(false, keyword, fallbackKeyword, size, userKey),
                        "拼多多", keyword, warnings));

        List<TbkPickItem> all = new ArrayList<>();
        collect(taobaoFuture, "淘宝", all, warnings);
        collect(pddFuture, "拼多多", all, warnings);

        // 同平台同商品可能因为价格/券不同重复出现，按 平台+商品ID 去重
        Map<String, TbkPickItem> unique = new LinkedHashMap<>();
        for (TbkPickItem item : all) {
            if (item == null || StringUtils.isEmpty(item.getTitle())) {
                continue;
            }
            String key = item.getPlatform() + ":" + item.getItemId();
            if (!unique.containsKey(key)) {
                unique.put(key, item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * 单平台搜索：精确词结果太少（拼多多对关键词也比较挑）时用宽泛词补充
     */
    private List<TbkPickItem> searchPlatform(boolean taobao, String keyword, String fallbackKeyword,
                                             int size, String userKey) {
        List<TbkPickItem> items = new ArrayList<>();
        List<TbkPickItem> first = taobao
                ? aggregatorClient.search(keyword, size) : pddClient.search(keyword, size, userKey);
        if (first != null) {
            items.addAll(first);
        }
        if (items.size() < 4 && StringUtils.isNotEmpty(fallbackKeyword) && !fallbackKeyword.equals(keyword)) {
            log.info("选品搜索「{}」仅 {} 条，补充宽泛词「{}」", keyword, items.size(), fallbackKeyword);
            List<TbkPickItem> more = taobao
                    ? aggregatorClient.search(fallbackKeyword, size)
                    : pddClient.search(fallbackKeyword, size, userKey);
            if (more != null) {
                items.addAll(more);
            }
        }
        return items;
    }

    private List<TbkPickItem> safeSearch(Callable<List<TbkPickItem>> task, String platformName,
                                         String keyword, List<String> warnings) {
        try {
            List<TbkPickItem> items = task.call();
            log.info("{} 选品搜索「{}」返回 {} 条", platformName, keyword,
                    items == null ? 0 : items.size());
            return items == null ? Collections.emptyList() : items;
        } catch (ServiceException e) {
            warnings.add(platformName + "：" + e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("{} 选品搜索失败: {}", platformName, e.getMessage(), e);
            warnings.add(platformName + "搜索失败，已跳过");
            return Collections.emptyList();
        }
    }

    private void collect(CompletableFuture<List<TbkPickItem>> future, String platformName,
                         List<TbkPickItem> all, List<String> warnings) {
        try {
            all.addAll(future.get(15, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.warn("{} 选品搜索超时或异常: {}", platformName, e.getMessage());
            warnings.add(platformName + "搜索超时，已跳过");
        }
    }

    /**
     * 预算过滤：预算内商品足够多时只留预算内，不够时把超预算的排到后面并提示
     */
    private List<TbkPickItem> filterByBudget(List<TbkPickItem> items, BigDecimal budget, List<String> warnings) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return new ArrayList<>(items);
        }
        List<TbkPickItem> within = items.stream()
                .filter(item -> item.getFinalPrice() != null && item.getFinalPrice().compareTo(budget) <= 0)
                .collect(Collectors.toList());
        if (within.size() >= 4) {
            return within;
        }
        warnings.add("预算内的商品不多，已把略超预算的选项排在后面");
        List<TbkPickItem> over = items.stream()
                .filter(item -> !within.contains(item))
                .collect(Collectors.toList());
        List<TbkPickItem> merged = new ArrayList<>(within);
        merged.addAll(over);
        return merged;
    }

    private int resolveMaxItems(Integer requestMax) {
        int configured = properties.getPickMaxCandidates() > 0 ? properties.getPickMaxCandidates() : 12;
        if (requestMax != null && requestMax > 0) {
            return Math.min(requestMax, configured);
        }
        return configured;
    }

    /**
     * 结果截断时保证每个平台至少有 2 个候选，避免「跨平台比价」变成单平台列表
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    private List<TbkPickItem> diversify(List<TbkPickItem> sorted, int maxItems) {
        List<TbkPickItem> selected = new ArrayList<>();
        Map<String, Integer> perPlatform = new HashMap<>();
        for (TbkPickItem item : sorted) {
            String platform = item.getPlatform();
            int count = perPlatform.getOrDefault(platform, 0);
            if (count < 2) {
                selected.add(item);
                perPlatform.put(platform, count + 1);
            }
            if (selected.size() >= maxItems) {
                break;
            }
        }
        if (selected.size() < maxItems) {
            for (TbkPickItem item : sorted) {
                if (selected.contains(item)) {
                    continue;
                }
                selected.add(item);
                if (selected.size() >= maxItems) {
                    break;
                }
            }
        }
        selected.sort(Comparator.comparingInt(
                (TbkPickItem item) -> item.getRuleScore() == null ? 0 : item.getRuleScore()).reversed());
        return selected;
    }

    // ------------------------------------------------------------------ AI 打分

    /**
     * 调用 AI 并限制等待时间：超时立即降级，不让用户等满前端超时
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    private String chat(AIChatFactory.StrategyType strategyType, String prompt, long timeoutSeconds)
            throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            AbstractAIChatTemplate ai = AIChatFactory.createAIChat(
                    AIChatFactory.ServiceType.OPENCODE_GO, strategyType);
            Map<String, Object> context = new HashMap<>();
            context.put("biz", "tbk-pick");
            return ai.chat(prompt, context);
        });
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
    }

    private void rankByAi(String content, Intent intent, List<TbkPickItem> items,
                          TbkPickResult result, List<String> warnings) {
        JSONObject json = null;
        try {
            json = parseJson(chat(AIChatFactory.StrategyType.SHOPPING_PICK,
                    buildRankPrompt(content, intent, items), 30L));
        } catch (Exception e) {
            log.warn("AI 选品打分失败，改用规则排序: {}", e.getMessage());
            warnings.add("AI 打分超时或暂时不可用，已改用规则排序");
        }
        if (json != null) {
            try {
                applyAiResult(json, items, result);
                result.setAiUsed(true);
                return;
            } catch (Exception e) {
                log.warn("AI 选品打分结果解析失败，改用规则排序: {}", e.getMessage());
                warnings.add("AI 打分结果解析失败，已按价格/销量规则排序");
            }
        }
        result.setAiUsed(false);
        for (TbkPickItem item : items) {
            if (item.getScore() == null) {
                item.setScore(item.getRuleScore());
            }
            if (StringUtils.isEmpty(item.getReason())) {
                item.setReason(ruleReason(item));
            }
        }
        result.setSummary(items.isEmpty() ? "没有找到合适的商品"
                : "按券后价、优惠力度和销量综合排序，供你参考");
        result.setAdvice("建议优先核对券后价与规格是否匹配需求；同一商品可以换个平台再比一次。");
        result.setBestIndex(items.isEmpty() ? null : 1);
    }

    private void applyAiResult(JSONObject json, List<TbkPickItem> items, TbkPickResult result) {
        JSONArray arr = json.getJSONArray("items");
        if (arr == null) {
            throw new IllegalArgumentException("AI 返回缺少 items");
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject node = arr.getJSONObject(i);
            int index = node.getIntValue("index");
            if (index < 1 || index > items.size()) {
                continue;
            }
            TbkPickItem item = items.get(index - 1);
            Integer score = node.getInteger("score");
            item.setScore(score == null ? item.getRuleScore() : clampScore(score));
            item.setReason(node.getString("reason"));
            item.setTags(toStringList(node.getJSONArray("tags")));
            item.setCaution(node.getString("caution"));
        }
        for (TbkPickItem item : items) {
            if (item.getScore() == null) {
                item.setScore(item.getRuleScore());
            }
            if (StringUtils.isEmpty(item.getReason())) {
                item.setReason(ruleReason(item));
            }
        }
        // bestIndex 先按 AI 原始编号定位，排序后再换算成展示顺序
        int best = json.getIntValue("bestIndex");
        TbkPickItem bestItem = best >= 1 && best <= items.size() ? items.get(best - 1) : null;
        items.sort(Comparator.comparingInt(
                (TbkPickItem item) -> item.getScore() == null ? 0 : item.getScore()).reversed());
        result.setSummary(json.getString("summary"));
        result.setAdvice(json.getString("advice"));
        result.setBestIndex(bestItem == null ? (items.isEmpty() ? null : 1) : items.indexOf(bestItem) + 1);
    }

    private String buildRankPrompt(String content, Intent intent, List<TbkPickItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户需求：").append(content).append('\n');
        sb.append("搜索词：").append(intent.getKeyword()).append('\n');
        sb.append("预算：").append(intent.getBudget() == null ? "未限定"
                : intent.getBudget().stripTrailingZeros().toPlainString() + " 元").append('\n');
        sb.append("偏好：").append(intent.getPreferences() == null || intent.getPreferences().isEmpty()
                ? "无" : String.join("、", intent.getPreferences())).append('\n');
        sb.append("候选商品（共 ").append(items.size()).append(" 个）：\n");
        for (int i = 0; i < items.size(); i++) {
            TbkPickItem item = items.get(i);
            sb.append(i + 1).append(". ")
              .append(item.getPlatformName()).append("｜")
              .append(shortTitle(item.getTitle())).append("｜")
              .append("券后 ¥").append(money(item.getFinalPrice()));
            if (item.getOriginalPrice() != null) {
                sb.append("｜原价 ¥").append(money(item.getOriginalPrice()));
            }
            if (item.getCouponAmount() != null) {
                sb.append("｜券 ¥").append(money(item.getCouponAmount()));
            }
            if (item.getSales() != null) {
                sb.append("｜销量 ").append(item.getSales());
            }
            if (StringUtils.isNotEmpty(item.getShopName())) {
                sb.append("｜店铺 ").append(item.getShopName());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 规则兜底

    /**
     * 规则打分：预算契合 + 折扣力度 + 销量 + 有券
     */
    private int ruleScore(TbkPickItem item, BigDecimal budget) {
        int score = 55;
        if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0 && item.getFinalPrice() != null) {
            double ratio = item.getFinalPrice().doubleValue() / budget.doubleValue();
            if (ratio <= 1) {
                score += (int) Math.round(10 + 10 * (1 - ratio));
            } else {
                score -= 20;
            }
        }
        if (item.getOriginalPrice() != null && item.getFinalPrice() != null
                && item.getOriginalPrice().compareTo(item.getFinalPrice()) > 0) {
            double off = 1 - item.getFinalPrice().doubleValue() / item.getOriginalPrice().doubleValue();
            score += (int) Math.round(Math.min(off / 0.5, 1) * 20);
        }
        if (item.getSales() != null && item.getSales() > 0) {
            double factor = Math.min(Math.log10(item.getSales() + 1) / 4.0, 1);
            score += (int) Math.round(factor * 10);
        }
        if (item.getCouponAmount() != null && item.getCouponAmount().compareTo(BigDecimal.ZERO) > 0) {
            score += 3;
        }
        return clampScore(score);
    }

    private String ruleReason(TbkPickItem item) {
        List<String> parts = new ArrayList<>();
        if (item.getFinalPrice() != null) {
            parts.add("券后 ¥" + money(item.getFinalPrice()));
        }
        if (item.getOriginalPrice() != null && item.getFinalPrice() != null
                && item.getOriginalPrice().compareTo(item.getFinalPrice()) > 0) {
            parts.add("省 " + money(item.getOriginalPrice().subtract(item.getFinalPrice())) + " 元");
        }
        if (item.getSales() != null && item.getSales() > 0) {
            parts.add("销量 " + item.getSales());
        }
        if (StringUtils.isNotEmpty(item.getShopName())) {
            parts.add(item.getShopName());
        }
        return parts.isEmpty() ? "按价格与平台排序" : String.join("；", parts);
    }

    private int clampScore(int score) {
        return Math.max(30, Math.min(99, score));
    }

    // ------------------------------------------------------------------ 限流 / 缓存 / 工具

    private void checkRateLimit(String ip) {
        int limit = properties.getPickRateLimitPerMinute();
        if (limit <= 0 || StringUtils.isEmpty(ip)) {
            return;
        }
        long now = System.currentTimeMillis();
        long[] window = rateWindow.compute(ip, (key, old) -> {
            if (old == null || now - old[0] >= 60_000L) {
                return new long[] { now, 1L };
            }
            old[1]++;
            return old;
        });
        if (window[1] > limit) {
            throw new ServiceException("AI 选品比较耗时，请 1 分钟后再试");
        }
        if (rateWindow.size() > 10_000) {
            rateWindow.entrySet().removeIf(entry -> now - entry.getValue()[0] >= 60_000L);
        }
    }

    private TbkPickResult getCache(String key) {
        try {
            String json = redisCache.getCacheObject(key);
            return StringUtils.isEmpty(json) ? null : JSON.parseObject(json, TbkPickResult.class);
        } catch (Exception e) {
            log.debug("读取选品缓存失败，忽略: {}", e.getMessage());
            return null;
        }
    }

    private void putCache(String key, TbkPickResult result) {
        int seconds = properties.getPickCacheSeconds();
        if (seconds <= 0) {
            return;
        }
        try {
            redisCache.setCacheObject(key, JSON.toJSONString(result), seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("写入选品缓存失败，忽略: {}", e.getMessage());
        }
    }

    private JSONObject parseJson(String raw) {
        if (StringUtils.isEmpty(raw)) {
            throw new IllegalArgumentException("AI 返回为空");
        }
        String text = raw.trim()
                .replaceAll("^```\\w*\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI 未返回 JSON");
        }
        return JSON.parseObject(text.substring(start, end + 1));
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).replaceAll("[^0-9.]", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            BigDecimal number = new BigDecimal(text);
            return number.compareTo(BigDecimal.ZERO) > 0 ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> toStringList(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String value = array.getString(i);
            if (StringUtils.isNotEmpty(value)) {
                list.add(value);
            }
        }
        return list;
    }

    private String shortTitle(String title) {
        if (title == null) {
            return "";
        }
        String text = title.trim();
        return text.length() > 60 ? text.substring(0, 60) + "…" : text;
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * AI 解析出的选品意图
     */
    @lombok.Data
    private static class Intent {
        private String keyword;
        private String fallbackKeyword;
        private BigDecimal budget;
        private List<String> preferences;
        private String category;
    }
}

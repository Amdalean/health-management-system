package com.hms.main.tbk.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.hms.common.constant.CacheConstants;
import com.hms.common.core.redis.RedisCache;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.client.PddLinkParser;
import com.hms.main.tbk.client.PddTbkClient;
import com.hms.main.tbk.client.TbkAggregatorClient;
import com.hms.main.tbk.client.TbkLinkParser;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.TbkConvertLog;
import com.hms.main.tbk.domain.TbkConvertResult;
import com.hms.main.tbk.domain.TbkMaterial;
import com.hms.main.tbk.mapper.TbkConvertLogMapper;

/**
 * 淘宝客转链服务
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Service
public class TbkConvertService {

    private static final Logger log = LoggerFactory.getLogger(TbkConvertService.class);

    private static final String CACHE_PREFIX = CacheConstants.TBK_CONVERT_KEY;

    /** 拼多多分享文案：`【拼多多】商品标题 https://...` */
    private static final java.util.regex.Pattern PDD_TITLE_PATTERN =
            java.util.regex.Pattern.compile("【拼多多】\\s*([^\\n\\r]+?)(?=\\s*https?://|\\s*[,，。；]|$)");

    /** 固定窗口限流：ip -> [窗口开始毫秒, 次数] */
    private final Map<String, long[]> rateWindow = new ConcurrentHashMap<>();

    @Autowired
    private TbkAggregatorClient aggregatorClient;

    @Autowired
    private PddTbkClient pddClient;

    @Autowired
    private TbkProperties properties;

    @Autowired
    private TbkConvertLogMapper convertLogMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private TbkLinkService linkService;

    /**
     * 把用户粘贴的任意淘宝文案转成带推广位的链接和淘口令
     *
     * @param content 用户原始输入
     * @param ip      请求 IP，用于限流和流水记录
     */
    public TbkConvertResult convert(String content, String ip) {
        return convert(content, ip, null);
    }

    /**
     * 把用户粘贴的任意淘宝文案转成带推广位的链接和淘口令
     *
     * @param content 用户原始输入
     * @param ip      请求 IP，用于限流和流水记录
     * @param userKey 用户归因标识（登录用户为用户ID，匿名为 anon:xxx），拼多多会写入 custom_parameters
     */
    public TbkConvertResult convert(String content, String ip, String userKey) {
        if (StringUtils.isEmpty(content)) {
            throw new ServiceException("请先粘贴商品分享文案或链接");
        }
        if (content.length() > properties.getMaxContentLength()) {
            throw new ServiceException("粘贴内容过长，请只保留商品链接或分享文案");
        }

        checkRateLimit(ip);

        TbkMaterial material = TbkLinkParser.parse(content);
        if (material == null) {
            throw new ServiceException("没有识别到商品链接，请从淘宝/拼多多重新复制分享文案");
        }

        // 拼多多链接自动走多多进宝通道，淘宝/天猫继续走 tbk.provider 配置的通道
        boolean pdd = PddLinkParser.isPddMaterial(material.getMaterial());
        String provider = pdd ? pddClient.providerName() : aggregatorClient.providerName();

        // 缓存必须带上通道名：切换 tbk.provider 后不能把上一个通道的结果继续发出去
        // 拼多多链接带用户归因，缓存 key 必须带用户，避免把别人的推广链接发出去
        String cacheKey = CACHE_PREFIX + provider + ":" + DigestUtil.md5Hex(material.getMaterial())
                + (pdd && StringUtils.isNotEmpty(userKey) ? ":" + DigestUtil.md5Hex(userKey) : "");
        TbkConvertResult cached = getCache(cacheKey);
        if (cached != null) {
            // 返现比例可能被调过，缓存结果也要按当前配置重算
            applyCashback(cached);
            if (!cached.isDemo()) {
                cached.setNotice("来自缓存，券信息可能已更新");
            }
            return cached;
        }

        String contentTitle = extractTitleFromContent(content);
        if (pdd && StringUtils.isEmpty(contentTitle)) {
            // 用户可能只粘了「商品标题 + 链接」，没有【拼多多】前缀
            contentTitle = extractPddTitleBeforeUrl(content);
        }

        TbkConvertResult result;
        try {
            result = pdd
                    ? pddClient.convert(material.getMaterial(), contentTitle, userKey)
                    : aggregatorClient.convert(material.getMaterial(), material.getItemId(), contentTitle);
        } catch (ServiceException e) {
            saveLog(content, material, null, ip, provider, "1", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("转链异常: {}", e.getMessage(), e);
            saveLog(content, material, null, ip, provider, "1", e.getMessage());
            throw new ServiceException("转链失败，请稍后重试");
        }

        if (StringUtils.isEmpty(result.getTitle())) {
            result.setTitle(contentTitle);
        }
        applyCashback(result);
        result.setCopyText(buildCopyText(result));
        saveLog(content, material, result, ip, provider, "0", null);
        // 记录链接台账，供订单近似归因（不阻塞转链）
        linkService.saveConvertLink(result, provider, userKey, "convert");
        putCache(cacheKey, result);
        return result;
    }

    /**
     * 拼多多：从「商品标题 + 链接」里取标题（用户没按官方分享格式粘贴时的兜底）
     */
    private String extractPddTitleBeforeUrl(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        String text = content.replaceAll("https?://\\S+", " ")
                .replace("【拼多多】", " ")
                .replace("【淘宝】", " ")
                .replaceAll("^[\\s:：,，。;；\\-]+", " ");
        int lineEnd = text.indexOf('\n');
        if (lineEnd > 0) {
            text = text.substring(0, lineEnd);
        }
        text = text.trim();
        if (text.length() < 4 || text.contains("复制这条信息") || text.contains("打开拼多多")
                || text.contains("点击链接") || text.contains("仅需")) {
            return null;
        }
        return text.length() > 100 ? text.substring(0, 100) : text;
    }

    /**
     * 商品详情接口拿不到标题时，从用户分享文案里提取「」中的标题兜底
     */
    private String extractTitleFromContent(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        // 注意标题里本身可能带「」：取第一个开括号到最后一个闭括号，避免只截到「设计师」
        String title = betweenFirstAndLast(content, "「", "」");
        if (title == null) {
            title = betweenFirstAndLast(content, "《", "》");
        }
        if (title != null) {
            return title;
        }
        java.util.regex.Matcher pdd = PDD_TITLE_PATTERN.matcher(content);
        if (pdd.find()) {
            String pddTitle = pdd.group(1).trim();
            return pddTitle.length() > 100 ? pddTitle.substring(0, 100) : pddTitle;
        }
        return null;
    }

    /**
     * 取第一个开括号到最后一个闭括号之间的内容（兼容标题里嵌套「」）
     */
    private String betweenFirstAndLast(String content, String open, String close) {
        int start = content.indexOf(open);
        int end = content.lastIndexOf(close);
        if (start < 0 || end <= start) {
            return null;
        }
        String title = content.substring(start + open.length(), end).trim();
        if (title.length() < 2 || title.length() > 200) {
            return null;
        }
        return title.length() > 100 ? title.substring(0, 100) : title;
    }

    /**
     * 组装可直接发送给用户的整段文案
     */
    private String buildCopyText(TbkConvertResult result) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(result.getTitle())) {
            String title = result.getTitle().trim();
            // 标题本身已经带【】/「」/《》时不要再套一层
            if (title.startsWith("【") || title.startsWith("「") || title.startsWith("《")) {
                sb.append(title).append("\n");
            } else {
                sb.append("【").append(title).append("】\n");
            }
        }
        if (result.getOriginalPrice() != null && result.getFinalPrice() != null) {
            sb.append("原价 ¥").append(plain(result.getOriginalPrice()))
              .append(" → 券后 ¥").append(plain(result.getFinalPrice()));
            if (result.getCouponAmount() != null && result.getCouponAmount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("（省 ").append(plain(result.getCouponAmount())).append(" 元）");
            }
            sb.append("\n");
        } else if (result.getFinalPrice() != null) {
            // 没有真实原价/券时只报当前价格，避免"原价250→203"这种误导
            sb.append("价格 ¥").append(plain(result.getFinalPrice())).append("\n");
        }
        if (StringUtils.isNotEmpty(result.getTpwd())) {
            sb.append("复制这条信息，打开手机淘宝：").append(result.getTpwd()).append("\n");
        }
        if (StringUtils.isNotEmpty(result.getShortUrl())) {
            sb.append("链接：").append(result.getShortUrl());
        }
        return sb.toString().trim();
    }

    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /**
     * 按 tbk.cashback-rate 配置计算「预估返利」，前端直接展示，不再自行折算
     *
     * 只有这一个配置来源：转链返回、选品返回、订单落库三处口径一致。
     * add by CYQ 2026-11-14 平台服务费，返现比例 90%
     */
    private void applyCashback(TbkConvertResult result) {
        if (result == null) {
            return;
        }
        BigDecimal rate = properties.currentCashbackRate();
        result.setCashbackRate(rate);
        BigDecimal commission = result.getCommission();
        if (commission == null || commission.compareTo(BigDecimal.ZERO) <= 0) {
            result.setCashbackAmount(BigDecimal.ZERO);
            return;
        }
        result.setCashbackAmount(commission.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    /**
     * 单 IP 固定窗口限流，防止公开接口被刷
     */
    private void checkRateLimit(String ip) {
        int limit = properties.getRateLimitPerMinute();
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
            throw new ServiceException("操作太频繁，请稍后再试");
        }
        // 简单的容量保护，避免长期运行后 map 无限增长
        if (rateWindow.size() > 10_000) {
            rateWindow.entrySet().removeIf(entry -> now - entry.getValue()[0] >= 60_000L);
        }
    }

    private TbkConvertResult getCache(String key) {
        try {
            String json = redisCache.getCacheObject(key);
            return StringUtils.isEmpty(json) ? null : JSON.parseObject(json, TbkConvertResult.class);
        } catch (Exception e) {
            log.debug("读取转链缓存失败，忽略: {}", e.getMessage());
            return null;
        }
    }

    private void putCache(String key, TbkConvertResult result) {
        int seconds = properties.getCacheSeconds();
        if (seconds <= 0) {
            return;
        }
        try {
            redisCache.setCacheObject(key, JSON.toJSONString(result), seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("写入转链缓存失败，忽略: {}", e.getMessage());
        }
    }

    private void saveLog(String content, TbkMaterial material, TbkConvertResult result,
                         String ip, String provider, String status, String errorMsg) {
        try {
            TbkConvertLog entity = new TbkConvertLog();
            entity.setContent(content.length() > 500 ? content.substring(0, 500) : content);
            entity.setItemId(result != null ? result.getItemId() : material.getItemId());
            entity.setTitle(result != null ? result.getTitle() : null);
            entity.setProvider(provider);
            entity.setIp(ip);
            entity.setStatus(status);
            entity.setErrorMsg(errorMsg != null && errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
            entity.setCreateTime(new Date());
            convertLogMapper.insertTbkConvertLog(entity);
        } catch (Exception e) {
            // 流水表未建或数据库异常不能影响转链主流程
            log.warn("写入转链流水失败: {}", e.getMessage());
        }
    }
}

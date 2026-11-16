package com.hms.main.tbk.client;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hms.common.utils.StringUtils;

/**
 * 拼多多链接识别与商品标识解析
 *
 * 支持：
 * - 商品页 https://mobile.yangkeduo.com/goods.html?goods_id=123456
 * - 二合一 https://yangkeduo.com/duo_coupon_landing.html?goods_id=123456
 * - 短链 https://p.pinduoduo.com/xxxx（服务端跟随跳转）
 * - 新商品标识 goods_sign
 *
 * add by CYQ 2026-11-03 转链模块支持拼多多
 */
public final class PddLinkParser {

    private static final Logger log = LoggerFactory.getLogger(PddLinkParser.class);

    private static final String UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    /** 拼多多域名 */
    private static final Pattern PDD_HOST = Pattern.compile(
            "(?i)(?:^|\\.)(?:yangkeduo\\.com|pinduoduo\\.com|pdd\\.cn)$");

    /** 链接/页面里的数字商品ID */
    private static final Pattern GOODS_ID = Pattern.compile(
            "(?:goods_id|goodsId|\"goodsID\")[\"']?\\s*[:=]\\s*[\"']?(\\d{6,20})");

    /** 链接/页面里的新商品标识 */
    private static final Pattern GOODS_SIGN = Pattern.compile(
            "(?:goods_sign|goodsSign)[\"']?\\s*[:=]\\s*[\"']?([0-9A-Za-z_-]{16,80})");

    /** 商品页路径 https://mobile.yangkeduo.com/goods/123456 */
    private static final Pattern GOODS_PATH = Pattern.compile("/(?:goods|goods2)/(\\d{6,20})");

    private static final Pattern ANY_URL = Pattern.compile("https?://[^\\s\"'<>\\\\]+", Pattern.CASE_INSENSITIVE);

    private static final int MAX_REDIRECTS = 5;

    private static final int MAX_BODY_BYTES = 300_000;

    private PddLinkParser() {
    }

    /**
     * 判断用户粘贴的素材是不是拼多多链接
     */
    public static boolean isPddMaterial(String material) {
        if (StringUtils.isEmpty(material)) {
            return false;
        }
        String url = material.trim();
        if (!url.startsWith("http")) {
            return false;
        }
        return isPddHost(host(url));
    }

    /**
     * 解析链接得到拼多多商品标识（短链会自动跟随跳转）
     *
     * @return 解析不到返回 null
     */
    public static PddGoodsRef resolve(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        PddGoodsRef direct = fromText(url);
        if (direct != null) {
            return direct;
        }
        if (!url.startsWith("http")) {
            return null;
        }

        HttpURLConnection connection = null;
        try {
            String currentUrl = url.trim();
            int redirects = 0;
            while (redirects++ < MAX_REDIRECTS) {
                connection = open(currentUrl);
                int status = connection.getResponseCode();
                String location = connection.getHeaderField("Location");
                if (isRedirect(status) && StringUtils.isNotEmpty(location)) {
                    currentUrl = absolutize(currentUrl, location);
                    PddGoodsRef ref = fromText(currentUrl);
                    if (ref != null) {
                        return ref;
                    }
                    connection.disconnect();
                    connection = null;
                    continue;
                }
                break;
            }
            if (connection == null) {
                return null;
            }

            PddGoodsRef ref = fromText(connection.getURL().toString());
            if (ref != null) {
                return ref;
            }

            // 只有还停在 p.pinduoduo.com/pdd.cn 这种短链落地页时才读响应体找真实商品地址；
            // 跳到 mobile.yangkeduo.com 首页说明短链已失效，页面里的推荐位商品ID不能用
            if (isShortLinkHost(host(connection.getURL().toString()))) {
                return fromText(readBody(connection));
            }
            return null;
        } catch (Exception e) {
            log.warn("拼多多链接解析失败 url={}: {}", url, e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 从任意文本（URL / HTML / JSON）里提取商品标识
     */
    public static PddGoodsRef fromText(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        // 新版链接同时带 goods_id 和 goods_sign，优先用 goods_sign（数字ID已不能调接口）
        Matcher signMatcher = GOODS_SIGN.matcher(text);
        if (signMatcher.find()) {
            return new PddGoodsRef(findGoodsId(text), signMatcher.group(1));
        }
        Matcher idMatcher = GOODS_ID.matcher(text);
        if (idMatcher.find()) {
            return new PddGoodsRef(idMatcher.group(1), null);
        }
        // 页面里可能只出现商品页地址
        Matcher urlMatcher = ANY_URL.matcher(text);
        while (urlMatcher.find()) {
            String candidate = urlMatcher.group();
            if (!isPddHost(host(candidate))) {
                continue;
            }
            Matcher sign = GOODS_SIGN.matcher(candidate);
            if (sign.find()) {
                return new PddGoodsRef(findGoodsId(candidate), sign.group(1));
            }
            Matcher id = GOODS_ID.matcher(candidate);
            if (id.find()) {
                return new PddGoodsRef(id.group(1), null);
            }
            Matcher path = GOODS_PATH.matcher(candidate);
            if (path.find()) {
                return new PddGoodsRef(path.group(1), null);
            }
        }
        return null;
    }

    private static boolean isPddHost(String host) {
        return StringUtils.isNotEmpty(host) && PDD_HOST.matcher(host).find();
    }

    private static String findGoodsId(String text) {
        Matcher matcher = GOODS_ID.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 短链落地页域名（会在页面里用 JS/meta 跳转到商品页）
     */
    private static boolean isShortLinkHost(String host) {
        return "p.pinduoduo.com".equalsIgnoreCase(host) || "pdd.cn".equalsIgnoreCase(host);
    }

    private static String host(String url) {
        try {
            String value = URI.create(url).getHost();
            return value == null ? null : value.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", UA);
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        return connection;
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static String absolutize(String base, String location) {
        try {
            return new URL(new URL(base), location).toString();
        } catch (Exception e) {
            return location;
        }
    }

    private static String readBody(HttpURLConnection connection) throws Exception {
        InputStream in = connection.getResponseCode() >= 400
                ? connection.getErrorStream() : connection.getInputStream();
        if (in == null) {
            return null;
        }
        if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
            in = new GZIPInputStream(in);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) > 0 && total < MAX_BODY_BYTES) {
                out.write(buffer, 0, read);
                total += read;
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } finally {
            in.close();
        }
    }

    /**
     * 拼多多商品标识：数字 goods_id 或新标识 goods_sign
     */
    public static class PddGoodsRef {

        private final String goodsId;

        private final String goodsSign;

        public PddGoodsRef(String goodsId, String goodsSign) {
            this.goodsId = goodsId;
            this.goodsSign = goodsSign;
        }

        public String getGoodsId() {
            return goodsId;
        }

        public String getGoodsSign() {
            return goodsSign;
        }

        public boolean isNumericId() {
            return StringUtils.isNotEmpty(goodsId);
        }
    }
}

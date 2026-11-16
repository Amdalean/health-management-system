package com.hms.main.tbk.client;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hms.common.utils.StringUtils;

/**
 * 淘宝短链解析（m.tb.cn / e.tb.cn / s.click.taobao.com 等）
 *
 * 两种短链行为都要兼容：
 * - m.tb.cn 之类：直接 302 到商品页，取最终地址即可；
 * - e.tb.cn（新版分享短链）：返回 200 的 HTML，真实地址写在 <script> 的
 *   var url = 'https://item.taobao.com/item.htm?id=xxx' 里，必须读响应体才能拿到商品ID。
 *
 * add by CYQ 2026-11-03 修复 e.tb.cn 短链解析不到商品ID
 */
public final class TbkShortLinkResolver {

    private static final Logger log = LoggerFactory.getLogger(TbkShortLinkResolver.class);

    private static final String UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    /** 响应体里可能出现的商品页地址（e.tb.cn 的 var url、meta refresh、a 标签等） */
    private static final Pattern PAGE_URL = Pattern.compile("https?://[^\\s\"'<>\\\\]+", Pattern.CASE_INSENSITIVE);

    /** 商品页地址特征，过滤掉统计/图片等无关链接 */
    private static final Pattern ITEM_PAGE = Pattern.compile(
            "(?:item\\.(?:taobao|tmall)\\.com|detail\\.tmall\\.com|chaoshi\\.detail\\.tmall\\.com"
                    + "|h5\\.m\\.taobao\\.com|main\\.m\\.taobao\\.com|a\\.m\\.taobao\\.com"
                    + "|market\\.m\\.taobao\\.com|uland\\.taobao\\.com)",
            Pattern.CASE_INSENSITIVE);

    /** JSON / JS 变量里的商品ID */
    private static final Pattern ITEM_ID_FIELD = Pattern.compile(
            "(?:itemId|item_id|num_iid|numIid)[\"']?\\s*[:=]\\s*[\"']?(\\d{6,20})",
            Pattern.CASE_INSENSITIVE);

    /** 老版手机商品页 a.m.taobao.com/i572209212242.htm */
    private static final Pattern MOBILE_ITEM_PATH = Pattern.compile("/i(\\d{6,20})\\.htm", Pattern.CASE_INSENSITIVE);

    /** 最大跳转次数，防止短链死循环 */
    private static final int MAX_REDIRECTS = 5;

    /** 响应体最大读取长度 */
    private static final int MAX_BODY_BYTES = 300_000;

    private TbkShortLinkResolver() {
    }

    /**
     * 解析短链得到商品ID，解析不到返回 null
     *
     * @param url 用户粘贴的分享短链（也兼容直接传商品链接）
     */
    public static String resolveItemId(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        String direct = TbkLinkParser.extractItemId(url);
        if (StringUtils.isNotEmpty(direct)) {
            return direct;
        }
        if (!url.startsWith("http")) {
            return null;
        }

        HttpURLConnection connection = null;
        try {
            String currentUrl = url;
            int redirects = 0;
            while (redirects++ < MAX_REDIRECTS) {
                connection = open(currentUrl);
                int status = connection.getResponseCode();
                String location = connection.getHeaderField("Location");
                if (isRedirect(status) && StringUtils.isNotEmpty(location)) {
                    currentUrl = absolutize(currentUrl, location);
                    String id = TbkLinkParser.extractItemId(currentUrl);
                    if (StringUtils.isNotEmpty(id)) {
                        return id;
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

            // 最终地址里可能已经带 id（HttpURLConnection 自动跟随的情况）
            String id = TbkLinkParser.extractItemId(connection.getURL().toString());
            if (StringUtils.isNotEmpty(id)) {
                return id;
            }

            // e.tb.cn 这类 200 的落地页，只能读 HTML 找真实地址
            String html = readBody(connection);
            return extractItemIdFromHtml(html);
        } catch (Exception e) {
            log.warn("短链解析失败 url={}: {}", url, e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 从短链落地页 HTML 中提取商品ID（拆出来便于单测）
     */
    public static String extractItemIdFromHtml(String html) {
        if (StringUtils.isEmpty(html)) {
            return null;
        }
        Matcher pageMatcher = PAGE_URL.matcher(html);
        while (pageMatcher.find()) {
            String candidate = pageMatcher.group();
            if (!ITEM_PAGE.matcher(candidate).find()) {
                continue;
            }
            String id = TbkLinkParser.extractItemId(candidate);
            if (StringUtils.isNotEmpty(id)) {
                return id;
            }
            Matcher mobile = MOBILE_ITEM_PATH.matcher(candidate);
            if (mobile.find()) {
                return mobile.group(1);
            }
        }
        Matcher fieldMatcher = ITEM_ID_FIELD.matcher(html);
        if (fieldMatcher.find()) {
            return fieldMatcher.group(1);
        }
        return null;
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        // 手动处理跳转，方便逐跳检查 Location 里的商品ID
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
            // 商品ID都是 ASCII，用 UTF-8 解码不会丢信息
            return out.toString(StandardCharsets.UTF_8.name());
        } finally {
            in.close();
        }
    }
}

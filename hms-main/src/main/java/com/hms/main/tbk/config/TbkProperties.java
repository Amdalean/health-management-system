package com.hms.main.tbk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import com.hms.common.utils.StringUtils;
import lombok.Data;

/**
 * 淘宝客转链配置
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Data
@Component
@ConfigurationProperties(prefix = "tbk")
public class TbkProperties {

    /**
     * 转链通道：mock-演示模式（默认，无需密钥）；ztk-折淘客
     */
    private String provider = "mock";

    /**
     * 默认推广位 pid，格式 mm_xxx_xxx_xxx
     */
    private String pid;

    /**
     * 渠道关系ID，用于订单归因（可选）
     */
    private String relationId;

    /**
     * 淘宝客外部用户标记（可选）
     */
    private String externalId;

    /**
     * 单 IP 每分钟最大转换次数
     */
    private int rateLimitPerMinute = 20;

    /**
     * 转链结果缓存秒数
     */
    private int cacheSeconds = 1800;

    /**
     * 输入内容最大长度
     */
    private int maxContentLength = 1000;

    /**
     * AI 选品单 IP 每分钟最大请求次数（一次选品会调用两次大模型，限得比转链严）
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    private int pickRateLimitPerMinute = 3;

    /**
     * AI 选品每个平台搜索的商品条数
     */
    private int pickSearchSize = 20;

    /**
     * 单次选品参与 AI 打分的最大候选数（越多越慢、越贵）
     */
    private int pickMaxCandidates = 12;

    /**
     * 选品结果缓存秒数
     */
    private int pickCacheSeconds = 600;

    /**
     * 联盟订单同步：单页条数
     *
     * add by CYQ 2026-11-09 联盟收益 PC 看板
     */
    private int orderSyncPageSize = 100;

    /**
     * 订单同步单次时间窗口（分钟）
     *
     * 淘宝客订单查询要求时间段不超过 3 小时，这里默认 2 小时留余量。
     */
    private int orderSyncChunkMinutes = 120;

    /**
     * 淘宝订单数据源：dataoke-大淘客订单查询；taobao-淘宝联盟官方接口
     *
     * 官方接口 taobao.tbk.order.details.get 是邀请制，应用未拿到权限包时返回 code=11；
     * 大淘客订单查询用同一个推广位账号即可查到联盟订单（含官方转链产生的订单）。
     *
     * add by CYQ 2026-11-10 淘宝订单数据源改为大淘客
     */
    private String orderSyncTaobaoProvider = "dataoke";

    /**
     * 大淘客订单查询单次时间窗口（分钟）
     *
     * 大淘客订单接口要求单次时间跨度不超过 3 小时，这里默认 3 小时。
     *
     * add by CYQ 2026-11-10 淘宝订单数据源改为大淘客
     */
    private int orderSyncDataokeChunkMinutes = 180;

    /**
     * 拼多多订单同步单次时间窗口（分钟）
     *
     * 多多进宝要求时间跨度不超过 24 小时，这里默认 23 小时。
     */
    private int orderSyncPddChunkMinutes = 1380;

    /**
     * 定时任务每次回看分钟数（覆盖订单状态变化）
     */
    private int orderSyncLookbackMinutes = 170;

    /**
     * 手动补同步最大天数
     */
    private int orderSyncMaxDays = 7;

    /**
     * 单次同步最大页数（防止异常翻页）
     */
    private int orderSyncMaxPages = 50;

    /**
     * 返现比例（占佣金的百分比）
     *
     * 平台有服务费，默认返 90%，剩余 10% 作为平台服务费/成本。
     * 现阶段只记录应返现金额，不实际打款。
     *
     * add by CYQ 2026-11-09 链接台账与返现估算
     * modify by CYQ 2026-11-14 返现比例 100 → 90
     */
    private BigDecimal cashbackRate = new BigDecimal("90");

    /**
     * 当前生效的返现比例（占佣金百分比）
     *
     * 配置缺失或非法时回退 90%，避免算出 0 / 负数返利。
     * add by CYQ 2026-11-14 平台服务费，返现比例 90%
     */
    public BigDecimal currentCashbackRate() {
        if (cashbackRate == null || cashbackRate.compareTo(BigDecimal.ZERO) < 0) {
            return new BigDecimal("90");
        }
        return cashbackRate;
    }

    /**
     * 链接归因时间窗（天）：订单付款前 N 天内生成过同商品链接才参与匹配
     */
    private int attributionWindowDays = 7;

    /**
     * 折淘客通道配置（provider=ztk 时生效）
     */
    private Ztk ztk = new Ztk();

    /**
     * 淘宝联盟官方通道配置（provider=taobao 时生效）
     */
    private Taobao taobao = new Taobao();

    /**
     * 大淘客通道配置（provider=dataoke 时生效）
     *
     * add by CYQ 2026-11-03 淘宝客转链模块
     */
    private Dataoke dataoke = new Dataoke();

    /**
     * 拼多多通道配置（链接自动识别，不占用 provider）
     *
     * add by CYQ 2026-11-03 转链模块支持拼多多
     */
    private Pdd pdd = new Pdd();

    /**
     * 美团联盟配置（自助取链接口）
     *
     * add by CYQ 2026-11-05 接入美团联盟
     */
    private Meituan meituan = new Meituan();

    /**
     * 淘宝闪购联盟配置（本地生活 alsc 接口）
     *
     * add by CYQ 2026-11-05 接入淘宝闪购
     */
    private Alsc alsc = new Alsc();

    /**
     * 从 pid（mm_用户ID_网站ID_推广位ID）解析推广位ID（最后一段）
     *
     * 注意：pid 可能是 mm_xxx_xxx_xxx（4 段），老格式才是 mm_xxx_xxx（3 段），
     * 统一取最后一段，避免把 site_id 当 adzone_id 用。
     */
    public String resolveAdzoneId() {
        if (StringUtils.isNotEmpty(taobao.getAdzoneId())) {
            return taobao.getAdzoneId();
        }
        String[] parts = StringUtils.isEmpty(pid) ? new String[0] : pid.split("_");
        return parts.length >= 3 ? parts[parts.length - 1] : null;
    }

    /**
     * 从 pid（mm_用户ID_网站ID_推广位ID）解析网站ID（倒数第二段）
     */
    public String resolveSiteId() {
        if (StringUtils.isNotEmpty(taobao.getSiteId())) {
            return taobao.getSiteId();
        }
        String[] parts = StringUtils.isEmpty(pid) ? new String[0] : pid.split("_");
        return parts.length >= 3 ? parts[parts.length - 2] : null;
    }

    @Data
    public static class Taobao {

        private String appKey;

        private String appSecret;

        private String adzoneId;

        private String siteId;

        private String gateway = "https://eco.taobao.com/router/rest";

        private String oauthAuthorizeUrl = "https://oauth.taobao.com/authorize";

        private String oauthTokenUrl = "https://oauth.taobao.com/token";

        /**
         * OAuth 回调地址，必须与开放平台「安全中心-回调地址」完全一致
         */
        private String redirectUri;

        /**
         * 手工配置的 session（可选），未配置时使用 OAuth 授权得到的 access_token
         */
        private String session;
    }

    @Data
    public static class Ztk {

        private String baseUrl = "https://api.zhetaoke.com:10001/api/open_gaoyongzhuanlian_tkl.ashx";

        /**
         * 折淘客对接密钥
         */
        private String appkey;

        /**
         * 淘客账号授权ID
         */
        private String sid;

        /**
         * 淘客 PID，必须与授权账号一致
         */
        private String pid;
    }

    /**
     * 大淘客开放平台配置
     *
     * 签名规则：signRan = MD5("appKey={appKey}&timer={timer}&nonce={nonce}&key={appSecret}") 转大写。
     *
     * add by CYQ 2026-11-03 淘宝客转链模块
     */
    @Data
    public static class Dataoke {

        private String appKey;

        private String appSecret;

        /**
         * 接口网关
         */
        private String baseUrl = "https://openapi.dataoke.com/api";

        /**
         * 商品详情接口版本
         */
        private String detailVersion = "v1.2.3";

        /**
         * 高效转链接口版本
         */
        private String convertVersion = "v1.3.1";

        /**
         * 商品搜索接口版本
         */
        private String searchVersion = "v1.2.4";

        /**
         * 推广位 pid，未配置时使用 tbk.pid
         */
        private String pid;
    }

    /**
     * 拼多多开放平台（多多进宝）配置
     *
     * 注意：开放平台应用必须先绑定多多进宝账号（duoId），否则 ddk 接口统一返回 20001。
     *
     * add by CYQ 2026-11-03 转链模块支持拼多多
     */
    @Data
    public static class Pdd {

        private String clientId;

        private String clientSecret;

        /**
         * 多多进宝推广位ID，形如 123456_789012
         */
        private String pid;

        /**
         * 接口网关
         */
        private String gateway = "https://gw-api.pinduoduo.com/api/router";

        /**
         * 自定义参数（订单归因用，可选）
         */
        private String customParameters;

        /**
         * 是否启用按用户的 custom_parameters 归因
         *
         * 拼多多要求「pid + custom_parameters」每组都备案，未备案会返回 60001；
         * 返现功能上线前保持 false，开启前需要先实现每个用户的授权备案流程。
         *
         * add by CYQ 2026-11-09 订单用户归因（返现基础）
         */
        private boolean userAttributionEnabled = false;
    }

    /**
     * 美团联盟开放平台配置
     *
     * 签名：sign = MD5(secret + 按参数名升序拼接的 key1value1key2value2... + secret)，参数不含 sign。
     *
     * add by CYQ 2026-11-05 接入美团联盟
     */
    @Data
    public static class Meituan {

        /**
         * 媒体 Appkey（媒体管理里查看）
         */
        private String appkey;

        /**
         * 签名密钥
         */
        private String secret;

        /**
         * 推广位 SID
         */
        private String sid;

        /**
         * 默认活动ID：我要推广-活动总览第一列的数字
         *
         * 注意：33（外卖）/4（闪购）等旧活动已下线，必须填当前有效活动
         */
        private String actId;

        /**
         * 链接类型：1-H5 2-美团App deeplink 3-中间页唤起 4-美团小程序 8-优选小程序
         */
        private int linkType = 1;

        /**
         * 自助取链接口地址
         */
        private String gateway = "https://openapi.meituan.com/api/generateLink";
    }

    /**
     * 淘宝闪购联盟（本地生活 alsc）配置
     *
     * 注意：闪购用的是独立接口族 alibaba.alsc.union.* 和独立推广位，
     * 与淘宝客的 taobao.tbk.* / mm_xxx pid 完全分开。
     *
     * add by CYQ 2026-11-05 接入淘宝闪购
     */
    @Data
    public static class Alsc {

        /**
         * 闪购推广位 pid，格式 alsc_xxx_xxx_xxx
         */
        private String pid;

        /**
         * 默认活动ID，如 12628（淘宝闪购领红包）
         */
        private String activityId;

        /**
         * 三方会员ID（可选，用于按用户归因）
         */
        private String sid;
    }
}

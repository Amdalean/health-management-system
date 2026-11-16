package com.hms.main.tbk.client;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.config.TbkProperties;
import com.hms.main.tbk.domain.AlscActivityLink;

/**
 * 淘宝闪购联盟（本地生活 alsc）通道
 *
 * 接口：alibaba.alsc.union.eleme.promotion.officialactivity.get
 * 入参：query_request = {"pid":"alsc_xxx_xxx_xxx","activity_id":"12628","sid":"..."}
 * 出参：H5 长短链、微信小程序 path、淘口令、闪购口令等。
 *
 * 注意：这是独立接口族 + 独立 alsc 推广位，和淘宝客的 taobao.tbk.* / mm_ pid 不通用，
 * 需要单独申请「淘宝闪购联盟」权限包。
 *
 * add by CYQ 2026-11-05 接入淘宝闪购
 */
@Component
public class AlscTbkClient {

    private static final Logger log = LoggerFactory.getLogger(AlscTbkClient.class);

    private static final String METHOD_ACTIVITY = "alibaba.alsc.union.eleme.promotion.officialactivity.get";

    @Autowired
    private TbkProperties properties;

    @Autowired
    private TopClient topClient;

    public String providerName() {
        return "alsc";
    }

    public boolean isConfigured() {
        return StringUtils.isNotEmpty(properties.getAlsc().getPid());
    }

    /**
     * 官方活动转链（使用配置里的默认活动ID）
     */
    public AlscActivityLink activityLink() {
        return activityLink(null, null);
    }

    /**
     * 官方活动转链
     *
     * @param activityId 活动ID，为空时用 tbk.alsc.activity-id
     * @param sid        三方会员ID，为空时用 tbk.alsc.sid
     */
    public AlscActivityLink activityLink(String activityId, String sid) {
        TbkProperties.Alsc alsc = properties.getAlsc();
        if (!isConfigured()) {
            throw new ServiceException("尚未配置淘宝闪购推广位，请填写 tbk.alsc.pid（格式 alsc_xxx_xxx_xxx）");
        }
        String resolvedActivityId = StringUtils.isNotEmpty(activityId)
                ? activityId.trim() : alsc.getActivityId();
        if (StringUtils.isEmpty(resolvedActivityId)) {
            throw new ServiceException("尚未配置闪购活动ID，请填写 tbk.alsc.activity-id（如 12628）");
        }
        String resolvedSid = StringUtils.isNotEmpty(sid) ? sid : alsc.getSid();

        JSONObject query = new JSONObject();
        query.put("pid", alsc.getPid());
        query.put("activity_id", resolvedActivityId);
        if (StringUtils.isNotEmpty(resolvedSid)) {
            query.put("sid", resolvedSid);
        }
        query.put("include_watchword", true);
        query.put("include_image", true);
        query.put("include_qr_code", true);

        Map<String, String> biz = new LinkedHashMap<>();
        biz.put("query_request", query.toJSONString());

        JSONObject resp;
        try {
            resp = topClient.execute(METHOD_ACTIVITY, biz, null);
        } catch (TopApiException e) {
            if ("11".equals(e.getCode())) {
                throw new ServiceException("淘宝闪购接口权限未开通，请到淘宝开放平台申请「淘宝闪购联盟」权限包");
            }
            throw new ServiceException(StringUtils.isNotEmpty(e.getMessage())
                    ? e.getMessage() : "淘宝闪购接口调用失败");
        }

        JSONObject data = resp.getJSONObject("data");
        if (data == null) {
            throw new ServiceException("没有获取到闪购活动信息，请检查活动ID是否有效");
        }
        JSONObject link = data.getJSONObject("link");

        AlscActivityLink result = new AlscActivityLink();
        result.setActivityId(data.getString("id"));
        result.setTitle(data.getString("title"));
        result.setDescription(data.getString("description"));
        result.setPicture(data.getString("picture"));
        result.setStartTime(data.getLong("start_time"));
        result.setEndTime(data.getLong("end_time"));
        if (link != null) {
            result.setH5Url(link.getString("h5_url"));
            result.setH5ShortLink(link.getString("h5_short_link"));
            result.setElemeWord(link.getString("eleme_word"));
            result.setTaobaoScheme(link.getString("tb_scheme_url"));

            JSONObject h5 = link.getJSONObject("h5_promotion");
            if (h5 != null) {
                if (StringUtils.isEmpty(result.getH5Url())) {
                    result.setH5Url(h5.getString("h5_url"));
                }
                if (StringUtils.isEmpty(result.getH5ShortLink())) {
                    result.setH5ShortLink(h5.getString("short_link"));
                }
            }
            JSONObject wx = link.getJSONObject("wx_promotion");
            if (wx != null) {
                result.setWxAppId(wx.getString("wx_app_id"));
                result.setWxPath(wx.getString("wx_path"));
                result.setWxQrCode(wx.getString("wx_qr_code"));
            }
            JSONObject taobao = link.getJSONObject("taobao_promotion");
            if (taobao != null) {
                result.setTaobaoWord(taobao.getString("tb_watchword"));
                if (StringUtils.isEmpty(result.getTaobaoScheme())) {
                    result.setTaobaoScheme(taobao.getString("scheme_url"));
                }
            }
            JSONObject app = link.getJSONObject("app_promotion");
            if (app != null) {
                result.setDeepLink(app.getString("deep_link"));
                if (StringUtils.isEmpty(result.getElemeWord())) {
                    result.setElemeWord(app.getString("eleme_word"));
                }
            }
        }
        log.info("淘宝闪购活动转链成功 activityId={} title={}", resolvedActivityId, result.getTitle());
        return result;
    }

    /**
     * 当前生效的闪购配置
     */
    public Map<String, Object> configInfo() {
        TbkProperties.Alsc alsc = properties.getAlsc();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("configured", isConfigured());
        info.put("pid", alsc.getPid());
        info.put("activityId", alsc.getActivityId());
        info.put("sid", alsc.getSid());
        return info;
    }
}

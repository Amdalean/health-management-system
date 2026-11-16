package com.hms.quartz.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.hms.ai.factory.AIChatFactory;
import com.hms.common.annotation.Log;
import com.hms.common.enums.BusinessType;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.hms.CommonUtil;
import com.hms.common.utils.http.HttpUtils;
import com.hms.common.utils.spring.SpringUtils;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.system.service.ISysConfigService;
import com.hms.system.service.ISysUserService;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 天气推送基础任务 - 取数、定城市、调 AI 生成正文
 * <p>
 * 人设与视觉风格由 {@link com.hms.ai.strategy.AIResponseStrategy} 提供，本类只关心数据与流程，
 * 换风格时仅需调整 {@link #getStrategyType()}。
 *
 * add by CYQ 2026-04-29
 * update by Qwen 2026-10-26 类名去掉龙族人设（原 FragonRajaBaseTask），人设不再泄漏到调用方
 */
@Log4j2
public class WeatherBaseTask {

    /** 默认城市：350206，厦门市湖里区（无位置上报时回退） */
    private static final String DEFAULT_ADCODE = "350206";
    private static final String DEFAULT_CITY = "厦门市";
    private static final String DEFAULT_DISTRICT = "湖里区";
    /** 配置项：天气推送取哪个用户的位置（sys_user.current_*） */
    private static final String CONFIG_WEATHER_USER_ID = "weather.location.userId";
    /** 位置上报有效期（天），超过则回退默认城市 */
    private static final int LOCATION_VALID_DAYS = 30;

    private static CommonUtil util = new CommonUtil();
    protected StringBuffer msg = new StringBuffer();
    /** 本次任务的城市信息，由 resolveLocation() 解析后复用 */
    protected SysUser currentLocation = null;

    /**
     * 重置本次任务的上下文（每次任务执行前调用，避免单例 Bean 复用上次任务的城市与日志）
     * add by Qwen 2026-10-25
     */
    protected void resetTaskContext() {
        this.msg = new StringBuffer();
        this.currentLocation = null;
    }

    /**
     * 解析本次天气任务使用的城市
     * add by Qwen 2026-10-26 说明: 取配置用户 sys_user 上最近上报的城市，无上报/已过期则回退默认城市（厦门湖里区）
     */
    protected SysUser resolveLocation() {
        if (currentLocation != null) {
            return currentLocation;
        }
        try {
            ISysConfigService configService = SpringUtils.getBean(ISysConfigService.class);
            String userIdStr = configService.selectConfigByKey(CONFIG_WEATHER_USER_ID);
            if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                ISysUserService userService = SpringUtils.getBean(ISysUserService.class);
                SysUser user = userService.selectUserById(Long.parseLong(userIdStr.trim()));
                if (user != null && user.getCurrentAdcode() != null && !user.getCurrentAdcode().isEmpty()) {
                    if (isLocationFresh(user.getLocationTime())) {
                        currentLocation = user;
                        log.info("天气任务使用上报城市: {} adcode={} 上报时间={}",
                                locationLabel(user), user.getCurrentAdcode(), user.getLocationTime());
                        return currentLocation;
                    }
                    log.warn("用户 {} 的位置上报已超过 {} 天，回退默认城市", userIdStr, LOCATION_VALID_DAYS);
                } else {
                    log.info("用户 {} 尚未上报位置，天气推送使用默认城市", userIdStr);
                }
            } else {
                log.info("未配置 {}，天气推送使用默认城市", CONFIG_WEATHER_USER_ID);
            }
        } catch (Exception e) {
            log.warn("获取上报城市失败，回退默认城市: {}", e.getMessage());
        }
        SysUser fallback = new SysUser();
        fallback.setCurrentAdcode(DEFAULT_ADCODE);
        fallback.setCurrentCity(DEFAULT_CITY);
        fallback.setCurrentDistrict(DEFAULT_DISTRICT);
        currentLocation = fallback;
        log.info("天气任务使用默认城市: {}{}", DEFAULT_CITY, DEFAULT_DISTRICT);
        return currentLocation;
    }

    /** 位置上报是否仍在有效期内 */
    private boolean isLocationFresh(Date locationTime) {
        if (locationTime == null) {
            return false;
        }
        long days = (System.currentTimeMillis() - locationTime.getTime()) / (24L * 3600 * 1000);
        return days <= LOCATION_VALID_DAYS;
    }

    /** 城市名，如“厦门市”；为空时回退默认城市 */
    private String cityLabel(SysUser user) {
        if (user.getCurrentCity() != null && !user.getCurrentCity().isEmpty()) {
            return user.getCurrentCity();
        }
        return DEFAULT_CITY;
    }

    /** 展示用地点名，如“厦门市湖里区” */
    private String locationLabel(SysUser user) {
        String city = cityLabel(user);
        String district = user.getCurrentDistrict();
        return (district == null || district.isEmpty()) ? city : city + district;
    }

    /**
     * 获取明天的天气
     * add by Qwen 2026-10-25 说明: 城市改为动态 adcode，取移动端上报的城市
     * update by Qwen 2026-10-26 方法名 getGDAPI → getTomorrowWeather，并修正 output=JSOM 拼写
     * @throws Exception
     */
    protected String getTomorrowWeather() throws BaseException {
        //url为高德天气预报服务的url，详情请参阅：https://console.amap.com/dev/key/app
        //city 传 adcode（区县级），由移动端上报的城市决定，缺省 350206 厦门市湖里区
        SysUser location = resolveLocation();
        String url = "https://restapi.amap.com/v3/weather/weatherInfo?key=9073e847b0ab5658da7bd99b6de16f34&city="
                + location.getCurrentAdcode() + "&output=JSON&extensions=all";
        String msg = HttpUtils.sendGet(url);
        log.info("高德返回消息:{}",msg);
        Map map = util.initMap(msg);
        String infocode = util.initstr(map.get("infocode"));
        if(!"10000".equals(infocode)) {
            throw new BaseException("高德请求异常,"+msg);
        }
        JSONArray forecasts = (JSONArray) JSON.parse(util.initstr(map.get("forecasts")));
        Map info = util.initMap(forecasts.get(0));
        JSONArray casts = (JSONArray)JSON.parse(util.initstr(info.get("casts")));
        String tomorrow = util.initstr(casts.get(1));
        this.msg.append("\n天气取数城市：" + locationLabel(location) + "（adcode=" + location.getCurrentAdcode() + "）");
        this.msg.append("\n高德返回消息："+tomorrow);
        return tomorrow;
    }

    /** 本次任务使用的 AI 策略（人设与视觉风格由策略决定，调用方不感知） */
    protected AIChatFactory.StrategyType getStrategyType() {
        // update by CYQ 2026-11-09 天气播报改为龙族角色每日随机；回退新海诚天空风格改回 WEATHER_SKY 即可
        return AIChatFactory.StrategyType.DRAGON_RAJA_WEATHER;
    }

    /**
     * 调用 AI 生成邮件内容（HTML + 图片提示词）
     * add by Qwen 2026-10-25 说明: 城市改为参数化，与天气取数城市保持一致
     * update by Qwen 2026-10-26 人设、格式、配图提示词全部由策略负责，这里只喂数据；方法名由 getgptMsg 改为 buildMailContent
     */
    protected String buildMailContent(String weatherData) {
        String input = "地点：" + locationLabel(resolveLocation()) + "\n明日天气：" + weatherData;
        Map<String, Object> aiContext = new HashMap<>();
        aiContext.put("billId", "weather-" + DateUtils.getDate());
        String response = AIChatFactory.createAIChat(
                AIChatFactory.ServiceType.OPENCODE_GO, getStrategyType()).chat(input, aiContext);
        this.msg.append("\nAI返回消息：" + response);
        return response;
    }

    /**
     * 从AI回复中提取图片描述词
     * add by Qwen 2026-07-20 Agnes Image 天气配图集成
     */
    protected String extractImagePrompt(String response) {
        if (response == null) {
            return null;
        }
        int start = response.indexOf("[IMAGE_PROMPT]");
        int end = response.indexOf("[/IMAGE_PROMPT]");
        if (start >= 0 && end > start) {
            return response.substring(start + "[IMAGE_PROMPT]".length(), end).trim();
        }
        return null;
    }

    /**
     * 从AI回复中移除图片描述词标签及内容
     * add by Qwen 2026-07-20 Agnes Image 天气配图集成
     */
    protected String removeImagePromptTags(String response) {
        if (response == null) {
            return null;
        }
        int start = response.indexOf("[IMAGE_PROMPT]");
        if (start >= 0) {
            return response.substring(0, start).trim();
        }
        return response;
    }

    @Log(title = "转换md文件", businessType = BusinessType.OTHER)
    protected static String removeTags(String s) {
        String startTag = "```markdown";
        String endTag = "```";

        if (s != null && s.startsWith(startTag) && s.endsWith(endTag)) {
            int startIndex = startTag.length();
            int endIndex = s.length() - endTag.length();
            return s.substring(startIndex, endIndex);
        }
        String startTagHtml = "```html";
        String endTagHtml = "```";

        if (s != null && s.startsWith(startTagHtml) && s.endsWith(endTagHtml)) {
            int startIndex = startTagHtml.length();
            int endIndex = s.length() - endTagHtml.length();
            return s.substring(startIndex, endIndex);
        }
        String startTagOther = "```";
        String endTagOther = "```";

        if (s != null && s.startsWith(startTagOther) && s.endsWith(endTagOther)) {
            int startIndex = startTagOther.length();
            int endIndex = s.length() - endTagOther.length();
            return s.substring(startIndex, endIndex);
        }
        return s;
    }
}

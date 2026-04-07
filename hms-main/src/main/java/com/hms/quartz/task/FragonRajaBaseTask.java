package com.hms.quartz.task;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.hms.ai.factory.AIChatFactory;
import com.hms.common.annotation.Log;
import com.hms.common.enums.BusinessType;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.hms.CommonUtil;
import com.hms.common.utils.http.HttpUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Log4j2
public class FragonRajaBaseTask {

    private static CommonUtil util = new CommonUtil();
    protected StringBuffer msg = new StringBuffer();
    /**
     * 获取明天的天气
     * @throws Exception
     */
    protected String getGDAPI() throws BaseException {
        //url为高德天气预报服务的url，详情请参阅：https://console.amap.com/dev/key/app
        //350206,厦门市湖里区
        String url = "https://restapi.amap.com/v3/weather/weatherInfo?key=9073e847b0ab5658da7bd99b6de16f34&city=350206&output=JSOM&extensions=all";
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
        this.msg.append("\n高德返回消息："+tomorrow);
        return tomorrow;
    }

    protected String getgptMsg(String data) throws BaseException {
        String now = DateUtil.now();
        String req = "请讲述天气及注意事项，并问好。干员将在厦门市湖里区出勤,现在的时间是"+now+"，以下是明天的天气信息："+data;
//        String msg = new Chat().getBody(req);
        //调用本地AI
//        String response = AIChatFactory.createAIChat(AIChatFactory.ServiceType.OLLAMA,AIChatFactory.StrategyType.DragonRaja).chat(req, null);
        //调用云AI
        String response = AIChatFactory.createAIChat(AIChatFactory.StrategyType.DragonRaja).chat(req, null);
        this.msg.append("\nqwen返回消息："+response);
//        String msg="";
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

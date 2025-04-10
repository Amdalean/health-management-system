package com.hms.quartz.task;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.hms.ai.qwen.Chat;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.hms.CommonUtil;
import com.hms.common.utils.hms.EmailUtil;
import com.hms.common.utils.hms.MDUtil;
import com.hms.common.utils.hms.UtilConstants;
//import com.hms.ollama.Chat;
//import com.hms.util.HttpConnection;
import com.hms.common.utils.http.HttpUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("SendMaillTask")
public class SendMaillTask {

    private static CommonUtil util = new CommonUtil();
    public void task() throws Exception {
        String gd = getGDAPI();//获取明天天气
        String gptbody = getgptMsg(gd);//通过qwen2获取信息
        String html = MDUtil.mdtohtml(gptbody);
        //发送邮件
        EmailUtil.sendEmail(UtilConstants.MaillSender.we, "明日天气预报，请查收~", html);
    }
    /**
     * 获取明天的天气
     * @throws Exception
     */
    private String getGDAPI() throws BaseException {
        //url为高德天气预报服务的url，详情请参阅：https://console.amap.com/dev/key/app
        //350206,厦门市湖里区
        String url = "https://restapi.amap.com/v3/weather/weatherInfo?key=9073e847b0ab5658da7bd99b6de16f34&city=350206&output=JSOM&extensions=all";
        String msg = HttpUtils.sendGet(url);
        System.out.println("高德返回消息："+msg);
        Map map = util.initMap(msg);
        String infocode = util.initstr(map.get("infocode"));
        if(!"10000".equals(infocode)) {
            throw new BaseException("高德请求异常,"+msg);
        }
        JSONArray forecasts = (JSONArray) JSON.parse(util.initstr(map.get("forecasts")));
        Map info = util.initMap(forecasts.get(0));
        JSONArray casts = (JSONArray)JSON.parse(util.initstr(info.get("casts")));
        String tomorrow = util.initstr(casts.get(1));
        return tomorrow;
    }
    private String getgptMsg(String data) throws BaseException {
        String now = DateUtil.now();
        String req = "请讲述天气及注意事项，并问好。生成内容请遵循markdown语法。现在的时间是"+now+"，以下是明天的天气信息："+data;
        String msg = new Chat().getBody(req);

        return msg;
    }
}

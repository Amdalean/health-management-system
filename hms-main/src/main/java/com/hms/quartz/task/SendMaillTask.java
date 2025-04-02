//package com.hms.quartz.task;
//
//import com.alibaba.fastjson2.JSON;
//import com.alibaba.fastjson2.JSONArray;
//import com.hms.common.utils.hms.CommonUtil;
//import com.hms.common.utils.hms.EmailUtil;
//import com.hms.common.utils.hms.MDUtil;
//import com.hms.common.utils.hms.UtilConstants;
//import com.hms.ollama.Chat;
//import com.hms.util.HttpConnection;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
//@Component("SendMaillTask")
//public class SendMaillTask {
//
//    private static CommonUtil util = new CommonUtil();
//    public void task() throws Exception {
//        String gd = getGDAPI();//获取明天天气
//        String gptbody = getgptMsg(gd);//通过qwen2获取信息
//        String html = MDUtil.mdtohtml(gptbody);
//        //发送邮件
//        EmailUtil.sendEmail(UtilConstants.MaillSender.we, "明日天气预报，请查收~", html);
//    }
//    /**
//     * 获取明天的天气
//     * @throws Exception
//     */
//    private String getGDAPI() throws Exception {
//        //url为高德天气预报服务的url，详情请参阅：https://console.amap.com/dev/key/app
//        String url = "https://restapi.amap.com/v3/weather/weatherInfo?key=9073e847b0ab5658da7bd99b6de16f34&city=110114&output=JSOM&extensions=all";
////        new HttpConnection();
//        String msg = HttpConnection.sendGET(url);
//        System.out.println("高德返回消息："+msg);
//        Map map = util.initMap(msg);
//        String infocode = util.initstr(map.get("infocode"));
//        if(!"10000".equals(infocode)) {
//            throw new Exception("高德请求异常,"+msg);
//        }
//        JSONArray forecasts = (JSONArray) JSON.parse(util.initstr(map.get("forecasts")));
//        Map info = util.initMap(forecasts.get(0));
//        JSONArray casts = (JSONArray)JSON.parse(util.initstr(info.get("casts")));
//        String tomorrow = util.initstr(casts.get(1));
//        return tomorrow;
//    }
//    private String getgptMsg(String data) throws Exception {
//        String msg = Chat.generate("收信人昵称:肉崽崽;我的昵称:崔崔;收信人身份:女朋友;请用中文讲述天气及注意事项，并问好。生成内容请遵循markdown语法。以下是明天的天气信息："+data);
//        return msg;
//    }
//}

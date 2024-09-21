package com.hms.task;

import com.hms.ollama.Chat;
import org.springframework.stereotype.Component;

@Component("SendMaillTask")
public class SendMaillTask {
    public void task(){

    }
    /**
     * 获取明天的天气
     * @throws BusinessException
     */
//    private String getGDAPI() throws BusinessException {
//        //url为高德天气预报服务的url，详情请参阅：https://console.amap.com/dev/key/app
//        String url = util.getParameter("gd_url");
//        String msg = HttpConnection.doGet(url, null, null);
//        Logger.error("高德返回消息："+msg);
//        Map map = util.initMap(msg);
//        String infocode = util.initstr(map.get("infocode"));
//        if(!"10000".equals(infocode)) {
//            throw new BusinessException("高德请求异常,"+msg);
//        }
//        JSONArray forecasts = (JSONArray)JSON.parse(util.initstr(map.get("forecasts")));
//        Map info = util.initMap(forecasts.get(0));
//        JSONArray casts = (JSONArray)JSON.parse(util.initstr(info.get("casts")));
//        String tomorrow = util.initstr(casts.get(1));
//        return tomorrow;
//    }
//    private String getgptMsg(){
//        Chat.generate("我的昵称:崔崔;收信人昵称:肉崽崽;收信人身份:女朋友;请用中文讲述天气及注意事项，并问好。生成内容请遵循markdown语法。今天是"+new UFDate()+"，以下是明天的天气信息："+data);
//    }
}

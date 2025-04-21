package com.hms.quartz.task;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
//import com.hms.ai.qwen.Chat;
import com.hms.ai.qwen.Chat;
import com.hms.common.annotation.Log;
import com.hms.common.config.MailConfig;
import com.hms.common.enums.BusinessType;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.hms.CommonUtil;
import com.hms.common.utils.hms.MDUtil;
import com.hms.common.utils.http.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Component("SendMaillTask")
public class SendMaillTask {

    @Autowired
    private JavaMailSender mailSender;

    private static final Logger log = LoggerFactory.getLogger(SendMaillTask.class);
    private StringBuffer msg = new StringBuffer();
    private static CommonUtil util = new CommonUtil();
    public String task() throws Exception {
        try {
            msg = new StringBuffer();
//            msg = new StringBuffer();
            String gd = getGDAPI();//获取明天天气
            String gptbody = getgptMsg(gd);//通过qwen2获取信息
            String md = removeMarkdownTags(gptbody);
            String html = MDUtil.mdtohtml(md);
            log.info("html正文：{}",html);
            //发送邮件
//            EmailUtil.sendEmail(MailConfig.getWe(), "明日天气预报，请查收~", "test");
//            EmailUtil.sendEmail(MailConfig.getWe(), "明日天气预报，请查收~", html);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper simpleMailMessage = new MimeMessageHelper(message, true);
            simpleMailMessage.setFrom(MailConfig.getMe());
            simpleMailMessage.setTo(MailConfig.getWe().split(","));
            simpleMailMessage.setSubject("明日天气预报，请查收~");
            simpleMailMessage.setText(html,true);
            mailSender.send(message);
            return msg.toString();
        }catch (Exception e){
            throw new Exception(e);
        }
    }
    /** 用于测试腾讯云对25端口的拦截 */
    public String taskTest() throws Exception {
        try {
            msg = new StringBuffer();
//            msg = new StringBuffer();
            String gd = getGDAPI();//获取明天天气
            String gptbody = getgptMsg(gd);//通过qwen2获取信息
            String md = removeMarkdownTags(gptbody);
            String html = MDUtil.mdtohtml(md);
            log.info("html正文：{}",html);
            //发送邮件
//            EmailUtil.sendEmail(MailConfig.getWe(), "明日天气预报，请查收~", "test");
//            EmailUtil.sendEmail(MailConfig.getWe(), "明日天气预报，请查收~", html);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper simpleMailMessage = new MimeMessageHelper(message, true);
            simpleMailMessage.setFrom(MailConfig.getMe());
            simpleMailMessage.setTo(MailConfig.getMe().split(","));
            simpleMailMessage.setSubject("明日天气预报，请查收~");
            simpleMailMessage.setText(html,true);
            mailSender.send(message);
            return msg.toString();
        } catch (BaseException e) {
            throw new RuntimeException(e);
        }
    }

    @Log(title = "转换md文件", businessType = BusinessType.OTHER)
    public static String removeMarkdownTags(String s) {
        String startTag = "```markdown";
        String endTag = "```";

        if (s != null && s.startsWith(startTag) && s.endsWith(endTag)) {
            int startIndex = startTag.length();
            int endIndex = s.length() - endTag.length();
            return s.substring(startIndex, endIndex);
        }
        return s;
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
    private String getgptMsg(String data) throws BaseException {
        String now = DateUtil.now();
        String req = "请讲述天气及注意事项，并问好。干员将在厦门市湖里区出勤,现在的时间是"+now+"，以下是明天的天气信息："+data;
        String msg = new Chat().getBody(req);
        this.msg.append("\nqwen返回消息："+msg);
//        String msg="";
        return msg;
    }
}

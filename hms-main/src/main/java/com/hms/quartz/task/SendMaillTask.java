package com.hms.quartz.task;

import com.hms.common.config.MailConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;

@Component("SendMaillTask")
@Log4j2
public class SendMaillTask extends FragonRajaBaseTask{

    @Autowired
    private JavaMailSender mailSender;

    public String task() throws Exception {
        return task(MailConfig.getWe());
    }

    public String task(String to) throws Exception {
        try {
            msg = new StringBuffer();
            String gd = getGDAPI();//获取明天天气
            String gptbody = getgptMsg(gd);//通过qwen获取信息
            String md = removeTags(gptbody);
//            String html = MDUtil.mdtohtml(md);
            String html = md;
            log.info("html正文：{}",html);
            //发送邮件
//            EmailUtil.sendEmail(MailConfig.getWe(), "明日天气预报，请查收~", "test");
//            EmailUtil.sendEmail(MailConfig.getWe(), "明日天气预报，请查收~", html);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper simpleMailMessage = new MimeMessageHelper(message, true);
            simpleMailMessage.setFrom(MailConfig.getMe());
            simpleMailMessage.setTo(to.split(","));
            simpleMailMessage.setSubject("明日天气预报，请查收~");
            simpleMailMessage.setText(html,true);
            mailSender.send(message);
            return msg.toString();
        }catch (Exception e){
            throw new Exception(e);
        }
    }
}

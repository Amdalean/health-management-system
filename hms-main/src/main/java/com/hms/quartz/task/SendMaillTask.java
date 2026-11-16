package com.hms.quartz.task;

import com.hms.common.config.MailConfig;
import com.hms.common.utils.DateUtils;
import com.hms.main.agnes.service.AgnesImageService;
import com.hms.main.integration.email.EmailIntegrationStrategy;
import com.hms.main.integration.email.EmailSendRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("SendMaillTask")
@Log4j2
public class SendMaillTask extends WeatherBaseTask{

    @Autowired
    private EmailIntegrationStrategy emailIntegrationStrategy;

    @Autowired
    private AgnesImageService agnesImageService;

    public String task() throws Exception {
        return task(MailConfig.getWeatherRecipients());
    }

    public String task(String to) throws Exception {
        try {
            resetTaskContext();
            String tomorrowWeather = getTomorrowWeather();//获取明天天气
            String mailContent = buildMailContent(tomorrowWeather);//通过 AI 策略生成正文（含图片描述词）

            //add by Qwen 2026-07-20 提取图片描述词并生成天气配图
            String imagePrompt = extractImagePrompt(mailContent);
            String html = removeTags(removeImagePromptTags(mailContent));

            // 生成天气配图
            String imageUrl = null;
            if (imagePrompt != null && !imagePrompt.isEmpty()) {
                try {
                    log.info("开始生成天气配图，prompt: {}", imagePrompt);
                    imageUrl = agnesImageService.generateImage(imagePrompt, "1K", "16:9");
                    log.info("天气配图生成成功: {}", imageUrl);
                    this.msg.append("\n配图URL：" + imageUrl);
                } catch (Exception e) {
                    log.warn("天气配图生成失败，邮件将不含图片: {}", e.getMessage());
                    this.msg.append("\n配图生成失败：" + e.getMessage());
                }
            } else {
                log.warn("未提取到图片描述词，跳过配图生成");
            }

            // 将配图占位符替换为真实URL
            if (imageUrl != null && !imageUrl.isEmpty()) {
                html = html.replace("IMAGE_PLACEHOLDER", imageUrl);
            } else {
                //配图失败时移除 Hero 图标签，避免邮件里出现裸占位符
                html = html.replaceAll("(?is)<img[^>]*IMAGE_PLACEHOLDER[^>]*>", "");
            }

            log.info("html正文：{}",html);
            //发送邮件（统一走邮件集成策略，自动记录集成日志）
            emailIntegrationStrategy.execute(new EmailSendRequest(
                to.split(","), "明日气象行动简报，请查收", html, "weather-" + DateUtils.getDate()));
            return msg.toString();
        }catch (Exception e){
            throw new Exception(e);
        }
    }
}

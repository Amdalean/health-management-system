package com.hms.main.integration.email;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import com.hms.common.annotation.IntegrationLog;
import com.hms.common.config.MailConfig;
import com.hms.common.integration.AbstractIntegrationStrategy;
import com.hms.common.utils.StringUtils;

/**
 * 邮件集成策略
 * <p>
 * 所有邮件发送统一走 {@link #execute(EmailSendRequest)}，由系统集成日志切面
 * 自动记录发送方、接收方、服务器标识、单据标识、报文、耗时等。
 * 类上标注 {@link IntegrationLog} 已自动注册为 Spring Bean，无需额外 {@code @Component}。
 *
 * add by CYQ 2026-09-14 系统集成日志框架-邮件接入
 */
@IntegrationLog(title = "邮件通知", bizType = "EMAIL", sender = "HMS")
/**
 * public class EmailIntegrationStrategy extends AbstractIntegrationStrategy<EmailSendRequest, EmailSendResponse>
 *
 * @author 崔永琦
 */
public class EmailIntegrationStrategy extends AbstractIntegrationStrategy<EmailSendRequest, EmailSendResponse>
{
    private static final Logger log = LoggerFactory.getLogger(EmailIntegrationStrategy.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    protected EmailSendResponse doSend(EmailSendRequest request) throws Exception
    {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(MailConfig.getMe());
        helper.setTo(request.getTos());
        helper.setSubject(request.getSubject());
        helper.setText(request.getContent(), true);
        mailSender.send(message);
        log.info("邮件发送成功，收件人：{}，主题：{}", StringUtils.join(request.getTos(), ","), request.getSubject());
        return new EmailSendResponse(true, "发送成功");
    }

    @Override
    public String resolveBillId(EmailSendRequest request)
    {
        return request.getBillId();
    }

    @Override
    public String resolveReceiver(EmailSendRequest request)
    {
        return StringUtils.join(request.getTos(), ",");
    }

    @Override
    public Object resolveSendMessage(EmailSendRequest request)
    {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("to", request.getTos());
        message.put("subject", request.getSubject());
        message.put("content", request.getContent());
        return message;
    }
}

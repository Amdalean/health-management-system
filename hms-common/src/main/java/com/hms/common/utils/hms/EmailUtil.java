package com.hms.common.utils.hms;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.hms.common.config.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮箱服务工具类
 */
public class EmailUtil {

    private static final Logger log = LoggerFactory.getLogger(EmailUtil.class);
	/**
    *
    * 配置发送基本参数
    * 发件人邮箱的SMTP服务器地址
    * 前三个不可更改
    *
    * */
    public static void sendEmail(String to, String subject, String body) throws Exception {
        log.info("正在向{}发送邮件，主题：{}，内容：{}",to,subject,body);
        String[] tos = to.split(",");
        main(tos,subject,body);

    }

   public static void main(String[] to,String subject, String body) throws Exception {
       //创建连接邮件服务器的参数配置
       Properties props = new Properties();// 参数配置
       props.setProperty("mail.smtp.host", "smtp.163.com");// 发件人的邮箱的 SMTP 服务器地址
       props.setProperty("mail.smtp.auth", "true");// 需要请求认证
       props.setProperty("mail.transport.protocol", "smtp");
//       props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
//       props.put("mail.smtp.ssl.protocols", "TLSv1.2");
//       props.setProperty("mail.smtp.port", "465");
//       props.put("mail.smtp.port", "465");
       //根据配置创建会话对象和邮件服务器交互
//       Session session = Session.getInstance(props);
//       session.setDebug(true);// 设置为debug模式, 可以查看详细的发送日志

       // 构建授权信息，用于进行SMTP进行身份验证
       Authenticator authenticator = new Authenticator() {
           protected PasswordAuthentication getPasswordAuthentication() {
               // 用户名、密码
               String userName = MailConfig.getMe();
               String password = MailConfig.getMailkey();
               return new PasswordAuthentication(userName, password);
           }
       };
       // 使用环境属性和授权信息，创建邮件会话
       Session session = Session.getInstance(props, authenticator);

       //创建邮件
       MimeMessage message = createEmail(session, to, subject, body);
       //使用Session获取邮件传输对象
       Transport transport = session.getTransport();
       //使用邮箱账号和密码连接邮件服务器
       transport.connect(MailConfig.getMe(), MailConfig.getMailkey());
       //发送邮件
       transport.sendMessage(message, message.getAllRecipients());
       //关闭连接
       transport.close();
   }

   /**
    *
    * 创建邮件
    *
    * */
   public static MimeMessage createEmail(Session session, String[] to,String subject, String body) throws Exception {
	   if(to==null||to.length==0) {
		   throw new Exception("未获取到有效的收件人");
	   }
       //创建一封邮件
       MimeMessage message = new MimeMessage(session);
       //发件人
       message.setFrom(new InternetAddress(MailConfig.getMe(), MailConfig.getMe(), "UTF-8"));
       //收件人
//       message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to, to, "UTF-8"));
       InternetAddress[] address= new InternetAddress[to.length];
       for(int i=0;i<to.length;i++) {
    	   address[i] = new InternetAddress(to[i], to[i], "UTF-8");
       }
       message.addRecipients(MimeMessage.RecipientType.TO, address);
       //邮件主题
       message.setSubject(subject, "UTF-8");
       //邮件正文
       message.setContent(body, "text/html;charset=UTF-8");
       //设置发件时间
       message.setSentDate(new Date());
       //保存设置
       message.saveChanges();
       return message;
   }
}

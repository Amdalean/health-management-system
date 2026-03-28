package com.hms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取邮件相关配置
 * @author CYQ
 */
@Component
@ConfigurationProperties(prefix = "mail")
public class MailConfig {
    private static String we;
    private static String me;
    private static String mailkey;

    public static String getWe() {
        return we;
    }

    public void setWe(String we) {
        MailConfig.we = we;
    }

    public static String getMe() {
        return me;
    }

    public void setMe(String me) {
        MailConfig.me = me;
    }

    public static String getMailkey() {
        return mailkey;
    }

    public void setMailkey(String mailkey) {
        MailConfig.mailkey = mailkey;
    }
}

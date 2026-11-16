package com.hms.main.integration.email;

import java.util.Arrays;

/**
 * 邮件发送请求
 *
 * add by CYQ 2026-09-14 系统集成日志框架-邮件接入
 */
public class EmailSendRequest
{
    /** 收件人 */
    private String[] tos;

    /** 邮件主题 */
    private String subject;

    /** 邮件正文（HTML） */
    private String content;

    /** 单据标识 */
    private String billId;

    public EmailSendRequest()
    {
    }

    public EmailSendRequest(String[] tos, String subject, String content)
    {
        this(tos, subject, content, null);
    }

    public EmailSendRequest(String[] tos, String subject, String content, String billId)
    {
        this.tos = tos;
        this.subject = subject;
        this.content = content;
        this.billId = billId;
    }

    public String[] getTos()
    {
        return tos;
    }

    public void setTos(String[] tos)
    {
        this.tos = tos;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getBillId()
    {
        return billId;
    }

    public void setBillId(String billId)
    {
        this.billId = billId;
    }

    @Override
    public String toString()
    {
        return "EmailSendRequest{tos=" + Arrays.toString(tos) + ", subject='" + subject + "', billId='" + billId + "'}";
    }
}

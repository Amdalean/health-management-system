package com.hms.main.integration.email;

/**
 * 邮件发送响应
 *
 * add by CYQ 2026-09-14 系统集成日志框架-邮件接入
 */
public class EmailSendResponse
{
    /** 是否成功 */
    private boolean success;

    /** 结果描述 */
    private String message;

    public EmailSendResponse()
    {
    }

    public EmailSendResponse(boolean success, String message)
    {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}

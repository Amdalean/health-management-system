package com.hms.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hms.common.annotation.Excel;
import com.hms.common.annotation.Excel.ColumnType;
import com.hms.common.core.domain.BaseEntity;

/**
 * 系统集成日志对象 hms_integration_log
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 */
public class HmsIntegrationLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键 */
    @Excel(name = "日志序号", cellType = ColumnType.NUMERIC)
    private Long logId;

    /** 集成节点名称 */
    @Excel(name = "集成节点")
    private String title;

    /** 业务类型 / 渠道 */
    @Excel(name = "业务类型")
    private String bizType;

    /** 发送方 */
    @Excel(name = "发送方")
    private String sender;

    /** 接收方 */
    @Excel(name = "接收方")
    private String receiver;

    /** 服务器标识 */
    @Excel(name = "服务器标识")
    private String serverId;

    /** 单据标识 */
    @Excel(name = "单据标识")
    private String billId;

    /** 发送报文 */
    @Excel(name = "发送报文")
    private String sendMessage;

    /** 响应报文 */
    @Excel(name = "响应报文")
    private String responseMessage;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 耗时（毫秒） */
    @Excel(name = "耗时", suffix = "毫秒")
    private Long costTime;

    /** 状态（0成功 1失败） */
    @Excel(name = "状态", readConverterExp = "0=成功,1=失败")
    private Integer status;

    /** 异常信息 */
    @Excel(name = "异常信息")
    private String errorMsg;

    /** 执行方法 */
    @Excel(name = "执行方法")
    private String method;

    /** 操作人 */
    @Excel(name = "操作人")
    private String operName;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public String getSender()
    {
        return sender;
    }

    public void setSender(String sender)
    {
        this.sender = sender;
    }

    public String getReceiver()
    {
        return receiver;
    }

    public void setReceiver(String receiver)
    {
        this.receiver = receiver;
    }

    public String getServerId()
    {
        return serverId;
    }

    public void setServerId(String serverId)
    {
        this.serverId = serverId;
    }

    public String getBillId()
    {
        return billId;
    }

    public void setBillId(String billId)
    {
        this.billId = billId;
    }

    public String getSendMessage()
    {
        return sendMessage;
    }

    public void setSendMessage(String sendMessage)
    {
        this.sendMessage = sendMessage;
    }

    public String getResponseMessage()
    {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage)
    {
        this.responseMessage = responseMessage;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public Long getCostTime()
    {
        return costTime;
    }

    public void setCostTime(Long costTime)
    {
        this.costTime = costTime;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public String getOperName()
    {
        return operName;
    }

    public void setOperName(String operName)
    {
        this.operName = operName;
    }
}

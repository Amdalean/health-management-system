package com.hms.main.reminder.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 提醒管理对象 hms_reminder
 * 
 * @author CYQ
 * @date 2026-01-26
 */
public class HmsReminder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 提醒ID */
    private Long id;

    /** 提醒标题 */
    @Excel(name = "提醒标题")
    private String title;

    /** 提醒日期 */
    @Excel(name = "提醒日期")
    private String remindDate;

    /** 历法类型（0公历 1农历） */
    @Excel(name = "历法类型", readConverterExp = "0=公历,1=农历")
    private String calendarType;

    /** 提醒邮箱 */
    @Excel(name = "提醒邮箱")
    private String remindEmail;

    /** AI提示词 */
    @Excel(name = "AI提示词")
    private String aiPrompt;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }
    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }
    public void setRemindDate(String remindDate) 
    {
        this.remindDate = remindDate;
    }

    public String getRemindDate() 
    {
        return remindDate;
    }
    public void setCalendarType(String calendarType) 
    {
        this.calendarType = calendarType;
    }

    public String getCalendarType() 
    {
        return calendarType;
    }
    public void setRemindEmail(String remindEmail) 
    {
        this.remindEmail = remindEmail;
    }

    public String getRemindEmail() 
    {
        return remindEmail;
    }
    public void setAiPrompt(String aiPrompt) 
    {
        this.aiPrompt = aiPrompt;
    }

    public String getAiPrompt() 
    {
        return aiPrompt;
    }
    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("remindDate", getRemindDate())
            .append("calendarType", getCalendarType())
            .append("remindEmail", getRemindEmail())
            .append("aiPrompt", getAiPrompt())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

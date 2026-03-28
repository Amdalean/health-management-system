package com.hms.main.itfdoc.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 接口档案对象 bd_itfdoc
 * 
 * @author CYQ
 * @date 2025-04-08
 */
public class BdItfdoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 接口档案主键 */
    private Long pkItfdoc;

    /** 接口编码 */
    @Excel(name = "接口编码")
    private String code;

    /** 接口名称 */
    @Excel(name = "接口名称")
    private String name;

    /** 插件全类名 */
    @Excel(name = "插件全类名")
    private String runclass;

    /** 发送方 */
    @Excel(name = "发送方")
    private String sender;

    /** 接收方 */
    @Excel(name = "接收方")
    private String receive;

    /** 备注 */
    @Excel(name = "备注")
    private String memo;

    /** 启用状态 */
    @Excel(name = "启用状态")
    private Long enablestate;

    /** 日志级别 */
    @Excel(name = "日志级别")
    private Long loglevel;

    /** 启用警告通知 */
    @Excel(name = "启用警告通知")
    private String warn;

    /** 加密级别 */
    @Excel(name = "加密级别")
    private Long enlevel;

    public void setPkItfdoc(Long pkItfdoc) 
    {
        this.pkItfdoc = pkItfdoc;
    }

    public Long getPkItfdoc() 
    {
        return pkItfdoc;
    }
    public void setCode(String code) 
    {
        this.code = code;
    }

    public String getCode() 
    {
        return code;
    }
    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }
    public void setRunclass(String runclass) 
    {
        this.runclass = runclass;
    }

    public String getRunclass() 
    {
        return runclass;
    }
    public void setSender(String sender) 
    {
        this.sender = sender;
    }

    public String getSender() 
    {
        return sender;
    }
    public void setReceive(String receive) 
    {
        this.receive = receive;
    }

    public String getReceive() 
    {
        return receive;
    }
    public void setMemo(String memo) 
    {
        this.memo = memo;
    }

    public String getMemo() 
    {
        return memo;
    }
    public void setEnablestate(Long enablestate) 
    {
        this.enablestate = enablestate;
    }

    public Long getEnablestate() 
    {
        return enablestate;
    }
    public void setLoglevel(Long loglevel) 
    {
        this.loglevel = loglevel;
    }

    public Long getLoglevel() 
    {
        return loglevel;
    }
    public void setWarn(String warn) 
    {
        this.warn = warn;
    }

    public String getWarn() 
    {
        return warn;
    }
    public void setEnlevel(Long enlevel) 
    {
        this.enlevel = enlevel;
    }

    public Long getEnlevel() 
    {
        return enlevel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("pkItfdoc", getPkItfdoc())
            .append("code", getCode())
            .append("name", getName())
            .append("runclass", getRunclass())
            .append("sender", getSender())
            .append("receive", getReceive())
            .append("memo", getMemo())
            .append("enablestate", getEnablestate())
            .append("loglevel", getLoglevel())
            .append("warn", getWarn())
            .append("enlevel", getEnlevel())
            .toString();
    }
}

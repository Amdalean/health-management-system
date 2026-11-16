package com.hms.main.timediary.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 时间管理局对象 hms_time_diary
 *
 * add by CYQ 2026年8月4日
 */
@Data
public class HsmTimeDiary extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long diaryId;

    /** 记录人用户ID */
    private Long userId;

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date diaryDate;

    /** 开始时间(HH:mm) */
    @Excel(name = "开始时间")
    private String startTime;

    /** 结束时间(HH:mm) */
    @Excel(name = "结束时间")
    private String endTime;

    /** 分类(工作/学习/生活/其他) */
    @Excel(name = "分类")
    private String category;

    /** 事项标题 */
    @Excel(name = "事项标题")
    private String title;

    /** 详细描述 */
    @Excel(name = "详细描述")
    private String description;

    /** 时长(分钟) */
    @Excel(name = "时长(分钟)")
    private Integer durationMin;
}

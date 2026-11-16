package com.hms.main.score.domain;

import java.math.BigDecimal;
import lombok.Data;
import com.hms.common.annotation.Excel;
import com.hms.common.core.domain.BaseEntity;

/**
 * 成绩智能分析对象 hms_score_record
 *
 * @author CYQ
 * @date 2026-03-01
 */
@Data
public class HmsScoreRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Long id;

    /** 课程 ID */
    @Excel(name = "课程 ID")
    private Long courseId;

    /** 学生 ID */
    @Excel(name = "学生 ID")
    private Long studentId;

    /** 文件路径 */
    private String filePath;

    /** 文件原始名称 */
    private String fileName;

    /** 文件大小 (字节) */
    private Long fileSize;

    /** 分析类型 */
    private String analysisType;

    /** AI 评分 */
    @Excel(name = "AI 评分")
    private BigDecimal score;

    /** 分析状态 */
    @Excel(name = "分析状态")
    private String status;

    /** AI 评语 */
    @Excel(name = "AI 评语")
    private String feedback;

    /** 分析详情 */
    @Excel(name = "分析详情")
    private String analysisDetails;

    /** 异步任务 ID */
    private String taskId;

    /** 学号（关联 hms_student，非持久化，用于列表展示） */
    private String studentNo;

    /** 学生姓名（关联 hms_student，非持久化，用于列表展示） */
    private String studentName;

    /** 课程名称（关联 hms_course，非持久化，用于列表展示） */
    private String courseName;

    /** 删除标志 */
    private String delFlag;

}

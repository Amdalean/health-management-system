package com.hms.main.tbk.domain;

import java.util.Date;
import lombok.Data;

/**
 * 转链流水（既是操作记录，也是人工对单依据）
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Data
public class TbkConvertLog {

    private Long id;

    /** 用户原始输入 */
    private String content;

    /** 商品ID */
    private String itemId;

    /** 商品标题 */
    private String title;

    /** 转链通道 */
    private String provider;

    /** 请求IP */
    private String ip;

    /** 状态：0-成功 1-失败 */
    private String status;

    /** 失败原因 */
    private String errorMsg;

    private Date createTime;
}

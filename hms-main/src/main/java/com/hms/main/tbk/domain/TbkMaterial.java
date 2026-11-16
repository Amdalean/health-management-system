package com.hms.main.tbk.domain;

import lombok.Data;

/**
 * 从用户粘贴的文本中识别出的转链素材
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Data
public class TbkMaterial {

    /** 原始输入 */
    private String rawContent;

    /** 用于提交给聚合平台的素材：链接或淘口令 */
    private String material;

    /** 商品ID（能从链接里解析出来时才有值） */
    private String itemId;

    /** 素材类型：url / tkl / id */
    private String type;
}

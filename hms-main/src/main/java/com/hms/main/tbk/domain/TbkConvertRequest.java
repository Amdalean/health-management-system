package com.hms.main.tbk.domain;

import lombok.Data;

/**
 * 转链请求
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Data
public class TbkConvertRequest {

    /** 用户粘贴的淘宝链接 / 淘口令 / 商品ID */
    private String content;

    /** 匿名用户标识（登录态下忽略，后端从 token 解析用户ID） */
    private String userKey;
}

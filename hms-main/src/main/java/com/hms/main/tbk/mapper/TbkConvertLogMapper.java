package com.hms.main.tbk.mapper;

import com.hms.main.tbk.domain.TbkConvertLog;

/**
 * 淘宝客转链流水Mapper
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
public interface TbkConvertLogMapper {

    /**
     * 新增转链流水
     */
    int insertTbkConvertLog(TbkConvertLog log);
}

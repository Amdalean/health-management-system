package com.hms.system.mapper;

import java.util.List;
import com.hms.system.domain.HmsIntegrationLog;

/**
 * 系统集成日志 数据层
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 */
public interface HmsIntegrationLogMapper
{
    /**
     * 新增集成日志
     */
    public int insertHmsIntegrationLog(HmsIntegrationLog integrationLog);

    /**
     * 查询集成日志列表
     */
    public List<HmsIntegrationLog> selectHmsIntegrationLogList(HmsIntegrationLog integrationLog);

    /**
     * 查询集成日志详细
     */
    public HmsIntegrationLog selectHmsIntegrationLogById(Long logId);

    /**
     * 批量删除集成日志
     */
    public int deleteHmsIntegrationLogByIds(Long[] logIds);

    /**
     * 清空集成日志
     */
    public int cleanHmsIntegrationLog();
}

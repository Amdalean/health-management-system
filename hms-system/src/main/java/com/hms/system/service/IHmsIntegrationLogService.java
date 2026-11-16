package com.hms.system.service;

import java.util.List;
import com.hms.system.domain.HmsIntegrationLog;

/**
 * 系统集成日志 服务层
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 */
public interface IHmsIntegrationLogService
{
    /**
     * 新增集成日志
     */
    public void insertHmsIntegrationLog(HmsIntegrationLog integrationLog);

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

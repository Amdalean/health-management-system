package com.hms.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.system.domain.HmsIntegrationLog;
import com.hms.system.mapper.HmsIntegrationLogMapper;
import com.hms.system.service.IHmsIntegrationLogService;

/**
 * 系统集成日志 服务层实现
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 */
@Service
public class HmsIntegrationLogServiceImpl implements IHmsIntegrationLogService
{
    @Autowired
    private HmsIntegrationLogMapper integrationLogMapper;

    @Override
    public void insertHmsIntegrationLog(HmsIntegrationLog integrationLog)
    {
        integrationLogMapper.insertHmsIntegrationLog(integrationLog);
    }

    @Override
    public List<HmsIntegrationLog> selectHmsIntegrationLogList(HmsIntegrationLog integrationLog)
    {
        return integrationLogMapper.selectHmsIntegrationLogList(integrationLog);
    }

    @Override
    public HmsIntegrationLog selectHmsIntegrationLogById(Long logId)
    {
        return integrationLogMapper.selectHmsIntegrationLogById(logId);
    }

    @Override
    public int deleteHmsIntegrationLogByIds(Long[] logIds)
    {
        return integrationLogMapper.deleteHmsIntegrationLogByIds(logIds);
    }

    @Override
    public int cleanHmsIntegrationLog()
    {
        return integrationLogMapper.cleanHmsIntegrationLog();
    }
}

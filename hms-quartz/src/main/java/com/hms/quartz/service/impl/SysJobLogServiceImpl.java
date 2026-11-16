package com.hms.quartz.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.quartz.domain.SysJobLog;
import com.hms.quartz.mapper.SysJobLogMapper;
import com.hms.quartz.service.ISysJobLogService;

/**
 * 定时任务调度日志信息 服务层
 * 
 * @author ruoyi
 */
@Service
public class SysJobLogServiceImpl implements ISysJobLogService
{
    @Autowired
    private SysJobLogMapper jobLogMapper;

    /**
     * 获取quartz调度器日志的计划任务
     * 
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog)
    {
        List<SysJobLog> list = jobLogMapper.selectJobLogList(jobLog);
        // add by CYQ 2026-09-09 回填耗时，前端直接展示秒数
        for (SysJobLog log : list)
        {
            fillRunMs(log);
        }
        return list;
    }

    /**
     * 通过调度任务日志ID查询调度信息
     * 
     * @param jobLogId 调度任务日志ID
     * @return 调度任务日志对象信息
     */
    @Override
    public SysJobLog selectJobLogById(Long jobLogId)
    {
        SysJobLog log = jobLogMapper.selectJobLogById(jobLogId);
        fillRunMs(log);
        return log;
    }

    /**
     * 回填耗时（毫秒）= 结束时间 - 开始时间
     * add by CYQ 2026-09-09 调度日志增加结束时间与耗时展示
     */
    private void fillRunMs(SysJobLog log)
    {
        if (log != null && log.getStartTime() != null && log.getStopTime() != null)
        {
            log.setRunMs(log.getStopTime().getTime() - log.getStartTime().getTime());
        }
    }

    /**
     * 新增任务日志
     *
     * @param jobLog 调度日志信息
     */
    @Override
    public void addJobLog(SysJobLog jobLog)
    {
        jobLogMapper.insertJobLog(jobLog);
    }

    /**
     * 更新任务日志
     * add by CYQ 2026-08-19
     */
    @Override
    public void updateJobLog(SysJobLog jobLog)
    {
        jobLogMapper.updateJobLog(jobLog);
    }

    /**
     * 批量删除调度日志信息
     * 
     * @param logIds 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteJobLogByIds(Long[] logIds)
    {
        return jobLogMapper.deleteJobLogByIds(logIds);
    }

    /**
     * 删除任务日志
     * 
     * @param jobId 调度日志ID
     */
    @Override
    public int deleteJobLogById(Long jobId)
    {
        return jobLogMapper.deleteJobLogById(jobId);
    }

    /**
     * 清空任务日志
     */
    @Override
    public void cleanJobLog()
    {
        jobLogMapper.cleanJobLog();
    }
}

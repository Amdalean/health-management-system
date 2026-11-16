package com.hms.quartz.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hms.common.constant.CacheConstants;
import com.hms.common.constant.Constants;
import com.hms.common.constant.ScheduleConstants;
import com.hms.common.core.redis.DistributedLockTemplate;
import com.hms.common.utils.ExceptionUtil;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.bean.BeanUtils;
import com.hms.common.utils.hms.ServerUtils;
import com.hms.common.utils.spring.SpringUtils;
import com.hms.quartz.domain.SysJob;
import com.hms.quartz.domain.SysJobLog;
import com.hms.quartz.service.ISysJobLogService;

/**
 * 抽象quartz调用
 * Edit by CYQ 2025年4月10日 成功了也允许返回报文
 * @author ruoyi
 */
public abstract class AbstractQuartzJob implements Job
{
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /** 禁止并发标志值（sys_job.concurrent=1） add by CYQ 2026-09-22 */
    private static final String LOCK_ENABLED_VALUE = "1";

    /** 分布式锁过期秒数，长任务由看门狗自动续期 add by CYQ 2026-09-22 */
    private static final long LOCK_EXPIRE_SECONDS = 300L;

    /** 未获取到分布式锁时的跳过提示 add by CYQ 2026-09-22 */
    private static final String SKIP_MESSAGE = "任务正在执行中，本次跳过（分布式锁未获取到）";

    /**
     * 线程本地变量：开始时间
     */
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    /**
     * 线程本地变量：运行中日志ID
     * add by CYQ 2026-08-19
     */
    private static ThreadLocal<Long> logIdLocal = new ThreadLocal<>();

    /**
     * 当前执行服务器标识
     * add by CYQ 2026-09-14 调度日志记录执行任务的所属服务器
     */
    private static final String SERVER_NAME = ServerUtils.getServerName();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        SysJob sysJob = new SysJob();
        BeanUtils.copyBeanProp(sysJob, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));
        try
        {
            before(context, sysJob);
            String msg = null;
            if (sysJob != null)
            {
                msg = invokeWithConcurrentGuard(context, sysJob);
            }
            after(context, sysJob, null,msg);
        }
        catch (Exception e)
        {
            log.error("任务执行异常  - ：", e);
            after(context, sysJob, e,null);
        }
    }

    /**
     * 高层统一并发控制：由 {@code sys_job.concurrent} 决定是否启用分布式锁。
     * <p>
     * 禁止并发(1) → 获取 Redis 分布式锁（跨实例防重复执行，长任务由看门狗自动续期）；
     * 允许并发(0)或未配置 → 不加锁直接执行。
     * <p>
     * 任务方法上无需再标注任何锁注解。
     * add by CYQ 2026-09-22 合并并发控制与分布式锁
     */
    private String invokeWithConcurrentGuard(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        if (!LOCK_ENABLED_VALUE.equals(sysJob.getConcurrent()))
        {
            return doExecute(context, sysJob);
        }
        DistributedLockTemplate lockTemplate = SpringUtils.getBean(DistributedLockTemplate.class);
        String lockKey = buildLockKey(sysJob);
        return lockTemplate.execute(lockKey, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS, 0L,
                SKIP_MESSAGE, () -> doExecute(context, sysJob));
    }

    /**
     * 分布式锁 key：统一前缀 + invokeTarget（同一执行目标的任务互斥）；invokeTarget 为空回退 jobId
     * add by CYQ 2026-09-22
     */
    private String buildLockKey(SysJob sysJob)
    {
        String target = StringUtils.isNotEmpty(sysJob.getInvokeTarget())
                ? sysJob.getInvokeTarget()
                : String.valueOf(sysJob.getJobId());
        return CacheConstants.LOCK_KEY + "quartz:" + target;
    }

    /**
     * 执行前：立即插入一条状态=运行中的日志记录
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     */
    protected void before(JobExecutionContext context, SysJob sysJob)
    {
        Date startTime = new Date();
        threadLocal.set(startTime);

        // 立即写入一条"运行中"日志，这样调度日志页面能实时看到任务在执行
        SysJobLog runningLog = new SysJobLog();
        runningLog.setJobName(sysJob.getJobName());
        runningLog.setJobGroup(sysJob.getJobGroup());
        runningLog.setInvokeTarget(sysJob.getInvokeTarget());
        runningLog.setServerName(SERVER_NAME);
        runningLog.setStartTime(startTime);
        runningLog.setStopTime(startTime);
        runningLog.setStatus(Constants.RUNNING);
        runningLog.setJobMessage(removeEmoji(sysJob.getJobName() == null ? "未知任务" : sysJob.getJobName()) + " 执行中...");
        try
        {
            ISysJobLogService logService = SpringUtils.getBean(ISysJobLogService.class);
            logService.addJobLog(runningLog);
            logIdLocal.set(runningLog.getJobLogId());
        }
        catch (Exception ex)
        {
            log.warn("写入运行中日志失败", ex);
        }
    }

    /**
     * 执行后：更新之前插入的运行中日志为最终状态
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e, String msg)
    {
        Date startTime = threadLocal.get();
        threadLocal.remove();
        Long logId = logIdLocal.get();
        logIdLocal.remove();

        Date stopTime = new Date();
        long runMs = stopTime.getTime() - startTime.getTime();

        msg = msg == null ? "" : msg;
        String jobName = sysJob.getJobName() == null ? "未知任务" : sysJob.getJobName();

        // 过滤 Emoji 表情字符（4字节 UTF-8），避免 MySQL utf8 编码问题
        String filteredMsg = removeEmoji(msg);
        String filteredJobName = removeEmoji(jobName);

        SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobMessage(filteredJobName + " 总共耗时：" + runMs + "毫秒\n" + filteredMsg);
        if (e != null)
        {
            sysJobLog.setStatus(Constants.FAIL);
            String errorMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(removeEmoji(errorMsg));
        }
        else
        {
            sysJobLog.setStatus(Constants.SUCCESS);
        }

        // 防止页面 HTML 等大文本撑爆 job_message 和 exception_info
        if (sysJobLog.getJobMessage() != null && sysJobLog.getJobMessage().length() > 2000) {
            sysJobLog.setJobMessage(sysJobLog.getJobMessage().substring(0, 2000) + "...(truncated)");
        }
        if (sysJobLog.getExceptionInfo() != null && sysJobLog.getExceptionInfo().length() > 2000) {
            sysJobLog.setExceptionInfo(sysJobLog.getExceptionInfo().substring(0, 2000) + "...(truncated)");
        }

        sysJobLog.setStopTime(stopTime);

        ISysJobLogService logService = SpringUtils.getBean(ISysJobLogService.class);
        if (logId != null)
        {
            // 更新之前插入的运行中日志
            sysJobLog.setJobLogId(logId);
            logService.updateJobLog(sysJobLog);
        }
        else
        {
            // 降级：如果 before() 插入失败，则新建一条完整日志
            sysJobLog.setJobName(filteredJobName);
            sysJobLog.setJobGroup(sysJob.getJobGroup());
            sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
            sysJobLog.setServerName(SERVER_NAME);
            sysJobLog.setStartTime(startTime);
            logService.addJobLog(sysJobLog);
        }
    }

    /**
     * 移除 Emoji 表情字符（4字节 UTF-8）
     * 解决 MySQL utf8 编码不支持 Emoji 的问题
     * 
     * add by CYQ 2026年4月6日
     * @param text 原始文本
     * @return 过滤后的文本
     */
    private String removeEmoji(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 移除 4 字节 UTF-8 字符（包括 Emoji 表情）
        return text.replaceAll("[\\x{10000}-\\x{10FFFF}]", "");
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected abstract String doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}

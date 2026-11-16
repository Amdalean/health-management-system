package com.hms.common.core.redis;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hms.common.utils.spring.SpringUtils;

/**
 * 分布式锁执行模板。
 *
 * <p>高层统一入口：调用方无需在方法上标注注解，直接调用
 * {@link #execute(String, long, TimeUnit, long, Object, LockedAction)} 即可获得
 * "获取锁 → 看门狗自动续期 → 执行业务 → 释放锁" 的完整能力。
 *
 * <p>典型使用场景为 Quartz 调度层：由 {@code sys_job.concurrent} 决定是否加锁，
 * 禁止并发(1)时加锁、允许并发(0)时不加锁。
 *
 * add by CYQ 2026-09-22 后台任务通用分布式锁
 */
@Component
public class DistributedLockTemplate
{
    private static final Logger log = LoggerFactory.getLogger(DistributedLockTemplate.class);

    /** 获取锁失败时的轮询间隔（毫秒） */
    private static final long WAIT_INTERVAL_MS = 100L;

    /** 看门狗续期使用的调度器 bean 名称（定义于 ThreadPoolConfig） */
    private static final String SCHEDULER_BEAN = "scheduledExecutorService";

    @Autowired
    private RedisLock redisLock;

    /**
     * 加锁执行。未获取到锁时不执行 action，直接返回 skipValue。
     *
     * @param key       锁 key
     * @param expire    锁过期时间，必须大于 0，防止任务异常后死锁
     * @param unit      过期时间单位
     * @param waitTime  尝试获取锁的等待时长（单位同 unit），0 表示不等待
     * @param skipValue 未获取到锁时的返回值
     * @param action    加锁成功后执行的业务逻辑
     */
    public <T> T execute(String key, long expire, TimeUnit unit, long waitTime, T skipValue, LockedAction<T> action)
            throws Exception
    {
        String token = acquire(key, expire, unit, waitTime);
        if (token == null)
        {
            log.warn("未获取到分布式锁，跳过执行: key={}", key);
            return skipValue;
        }

        ScheduledFuture<?> renewFuture = startWatchdog(key, token, expire, unit);
        try
        {
            return action.run();
        }
        finally
        {
            if (renewFuture != null)
            {
                renewFuture.cancel(false);
            }
            try
            {
                redisLock.releaseLock(key, token);
            }
            catch (Exception e)
            {
                log.warn("释放分布式锁异常, key={}", key, e);
            }
        }
    }

    /**
     * 获取锁，支持按 waitTime 轮询等待
     *
     * @return 持有者 token，未获取到返回 null
     */
    private String acquire(String key, long expire, TimeUnit unit, long waitTime)
    {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        while (true)
        {
            String token = redisLock.tryLock(key, expire, unit);
            if (token != null)
            {
                return token;
            }
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0)
            {
                return null;
            }
            try
            {
                Thread.sleep(Math.min(WAIT_INTERVAL_MS, remaining));
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    /**
     * 启动看门狗，按 expire/3 周期自动续期，防止长任务执行中锁提前失效
     */
    private ScheduledFuture<?> startWatchdog(String key, String token, long expire, TimeUnit unit)
    {
        if (expire <= 0)
        {
            return null;
        }
        ScheduledExecutorService scheduler = resolveScheduler();
        if (scheduler == null)
        {
            log.warn("未找到 {}，跳过分布式锁看门狗续期, key={}", SCHEDULER_BEAN, key);
            return null;
        }
        long periodMs = Math.max(unit.toMillis(expire) / 3, 1000L);
        try
        {
            return scheduler.scheduleAtFixedRate(() ->
            {
                try
                {
                    redisLock.renewLock(key, token, expire, unit);
                }
                catch (Exception e)
                {
                    log.warn("分布式锁自动续期异常, key={}", key, e);
                }
            }, periodMs, periodMs, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
            log.warn("启动分布式锁看门狗失败, key={}", key, e);
            return null;
        }
    }

    /**
     * 解析看门狗调度器，缺失时返回 null（不影响加锁本身）
     */
    private ScheduledExecutorService resolveScheduler()
    {
        try
        {
            Object bean = SpringUtils.getBean(SCHEDULER_BEAN);
            return bean instanceof ScheduledExecutorService ? (ScheduledExecutorService) bean : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 加锁后执行的业务逻辑
     */
    @FunctionalInterface
    public interface LockedAction<T>
    {
        T run() throws Exception;
    }
}

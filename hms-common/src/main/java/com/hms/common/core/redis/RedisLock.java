package com.hms.common.core.redis;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的通用分布式锁工具类。
 *
 * <p>核心特性：
 * <ul>
 *   <li>加锁使用 SET key value NX EX 原子操作，避免死锁；</li>
 *   <li>锁持有者为随机 token，解锁使用 Lua 脚本比对 token 后删除，避免误删他人锁；</li>
 *   <li>提供续期能力，长耗时任务可配合看门狗自动续期。</li>
 * </ul>
 *
 * <p>本类使用 {@link StringRedisTemplate}，避免项目默认 RedisTemplate（FastJson 值序列化）
 * 导致 token 与 Lua 脚本比较不一致的问题。
 *
 * add by CYQ 2026-09-22 后台任务通用分布式锁
 */
@Component
public class RedisLock
{
    private static final Logger log = LoggerFactory.getLogger(RedisLock.class);

    /**
     * 解锁脚本：仅当 key 存在且 value 等于持有者 token 时才删除
     */
    private static final String RELEASE_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * 续期脚本：仅当 key 存在且 value 等于持有者 token 时才续期（单位：秒）
     */
    private static final String RENEW_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end";

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(RELEASE_LUA, Long.class);

    private static final DefaultRedisScript<Long> RENEW_SCRIPT =
            new DefaultRedisScript<>(RENEW_LUA, Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试加锁（自动生成持有者 token）
     *
     * @param key    锁的 key
     * @param expire 过期时间，必须大于 0，避免任务异常后死锁
     * @param unit   时间单位
     * @return 加锁成功返回持有者 token（解锁时原样传回），失败返回 null
     */
    public String tryLock(String key, long expire, TimeUnit unit)
    {
        String token = UUID.randomUUID().toString().replace("-", "");
        return tryLock(key, token, expire, unit) ? token : null;
    }

    /**
     * 尝试加锁（指定持有者 token）
     *
     * @param key    锁的 key
     * @param token  持有者标识
     * @param expire 过期时间，必须大于 0
     * @param unit   时间单位
     * @return true 加锁成功；false 锁已被其他持有者占用
     */
    public boolean tryLock(String key, String token, long expire, TimeUnit unit)
    {
        if (expire <= 0)
        {
            throw new IllegalArgumentException("分布式锁 expire 必须大于 0，否则可能死锁");
        }
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, token, expire, unit);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 释放锁（Lua 脚本原子校验 token 后删除，避免误删他人锁）
     *
     * @param key   锁的 key
     * @param token 加锁时返回的持有者 token
     * @return true 释放成功；false 锁已不属于当前持有者或已过期
     */
    public boolean releaseLock(String key, String token)
    {
        if (key == null || token == null)
        {
            return false;
        }
        Long result = stringRedisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), token);
        return result != null && result > 0;
    }

    /**
     * 续期锁（Lua 脚本原子校验 token 后重置过期时间，避免长任务执行中锁提前失效）
     *
     * @param key    锁的 key
     * @param token  加锁时返回的持有者 token
     * @param expire 新的过期时间
     * @param unit   时间单位
     * @return true 续期成功；false 锁已不属于当前持有者或已过期
     */
    public boolean renewLock(String key, String token, long expire, TimeUnit unit)
    {
        if (key == null || token == null)
        {
            return false;
        }
        long seconds = unit.toSeconds(expire);
        if (seconds <= 0)
        {
            seconds = 1;
        }
        try
        {
            Long result = stringRedisTemplate.execute(RENEW_SCRIPT, Collections.singletonList(key),
                    token, String.valueOf(seconds));
            return result != null && result > 0;
        }
        catch (Exception e)
        {
            log.warn("分布式锁续期异常, key={}", key, e);
            return false;
        }
    }

    /**
     * 判断锁是否被占用
     *
     * @param key 锁的 key
     * @return true 已被占用；false 空闲
     */
    public boolean isLocked(String key)
    {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}

package com.hms.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 淘宝客转链结果 cache key
     * add by CYQ 2026-11-04 缓存监控可见
     */
    public static final String TBK_CONVERT_KEY = "tbk:convert:";

    /**
     * 淘宝客 AI 选品结果 cache key
     * add by CYQ 2026-11-04 缓存监控可见
     */
    public static final String TBK_PICK_KEY = "tbk:pick:";

    /**
     * 淘宝授权 token cache key（单 key，无尾冒号）
     * add by CYQ 2026-11-04 缓存监控可见
     */
    public static final String TBK_TAOBAO_TOKEN_KEY = "tbk:taobao:token";

    /**
     * 用户名称 cache key
     * add by CYQ 2026-11-04 缓存监控可见
     */
    public static final String USER_NAME_KEY = "user:id:";

    /**
     * 分布式锁 redis key
     * add by CYQ 2026-09-22
     */
    public static final String LOCK_KEY = "lock:";
}

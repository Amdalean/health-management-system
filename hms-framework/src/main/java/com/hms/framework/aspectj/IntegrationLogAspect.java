package com.hms.framework.aspectj;

import java.lang.reflect.Method;
import java.util.Date;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.hms.common.annotation.IntegrationLog;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.enums.BusinessStatus;
import com.hms.common.integration.IntegrationStrategy;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.hms.ServerUtils;
import com.hms.framework.manager.AsyncManager;
import com.hms.framework.manager.factory.AsyncFactory;
import com.hms.system.domain.HmsIntegrationLog;

/**
 * 系统集成日志记录处理
 * <p>
 * 拦截所有标注 {@link IntegrationLog} 的类（或方法），自动采集
 * 发送方、接收方、服务器标识、单据标识、发送报文、响应报文、开始时间、结束时间、耗时并异步落库。
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 */
@Aspect
@Component
public class IntegrationLogAspect
{
    private static final Logger log = LoggerFactory.getLogger(IntegrationLogAspect.class);

    /** 报文最大保存长度 */
    private static final int MAX_CONTENT_LENGTH = 65535;

    /** 默认操作人 */
    private static final String SYSTEM_OPER = "system";

    @Around("@within(com.hms.common.annotation.IntegrationLog) || @annotation(com.hms.common.annotation.IntegrationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Class<?> targetClass = target.getClass();

        // 元数据以类注解为准；仅方法标注时退化为方法注解
        IntegrationLog annotation = targetClass.getAnnotation(IntegrationLog.class);
        if (annotation == null)
        {
            annotation = method.getAnnotation(IntegrationLog.class);
        }
        if (annotation == null)
        {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        Object request = (args != null && args.length > 0) ? args[0] : null;
        @SuppressWarnings("rawtypes")
        IntegrationStrategy strategy = (target instanceof IntegrationStrategy) ? (IntegrationStrategy) target : null;

        HmsIntegrationLog integrationLog = new HmsIntegrationLog();
        integrationLog.setTitle(annotation.title());
        integrationLog.setBizType(annotation.bizType());
        integrationLog.setSender(annotation.sender());
        integrationLog.setReceiver(annotation.receiver());
        integrationLog.setServerId(StringUtils.isNotEmpty(annotation.serverId()) ? annotation.serverId() : ServerUtils.getServerName());
        integrationLog.setMethod(targetClass.getName() + "." + method.getName() + "()");
        integrationLog.setOperName(resolveOperName());

        // 采集单据标识、接收方、发送报文
        if (strategy != null)
        {
            Object billId = safeResolve(() -> strategy.resolveBillId(request));
            if (billId != null)
            {
                integrationLog.setBillId(String.valueOf(billId));
            }
            String receiver = safeResolve(() -> strategy.resolveReceiver(request));
            if (StringUtils.isNotEmpty(receiver))
            {
                integrationLog.setReceiver(receiver);
            }
        }
        if (annotation.isSaveSendMessage())
        {
            Object sendMessage = strategy != null ? safeResolve(() -> strategy.resolveSendMessage(request)) : request;
            if (sendMessage == null && strategy != null)
            {
                sendMessage = request;
            }
            integrationLog.setSendMessage(toJsonString(sendMessage));
        }

        Date startTime = new Date();
        long startMillis = System.currentTimeMillis();
        integrationLog.setStartTime(startTime);

        try
        {
            Object result = joinPoint.proceed();
            if (annotation.isSaveResponseMessage())
            {
                Object responseMessage = strategy != null ? safeResolve(() -> strategy.resolveResponseMessage(result)) : result;
                integrationLog.setResponseMessage(toJsonString(responseMessage));
            }
            integrationLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            return result;
        }
        catch (Throwable e)
        {
            integrationLog.setStatus(BusinessStatus.FAIL.ordinal());
            integrationLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            throw e;
        }
        finally
        {
            integrationLog.setEndTime(new Date());
            integrationLog.setCostTime(System.currentTimeMillis() - startMillis);
            saveLog(integrationLog);
        }
    }

    /**
     * 安全执行策略钩子，异常时返回 null，避免影响主流程
     */
    private <T> T safeResolve(ResolveSupplier<T> supplier)
    {
        try
        {
            return supplier.get();
        }
        catch (Exception e)
        {
            log.debug("集成日志字段解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前操作人，无登录上下文（定时任务）时返回 system
     */
    private String resolveOperName()
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null && StringUtils.isNotEmpty(loginUser.getUsername()))
            {
                return loginUser.getUsername();
            }
        }
        catch (Exception ignored)
        {
        }
        return SYSTEM_OPER;
    }

    /**
     * 对象转 JSON 字符串并截断
     */
    private String toJsonString(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            String json = (value instanceof String) ? (String) value : JSON.toJSONString(value);
            return StringUtils.substring(json, 0, MAX_CONTENT_LENGTH);
        }
        catch (Exception e)
        {
            return StringUtils.substring(String.valueOf(value), 0, MAX_CONTENT_LENGTH);
        }
    }

    /**
     * 异步保存集成日志
     */
    private void saveLog(HmsIntegrationLog integrationLog)
    {
        AsyncManager.me().execute(AsyncFactory.recordIntegrationLog(integrationLog));
    }

    @FunctionalInterface
    private interface ResolveSupplier<T>
    {
        T get() throws Exception;
    }
}

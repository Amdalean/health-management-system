package com.hms.common.integration;

import java.lang.reflect.Field;
import java.util.Map;
import com.hms.common.annotation.IntegrationLog;
import com.hms.common.utils.StringUtils;

/**
 * 系统集成策略抽象基类（模板方法 + 策略模式）
 * <p>
 * 子类只需实现 {@link #doSend(Object)} 完成真正的对外调用，
 * 日志采集、记录由切面统一处理，业务代码零侵入。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;IntegrationLog(title = "邮件通知", bizType = "EMAIL", sender = "HMS")
 * public class EmailIntegrationStrategy extends AbstractIntegrationStrategy&lt;EmailRequest, EmailResponse&gt;
 * {
 *     protected EmailResponse doSend(EmailRequest req) throws Exception { ... }
 * }
 * </pre>
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 *
 * @param <REQ> 请求参数类型
 * @param <RSP> 响应结果类型
 */
public abstract class AbstractIntegrationStrategy<REQ, RSP> implements IntegrationStrategy<REQ, RSP>
{
    /**
     * 单据标识候选字段名（按顺序查找）
     */
    private static final String[] BILL_ID_FIELDS = { "billId", "billNo", "documentId", "documentNo", "orderNo", "bizId", "id" };

    /**
     * 模板方法：固定集成调用流程，保证日志入口唯一（子类不应覆写）
     */
    @IntegrationLog
    @Override
    public RSP execute(REQ request) throws Exception
    {
        return doSend(request);
    }

    /**
     * 子类实现真正的对接逻辑
     */
    protected abstract RSP doSend(REQ request) throws Exception;

    /**
     * 默认从请求对象中反射提取单据标识，子类可覆写
     */
    @Override
    public String resolveBillId(REQ request)
    {
        if (request == null)
        {
            return null;
        }
        if (request instanceof Map)
        {
            return stringify(((Map<?, ?>) request).get("billId"));
        }
        for (String fieldName : BILL_ID_FIELDS)
        {
            Object value = readProperty(request, fieldName);
            if (value != null)
            {
                return stringify(value);
            }
        }
        return null;
    }

    /**
     * 默认不覆盖接收方
     */
    @Override
    public String resolveReceiver(REQ request)
    {
        return null;
    }

    /**
     * 默认发送报文为请求对象本身
     */
    @Override
    public Object resolveSendMessage(REQ request)
    {
        return request;
    }

    /**
     * 默认响应报文为响应对象本身
     */
    @Override
    public Object resolveResponseMessage(RSP response)
    {
        return response;
    }

    /**
     * 反射读取属性值（getter 优先，其次字段）
     */
    private Object readProperty(Object target, String name)
    {
        Class<?> clazz = target.getClass();
        try
        {
            String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            return clazz.getMethod(getter).invoke(target);
        }
        catch (Exception ignored)
        {
        }
        try
        {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    private String stringify(Object value)
    {
        return value == null ? null : (StringUtils.isEmpty(String.valueOf(value)) ? null : String.valueOf(value));
    }
}

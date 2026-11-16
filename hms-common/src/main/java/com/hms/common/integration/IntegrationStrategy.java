package com.hms.common.integration;

import com.hms.common.annotation.IntegrationLog;

/**
 * 系统集成策略接口（策略模式）
 * <p>
 * 每一个外部集成（邮件、HTTP 接口、消息队列等）实现为一个策略类，
 * 类上标注 {@link com.hms.common.annotation.IntegrationLog} 并继承
 * {@link AbstractIntegrationStrategy}，通过模板方法 {@link #execute(Object)} 调用。
 * <p>
 * 切面会拦截 {@link #execute(Object)}，并按下列钩子采集日志字段：
 * <ul>
 *     <li>{@link #resolveBillId(Object)} —— 单据标识</li>
 *     <li>{@link #resolveReceiver(Object)} —— 接收方（覆盖注解默认值）</li>
 *     <li>{@link #resolveSendMessage(Object)} —— 发送报文</li>
 *     <li>{@link #resolveResponseMessage(Object)} —— 响应报文</li>
 * </ul>
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 *
 * @param <REQ> 请求参数类型
 * @param <RSP> 响应结果类型
 */
public interface IntegrationStrategy<REQ, RSP>
{
    /**
     * 模版方法入口，外部统一通过此方法调用集成（切面在此采集日志）。
     * <p>
     * 方法上的 {@link IntegrationLog} 仅作为切面触发标记（兼容 JDK 动态代理），
     * 具体元数据以策略类上的注解为准。
     */
    @IntegrationLog
    RSP execute(REQ request) throws Exception;

    /**
     * 单据标识，如业务单号、消息 ID，默认取请求对象中的 billId 字段
     */
    String resolveBillId(REQ request);

    /**
     * 接收方，返回 null 表示使用注解上的默认值
     */
    String resolveReceiver(REQ request);

    /**
     * 发送报文内容，切面会序列化为字符串保存
     */
    Object resolveSendMessage(REQ request);

    /**
     * 响应报文内容，切面会序列化为字符串保存
     */
    Object resolveResponseMessage(RSP response);
}

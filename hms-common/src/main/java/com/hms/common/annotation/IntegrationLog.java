package com.hms.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * 系统集成日志注解
 * <p>
 * 本注解已通过元注解集成 {@link Component}，标注该注解的类会自动注册为 Spring Bean，
 * 无需再额外标注 {@code @Component}。切面会自动采集发送方、接收方、服务器标识、
 * 单据标识、发送报文、响应报文、开始时间、结束时间、耗时等信息并落库。
 * 推荐配合 {@link com.hms.common.integration.AbstractIntegrationStrategy} 使用。
 *
 * add by CYQ 2026-09-14 系统集成日志框架
 */
@Component
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IntegrationLog
{
    /**
     * 集成节点名称
     */
    String title() default "";

    /**
     * 业务类型 / 渠道，如 EMAIL、HTTP、MQ
     */
    String bizType() default "";

    /**
     * 发送方（系统或服务名）
     */
    String sender() default "";

    /**
     * 接收方，可在运行时通过策略 resolveReceiver 覆盖
     */
    String receiver() default "";

    /**
     * 服务器标识，为空时自动取本机 IP
     */
    String serverId() default "";

    /**
     * 是否保存发送报文
     */
    boolean isSaveSendMessage() default true;

    /**
     * 是否保存响应报文
     */
    boolean isSaveResponseMessage() default true;
}

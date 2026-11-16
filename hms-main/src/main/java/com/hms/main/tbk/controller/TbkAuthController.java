package com.hms.main.tbk.controller;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.hms.common.annotation.Anonymous;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.utils.StringUtils;
import com.hms.main.tbk.service.TaobaoAuthService;

/**
 * 淘宝账号授权入口
 *
 * taobao.tbk.privilege.get 需要 session，先访问 /tbk/auth/start 完成一次授权。
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Anonymous
@Controller
@RequestMapping("/tbk/auth")
public class TbkAuthController {

    private static final Logger log = LoggerFactory.getLogger(TbkAuthController.class);

    @Autowired
    private TaobaoAuthService authService;

    /**
     * 跳转淘宝授权页
     */
    @GetMapping("/start")
    public void start(HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.buildAuthorizeUrl());
    }

    /**
     * 授权回调：用 code 换 token
     */
    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String error,
                         @RequestParam(required = false) String error_description,
                         HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String html;
        try {
            if (StringUtils.isNotEmpty(error)) {
                throw new IllegalStateException(error_description == null ? error : error_description);
            }
            if (StringUtils.isEmpty(code)) {
                throw new IllegalStateException("没有收到授权码 code");
            }
            authService.exchangeCode(code);
            html = page("授权成功", "淘宝账号授权已完成，可以返回小程序继续使用。", true);
        } catch (Exception e) {
            log.warn("淘宝授权回调失败: {}", e.getMessage(), e);
            html = page("授权失败", StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : "未知错误", false);
        }
        response.getWriter().write(html);
    }

    /**
     * 授权状态（不返回 token 明文）
     */
    @GetMapping("/status")
    @ResponseBody
    public AjaxResult status() {
        return AjaxResult.success(authService.status());
    }

    private String page(String title, String message, boolean success) {
        String color = success ? "#16a34a" : "#dc2626";
        String safeTitle = escapeHtml(title);
        String safeMessage = escapeHtml(message);
        return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + safeTitle + "</title></head>"
                + "<body style=\"margin:0;background:#f5f6f8;font-family:-apple-system,'PingFang SC',sans-serif\">"
                + "<div style=\"max-width:420px;margin:80px auto;background:#fff;border-radius:16px;padding:32px 24px;text-align:center;box-shadow:0 2px 12px rgba(0,0,0,.06)\">"
                + "<div style=\"font-size:44px\">" + (success ? "✅" : "⚠️") + "</div>"
                + "<h2 style=\"color:" + color + ";margin:12px 0\">" + safeTitle + "</h2>"
                + "<p style=\"color:#5b6370;font-size:14px;line-height:1.7\">" + safeMessage + "</p>"
                + "</div></body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

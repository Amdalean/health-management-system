package com.hms.mobile.push.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.mobile.push.service.IUniPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 推送消息发送Controller
 * add by CYQ 2026-05-22
 * modify by CYQ 2026-06-08 修复推送结果判断
 */
@RestController
@RequestMapping("/mobile/push/message")
/**
 * public class PushMessageController extends BaseController {
 *
 * @author 崔永琦
 */
public class PushMessageController extends BaseController {

    @Autowired
    private IUniPushService uniPushService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 向指定设备发送推送
     */
    @PreAuthorize("@ss.hasPermi('mobile:pushDevice:send')")
    @PostMapping("/single")
    public AjaxResult pushToSingle(@RequestBody Map<String, String> params) {
        String clientId = params.get("clientId");
        String title = params.get("title");
        String body = params.get("body");

        if (clientId == null || clientId.isEmpty()) {
            return error("clientId不能为空");
        }
        if (title == null || title.isEmpty()) {
            return error("title不能为空");
        }
        if (body == null || body.isEmpty()) {
            return error("body不能为空");
        }

        String result = uniPushService.pushToSingle(clientId, title, body);
        return checkPushResult(result);
    }

    /**
     * 向所有设备发送推送
     */
    @PreAuthorize("@ss.hasPermi('mobile:pushDevice:send')")
    @PostMapping("/all")
    public AjaxResult pushToAll(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        String body = params.get("body");

        if (title == null || title.isEmpty()) {
            return error("title不能为空");
        }
        if (body == null || body.isEmpty()) {
            return error("body不能为空");
        }

        String result = uniPushService.pushToAll(title, body);
        return checkPushResult(result);
    }

    /**
     * 向指定用户的所有设备推送
     */
    @PreAuthorize("@ss.hasPermi('mobile:pushDevice:send')")
    @PostMapping("/user")
    public AjaxResult pushToUser(@RequestBody Map<String, String> params) {
        String userIdStr = params.get("userId");
        String title = params.get("title");
        String body = params.get("body");

        if (userIdStr == null || userIdStr.isEmpty()) {
            return error("userId不能为空");
        }
        if (title == null || title.isEmpty()) {
            return error("title不能为空");
        }
        if (body == null || body.isEmpty()) {
            return error("body不能为空");
        }

        Long userId = Long.valueOf(userIdStr);
        String result = uniPushService.pushToUser(userId, title, body);
        return checkPushResult(result);
    }

    /**
     * 检查推送结果：OpenAPI返回 code=0 才算成功
     */
    private AjaxResult checkPushResult(String resultJson) {
        try {
            JsonNode node = objectMapper.readTree(resultJson);
            if (node.has("code") && node.get("code").asInt() == 0) {
                return AjaxResult.success("推送成功");
            } else {
                String msg = node.has("msg") ? node.get("msg").asText() : "推送失败";
                return AjaxResult.error("推送失败: " + msg);
            }
        } catch (Exception e) {
            return AjaxResult.error("推送结果解析异常: " + e.getMessage());
        }
    }
}
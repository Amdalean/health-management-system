package com.hms.mobile.push.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.mobile.push.domain.PushDevice;
import com.hms.mobile.push.service.IPushDeviceService;
import com.hms.mobile.push.service.IWxMaSubscribeService;
import com.hms.system.service.ISysUserService;

/**
 * 微信小程序订阅消息Controller
 * add by CYQ 2026-06-10
 */
@RestController
@RequestMapping("/mobile/wx/subscribe")
/**
 * public class WxMaSubscribeController extends BaseController {
 *
 * @author 崔永琦
 */
public class WxMaSubscribeController extends BaseController {

    @Autowired
    private IWxMaSubscribeService wxMaSubscribeService;

    @Autowired
    private IPushDeviceService pushDeviceService;

    @Autowired
    private ISysUserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送订阅消息
     */
    @PreAuthorize("@ss.hasPermi('mobile:pushDevice:send')")
    @PostMapping("/send")
    public AjaxResult send(@RequestBody Map<String, Object> params) {
        String openId = (String) params.get("openId");
        String page = (String) params.get("page");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) params.get("data");

        if (openId == null || openId.isEmpty()) return error("openId不能为空");
        String result = wxMaSubscribeService.sendSubscribeMessage(openId, page, data);
        return parseWxResult(result);
    }

    /**
     * 发送打卡提醒
     */
    @PreAuthorize("@ss.hasPermi('mobile:pushDevice:send')")
    @PostMapping("/clockinRemind")
    public AjaxResult clockinRemind(@RequestBody Map<String, String> params) {
        String openId = params.get("openId");
        String name = params.getOrDefault("name", "");
        String date = params.getOrDefault("date", "");
        String status = params.getOrDefault("status", "已签到");

        if (openId == null || openId.isEmpty()) return error("openId不能为空");
        String result = wxMaSubscribeService.sendClockinRemind(openId, name, date, status);
        return parseWxResult(result);
    }

    /**
     * 小程序登录：用 wx.login() 返回的 code 换取 openid
     * add by CYQ 2026-06-10
     */
    @PostMapping("/code2session")
    public AjaxResult code2session(@RequestBody Map<String, String> params) {
        String code = params.get("code");
        if (code == null || code.isEmpty()) return error("code不能为空");

        Map<String, Object> result = wxMaSubscribeService.code2session(code);
        if (result.containsKey("openid")) {
            return success(result);
        } else {
            return error("换取openid失败: " + result.getOrDefault("errmsg", ""));
        }
    }

    /**
     * 上报绑定 openid（小程序首次打卡时调用）
     * 小程序端先 wx.login() 拿到 code，调 /code2session 换 openid，
     * 再调此接口将 openid 绑定到当前登录用户
     * add by CYQ 2026-06-10
     */
    @PostMapping("/bindOpenid")
    public AjaxResult bindOpenid(@RequestBody Map<String, String> params) {
        String openId = params.get("openid");
        if (openId == null || openId.isEmpty()) return error("openid不能为空");

        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            Long userId = loginUser.getUserId();

            // 先尝试更新已存在的记录
            PushDevice device = new PushDevice();
            device.setUserId(userId);
            device.setWxOpenid(openId);
            int rows = pushDeviceService.bindWxOpenid(device);

            // 如果没有更新到（用户还没有 PushDevice 记录），则插入
            if (rows == 0) {
                device.setPlatform("wx_miniapp");
                pushDeviceService.insertPushDevice(device);
            }

            // 同步一份到用户表（微信一键登录主查 sys_user.wx_openid）
            // add by CYQ 2026-09-06 openid 改记到用户表
            SysUser bindUser = new SysUser();
            bindUser.setUserId(userId);
            bindUser.setWxOpenid(openId);
            userService.updateUserProfile(bindUser);

            return success("绑定成功");
        } catch (Exception e) {
            return error("请先登录");
        }
    }

    private AjaxResult parseWxResult(String resultJson) {
        try {
            JsonNode node = objectMapper.readTree(resultJson);
            int errcode = node.has("errcode") ? node.get("errcode").asInt() : -1;
            if (errcode == 0) {
                return success("发送成功");
            } else {
                String errmsg = node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";
                return error("发送失败 [" + errcode + "]: " + errmsg);
            }
        } catch (Exception e) {
            return error("结果解析异常: " + e.getMessage());
        }
    }
}

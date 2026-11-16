package com.hms.main.tbk.controller;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.annotation.Anonymous;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.domain.model.LoginUser;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.ip.IpUtils;
import com.hms.main.tbk.client.MeituanTbkClient;
import com.hms.main.tbk.client.AlscTbkClient;
import com.hms.main.tbk.domain.TbkConvertRequest;
import com.hms.main.tbk.domain.TbkConvertResult;
import com.hms.main.tbk.domain.TbkPickRequest;
import com.hms.main.tbk.service.TbkConvertService;
import com.hms.main.tbk.service.TbkPickService;

/**
 * 淘宝客转链接口（用户自助 H5 使用，匿名访问）
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Anonymous
@RestController
@RequestMapping("/tbk/api")
public class TbkController extends BaseController {

    @Autowired
    private TbkConvertService convertService;

    @Autowired
    private TbkPickService pickService;

    @Autowired
    private com.hms.main.tbk.client.PddTbkClient pddClient;

    @Autowired
    private MeituanTbkClient meituanClient;

    @Autowired
    private AlscTbkClient alscClient;

    /**
     * 把淘宝链接/淘口令转成带推广位的链接和淘口令
     */
    @PostMapping("/convert")
    public AjaxResult convert(@RequestBody TbkConvertRequest request, HttpServletRequest httpRequest) {
        TbkConvertResult result = convertService.convert(request.getContent(),
                IpUtils.getIpAddr(httpRequest), resolveAttributionKey(request.getUserKey()));
        return success(result);
    }

    /**
     * 健康检查，便于部署后确认路由是否生效
     */
    @GetMapping("/health")
    public AjaxResult health() {
        return success("tbk ok");
    }

    /**
     * AI 选品：输入标题/商品/需求，跨淘宝、拼多多搜索并按性价比打分
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    @PostMapping("/pick")
    public AjaxResult pick(@RequestBody TbkPickRequest request, HttpServletRequest httpRequest) {
        return success(pickService.pick(request, IpUtils.getIpAddr(httpRequest),
                resolveAttributionKey(request.getUserKey())));
    }

    /**
     * 选品结果转链：把某个候选商品转成推广链接/淘口令
     *
     * add by CYQ 2026-11-04 优惠查询-AI 选品
     */
    @PostMapping("/pick/convert")
    public AjaxResult pickConvert(@RequestBody TbkPickRequest request, HttpServletRequest httpRequest) {
        TbkConvertResult result = pickService.convertCandidate(request,
                IpUtils.getIpAddr(httpRequest), resolveAttributionKey(request.getUserKey()));
        return success(result);
    }

    /**
     * 拼多多推广位授权备案链接（返回 60001 时用手机打开一次）
     *
     * add by CYQ 2026-11-03 转链模块支持拼多多
     */
    @GetMapping("/pdd/authorize")
    public AjaxResult pddAuthorize() {
        return success(pddClient.buildAuthorityUrl());
    }

    /**
     * 拼多多推广位是否已完成授权备案
     */
    @GetMapping("/pdd/status")
    public AjaxResult pddStatus() {
        return success(pddClient.isAuthorityBound());
    }

    /**
     * 查看线上实际生效的拼多多配置（排查 pid 配错）
     */
    @GetMapping("/pdd/config")
    public AjaxResult pddConfig() {
        return success(pddClient.configInfo());
    }

    /**
     * 查询当前多多进宝账号下的推广位（pid）列表
     */
    @GetMapping("/pdd/pids")
    public AjaxResult pddPids() {
        return success(pddClient.listPids());
    }

    /**
     * 美团联盟自助取链：生成推广链接
     *
     * add by CYQ 2026-11-05 接入美团联盟
     *
     * @param actId    活动ID，不传用 tbk.meituan.act-id
     * @param linkType 链接类型，不传用 tbk.meituan.link-type（1-H5 4-美团小程序 8-优选小程序）
     */
    @GetMapping("/meituan/link")
    public AjaxResult meituanLink(@RequestParam(required = false) String actId,
                                  @RequestParam(required = false) Integer linkType) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("link", meituanClient.generateLink(actId, linkType));
        data.put("actId", StringUtils.isNotEmpty(actId) ? actId : meituanClient.configInfo().get("actId"));
        data.put("sid", meituanClient.configInfo().get("sid"));
        return success(data);
    }

    /**
     * 查看线上实际生效的美团配置（排查 actId / sid 配错）
     */
    @GetMapping("/meituan/config")
    public AjaxResult meituanConfig() {
        return success(meituanClient.configInfo());
    }

    /**
     * 淘宝闪购官方活动转链
     *
     * add by CYQ 2026-11-05 接入淘宝闪购
     *
     * @param activityId 活动ID，不传用 tbk.alsc.activity-id（如 12628）
     * @param sid        三方会员ID，不传用 tbk.alsc.sid
     */
    @GetMapping("/alsc/link")
    public AjaxResult alscLink(@RequestParam(required = false) String activityId,
                               @RequestParam(required = false) String sid) {
        return success(alscClient.activityLink(activityId, sid));
    }

    /**
     * 查看线上实际生效的闪购配置
     */
    @GetMapping("/alsc/config")
    public AjaxResult alscConfig() {
        return success(alscClient.configInfo());
    }

    /**
     * 解析用户归因标识
     *
     * 登录态下只信任 token 里的用户ID；匿名请求用客户端稳定 key，并加 anon: 前缀，
     * 避免匿名用户伪造数字ID冒充他人（返现功能上线后尤其重要）。
     *
     * add by CYQ 2026-11-09 订单用户归因（返现基础）
     */
    private String resolveAttributionKey(String clientKey) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
                Long userId = ((LoginUser) authentication.getPrincipal()).getUserId();
                if (userId != null) {
                    return String.valueOf(userId);
                }
            }
        } catch (Exception ignored) {
            // 匿名单：忽略
        }
        if (StringUtils.isNotEmpty(clientKey)) {
            String key = clientKey.trim();
            if (key.length() > 60) {
                key = key.substring(0, 60);
            }
            return "anon:" + key;
        }
        return null;
    }
}

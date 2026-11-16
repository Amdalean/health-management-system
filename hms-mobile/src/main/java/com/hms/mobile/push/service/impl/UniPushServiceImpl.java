package com.hms.mobile.push.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.mobile.push.config.UniPushProperties;
import com.hms.mobile.push.domain.PushDevice;
import com.hms.mobile.push.mapper.PushDeviceMapper;
import com.hms.mobile.push.service.IUniPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * UniPush 2.0 推送发送服务实现（调用 uniCloud 云函数）
 * add by CYQ 2026-05-22
 * modify by CYQ 2026-06-08
 */
@Service
public class UniPushServiceImpl implements IUniPushService {
    private static final Logger log = LoggerFactory.getLogger(UniPushServiceImpl.class);

    @Autowired
    private UniPushProperties uniPushProperties;

    @Autowired
    private PushDeviceMapper pushDeviceMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> callCloudFunction(String type, String title, String content, List<String> cids) {
        try {
            String url = uniPushProperties.getFunctionUrl();
            if (url == null || url.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("code", -1);
                err.put("msg", "unipush.function-url 未配置");
                return err;
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("type", type);
            params.put("title", title);
            params.put("content", content);
            params.put("payload", "");
            if (cids != null && !cids.isEmpty()) {
                params.put("cids", cids);
            }
            params.put("request_id", UUID.randomUUID().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(params), headers);

            log.info("[推送] 调用云函数: url={}, type={}, title={}, cidsCount={}", url, type, title, cids != null ? cids.size() : 0);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("[推送] 云函数返回: {}", response.getBody());

            JsonNode result = objectMapper.readTree(response.getBody());
            Map<String, Object> map = new HashMap<>();
            if (result.has("code") && result.get("code").asInt() == 0) {
                map.put("code", 0);
                map.put("msg", "success");
            } else {
                map.put("code", -1);
                map.put("msg", result.has("msg") ? result.get("msg").asText() : "unknown error");
            }
            map.put("raw", response.getBody());
            return map;
        } catch (Exception e) {
            log.error("[推送] 调用云函数异常", e);
            Map<String, Object> map = new HashMap<>();
            map.put("code", -1);
            map.put("msg", e.getMessage());
            return map;
        }
    }

    private String toJson(Map<String, Object> result) {
        try { return objectMapper.writeValueAsString(result); }
        catch (Exception e) { return "{\"code\":-1,\"msg\":\"序列化失败\"}"; }
    }

    @Override
    public String pushToSingle(String clientId, String title, String body) {
        return toJson(callCloudFunction("single", title, body, Collections.singletonList(clientId)));
    }

    @Override
    public String pushToAll(String title, String body) {
        return toJson(callCloudFunction("all", title, body, null));
    }

    @Override
    public String pushToUser(Long userId, String title, String body) {
        try {
            PushDevice query = new PushDevice();
            query.setUserId(userId);
            List<PushDevice> devices = pushDeviceMapper.selectPushDeviceList(query);
            if (devices == null || devices.isEmpty()) {
                return "{\"code\":-1,\"msg\":\"该用户无已注册的推送设备\"}";
            }
            List<String> cids = new ArrayList<>();
            for (PushDevice device : devices) {
                if (device.getClientId() != null && !device.getClientId().isEmpty()) {
                    cids.add(device.getClientId());
                }
            }
            if (cids.isEmpty()) {
                return "{\"code\":-1,\"msg\":\"该用户无有效的推送设备\"}";
            }
            log.info("[推送] 按用户推送: userId={}, cids数量={}, title={}", userId, cids.size(), title);
            return toJson(callCloudFunction("single", title, body, cids));
        } catch (Exception e) {
            log.error("[推送] 按用户推送异常", e);
            return "{\"code\":-1,\"msg\":\"" + e.getMessage() + "\"}";
        }
    }
}
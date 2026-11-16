package com.hms.main.agnes.service;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.hms.main.agnes.domain.AgnesImageRequest;
import com.hms.main.agnes.domain.AgnesImageResponse;

/**
 * Agnes Image 2.5 Flash 图像生成服务
 *
 * add by Qwen 2026-07-20 Agnes Image 集成
 * update by CYQ 2026-11-09 模型升级至 agnes-image-2.5-flash（接口/尺寸/价格不变）
 */
@Service
public class AgnesImageService {

    private static final Logger log = LoggerFactory.getLogger(AgnesImageService.class);

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 初始重试间隔（毫秒） */
    private static final long RETRY_DELAY_MS = 2000;

    @Value("${ai.agnes.image.api-key}")
    private String apiKey;

    @Value("${ai.agnes.image.base-url}")
    private String baseUrl;

    @Value("${ai.agnes.image.model}")
    private String model;

    private final RestTemplate restTemplate;

    public AgnesImageService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(180000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 文生图 - 返回图片 URL
     */
    public String generateImage(String prompt, String size, String ratio) {
        AgnesImageRequest request = buildBaseRequest(prompt, size, ratio);

        AgnesImageRequest.ExtraBody extraBody = new AgnesImageRequest.ExtraBody();
        extraBody.setResponseFormat("url");
        request.setExtraBody(extraBody);

        return executeRequest(request, false);
    }

    /**
     * 文生图 - 返回 Base64
     */
    public String generateImageBase64(String prompt, String size, String ratio) {
        AgnesImageRequest request = buildBaseRequest(prompt, size, ratio);
        request.setReturnBase64(true);

        return executeRequest(request, true);
    }

    /**
     * 图生图 - 基于输入图像编辑/风格化
     */
    public String editImage(String prompt, String imageUrl, String size, String ratio) {
        AgnesImageRequest request = buildBaseRequest(prompt, size, ratio);

        AgnesImageRequest.ExtraBody extraBody = new AgnesImageRequest.ExtraBody();
        extraBody.setResponseFormat("url");
        extraBody.setImage(Collections.singletonList(imageUrl));
        request.setExtraBody(extraBody);

        return executeRequest(request, false);
    }

    private AgnesImageRequest buildBaseRequest(String prompt, String size, String ratio) {
        AgnesImageRequest request = new AgnesImageRequest();
        request.setModel(model);
        request.setPrompt(prompt);
        request.setSize(size);
        request.setRatio(ratio);
        return request;
    }

    private String executeRequest(AgnesImageRequest request, boolean returnBase64) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<AgnesImageRequest> entity = new HttpEntity<>(request, headers);

        log.info("Agnes Image 请求: model={}, size={}, ratio={}", request.getModel(), request.getSize(), request.getRatio());

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<AgnesImageResponse> response = restTemplate.exchange(
                        baseUrl, HttpMethod.POST, entity, AgnesImageResponse.class);

                AgnesImageResponse body = response.getBody();
                if (body == null || body.getData() == null || body.getData().isEmpty()) {
                    throw new RuntimeException("Agnes Image API 返回空结果");
                }

                AgnesImageResponse.ImageData imageData = body.getData().get(0);
                String result = returnBase64 ? imageData.getB64Json() : imageData.getUrl();
                // update by CYQ 2026-11-09 API 未使用的一侧返回空串，需判空避免把 "" 当成功
                if (result == null || result.trim().isEmpty()) {
                    throw new RuntimeException("Agnes Image API 返回空结果");
                }

                log.info("Agnes Image 生成成功: url={}", result);
                return result;
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_DELAY_MS * (1L << (attempt - 1));
                    log.warn("Agnes Image 请求失败(尝试 {}/{}), {}秒后重试: {}",
                            attempt, MAX_RETRIES, delay / 1000, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Agnes Image API 调用失败，已重试 " + MAX_RETRIES + " 次", lastException);
    }
}

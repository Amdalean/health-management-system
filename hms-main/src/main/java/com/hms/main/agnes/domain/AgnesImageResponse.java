package com.hms.main.agnes.domain;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Agnes Image API 响应 DTO
 *
 * add by Qwen 2026-07-20 Agnes Image 集成
 */
@Data
public class AgnesImageResponse {

    private Long created;

    private List<ImageData> data;

    @Data
    public static class ImageData {

        private String url;

        @JsonProperty("b64_json")
        private String b64Json;

        @JsonProperty("revised_prompt")
        private String revisedPrompt;
    }
}

package com.hms.main.agnes.domain;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Agnes Image API 请求 DTO
 *
 * add by Qwen 2026-07-20 Agnes Image 集成
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgnesImageRequest {

    private String model;

    private String prompt;

    private String size;

    private String ratio;

    @JsonProperty("return_base64")
    private Boolean returnBase64;

    @JsonProperty("extra_body")
    private ExtraBody extraBody;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExtraBody {

        @JsonProperty("response_format")
        private String responseFormat;

        private List<String> image;
    }
}

package com.hms.main.agnes.controller;

import javax.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.main.agnes.service.AgnesImageService;
import lombok.Data;

/**
 * Agnes Image 2.5 Flash 图像生成接口
 *
 * add by Qwen 2026-07-20 Agnes Image 集成
 * update by CYQ 2026-11-09 模型升级至 agnes-image-2.5-flash
 */
@RestController
@RequestMapping("/agnes/image")
public class AgnesImageController extends BaseController {

    @Autowired
    private AgnesImageService agnesImageService;

    /**
     * 文生图
     */
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody GenerateRequest request) {
        try {
            String result = agnesImageService.generateImage(
                    request.getPrompt(),
                    request.getSize(),
                    request.getRatio()
            );
            return success(result);
        } catch (Exception e) {
            return error("图像生成失败: " + e.getMessage());
        }
    }

    /**
     * 文生图 - Base64 输出
     */
    @PostMapping("/generateBase64")
    public AjaxResult generateBase64(@RequestBody GenerateRequest request) {
        try {
            String result = agnesImageService.generateImageBase64(
                    request.getPrompt(),
                    request.getSize(),
                    request.getRatio()
            );
            return success(result);
        } catch (Exception e) {
            return error("图像生成失败: " + e.getMessage());
        }
    }

    /**
     * 图生图 - 基于已有图像编辑
     */
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody EditRequest request) {
        try {
            String result = agnesImageService.editImage(
                    request.getPrompt(),
                    request.getImageUrl(),
                    request.getSize(),
                    request.getRatio()
            );
            return success(result);
        } catch (Exception e) {
            return error("图像编辑失败: " + e.getMessage());
        }
    }
}

@Data
class GenerateRequest {
    @NotBlank(message = "提示词不能为空")
    private String prompt;
    private String size = "1K";
    private String ratio = "1:1";
}

@Data
class EditRequest {
    @NotBlank(message = "提示词不能为空")
    private String prompt;
    @NotBlank(message = "输入图片URL不能为空")
    private String imageUrl;
    private String size = "1K";
    private String ratio = "1:1";
}

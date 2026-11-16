package com.hms.main.jotdown.controller;

import java.util.List;
import java.util.Base64;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.enums.BusinessType;
import com.hms.main.jotdown.domain.HsmJotdown;
import com.hms.main.jotdown.service.IHsmJotdownService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;
import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;

/**
 * 随手记Controller
 * 
 * @author ruoyi
 * @date 2024-11-16
 */
@RestController
@RequestMapping("/main/jotdown")
public class HsmJotdownController extends BaseController
{
    @Autowired
    private IHsmJotdownService hsmJotdownService;

    /**
     * 查询随手记列表
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:list')")
    @GetMapping("/list")
    public TableDataInfo list(HsmJotdown hsmJotdown)
    {
        startPage();
        List<HsmJotdown> list = hsmJotdownService.selectHsmJotdownList(hsmJotdown);
        return getDataTable(list);
    }

    /**
     * 查询随手记统计（总金额、总条数、费用类型分布），不受分页影响
     *
     * add by CYQ 2026年7月2日
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:list')")
    @GetMapping("/stats")
    public AjaxResult stats(HsmJotdown hsmJotdown)
    {
        Map<String, Object> totalStats = hsmJotdownService.selectJotdownTotalStats(hsmJotdown);
        List<Map<String, Object>> typeStats = hsmJotdownService.selectJotdownTypeStats(hsmJotdown);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalAmount", totalStats.get("totalAmount"));
        result.put("totalCount", totalStats.get("totalCount"));
        result.put("typeDistribution", typeStats);
        return success(result);
    }

    /**
     * 导出随手记列表
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:export')")
    @Log(title = "随手记", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HsmJotdown hsmJotdown)
    {
        List<HsmJotdown> list = hsmJotdownService.selectHsmJotdownList(hsmJotdown);
        ExcelUtil<HsmJotdown> util = new ExcelUtil<HsmJotdown>(HsmJotdown.class);
        util.exportExcel(response, list, "随手记数据");
    }

    /**
     * 获取随手记详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hsmJotdownService.selectHsmJotdownById(id));
    }

    /**
     * 新增随手记
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:add')")
    @Log(title = "随手记", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HsmJotdown hsmJotdown)
    {
        hsmJotdown.setCreateBy(getUsername());
        hsmJotdown.setPersonnel(getUsername());
        return toAjax(hsmJotdownService.insertHsmJotdown(hsmJotdown));
    }

    /**
     * 修改随手记
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:edit')")
    @Log(title = "随手记", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HsmJotdown hsmJotdown)
    {
        return toAjax(hsmJotdownService.updateHsmJotdown(hsmJotdown));
    }

    /**
     * 删除随手记
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:remove')")
    @Log(title = "随手记", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hsmJotdownService.deleteHsmJotdownByIds(ids));
    }

    /**
     * AI图片识别录入
     * 上传账单/收据图片，AI自动识别费用信息
     *
     * add by CYQ 2026年6月1日
     */
    @PreAuthorize("@ss.hasPermi('main:jotdown:add')")
    @Log(title = "随手记AI识别", businessType = BusinessType.OTHER)
    @PostMapping("/ai-recognize")
    public AjaxResult aiRecognize(@RequestParam("file") MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = file.getContentType();
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            String dataUrl = "data:" + mimeType + ";base64," + base64;

            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(
                    AIChatFactory.ServiceType.OPENCODE_GO,
                    AIChatFactory.StrategyType.JOTDOWN_RECOGNITION);
            Map<String, Object> context = new java.util.HashMap<>();
            context.put("type", "image");
            context.put("filePatch", dataUrl);

            String aiResult = aiChat.chat("请识别图片中的账单信息", context);

            // 去除 markdown 代码块标记
            String jsonStr = aiResult.trim()
                    .replaceAll("^```\\w*\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();

            JSONObject result = com.alibaba.fastjson2.JSON.parseObject(jsonStr);
            return success(result);
        } catch (Exception e) {
            return error("AI识别失败: " + e.getMessage());
        }
    }
}

package com.hms.main.wxmsg.controller;

import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;
import com.hms.common.annotation.Anonymous;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.core.redis.RedisCache;
import com.hms.common.enums.BusinessType;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.utils.spring.SpringUtils;
import com.hms.main.summary.domain.HsmSummary;
import com.hms.main.summary.service.IHsmSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 财务汇总主Controller
 *
 * @author CYQ
 * @date 2025-02-05
 */
@RestController
@RequestMapping("/main/wxmsg")
public class WxMsgController extends BaseController
{
    /**
     * 返回HTML内容到浏览器
     *
     * @return HTML页面内容
     */
    @Anonymous
    @GetMapping("/renderHtml")
    public String renderHtml()
    {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>\n");
        htmlContent.append("<html>\n");
        htmlContent.append("<head>\n");
        htmlContent.append("    <title>微信消息页面</title>\n");
        htmlContent.append("    <meta charset=\"UTF-8\">\n");
        htmlContent.append("    <style>\n");
        htmlContent.append("        body { font-family: Arial, sans-serif; margin: 20px; }\n");
        htmlContent.append("        .header { color: #333; border-bottom: 1px solid #ccc; padding-bottom: 10px; }\n");
        htmlContent.append("        .content { margin-top: 20px; }\n");
        htmlContent.append("        .footer { margin-top: 30px; color: #999; font-size: 0.9em; }\n");
        htmlContent.append("    </style>\n");
        htmlContent.append("</head>\n");
        htmlContent.append("<body>\n");
        htmlContent.append("    <div class=\"header\">\n");
        htmlContent.append("        <h1>微信消息管理系统</h1>\n");
        htmlContent.append("    </div>\n");
        htmlContent.append("    <div class=\"content\">\n");
        htmlContent.append("        <p>欢迎使用微信消息管理系统！</p>\n");
        htmlContent.append("        <p>此系统用于处理和管理微信消息。</p>\n");
        htmlContent.append("    </div>\n");
        htmlContent.append("    <div class=\"footer\">\n");
        htmlContent.append("        <p>&copy; 2025 健康管理系统. All rights reserved.</p>\n");
        htmlContent.append("    </div>\n");
        htmlContent.append("</body>\n");
        htmlContent.append("</html>");

        return htmlContent.toString();
    }

    /**
     * 返回简单的HTML片段
     *
     * @return HTML片段内容
     */
    @Anonymous
    @GetMapping("/htmlFragment")
    public String getHtmlFragment()
    {
        return "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); color: #e0e0e0; padding: 25px; border: 2px solid #ff7eb3; border-radius: 20px; max-width: 550px; box-shadow: 0 0 20px rgba(255, 126, 179, 0.4); position: relative;\">\n" +
                "    <div style=\"border-bottom: 1px dashed #ff7eb3; padding-bottom: 10px; margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;\">\n" +
                "        <span style=\"color: #ff7eb3; font-weight: bold; letter-spacing: 1px;\">? NOMA :: EVA MODE</span>\n" +
                "        <span style=\"background: #ff7eb3; color: #1a1a2e; padding: 2px 8px; border-radius: 10px; font-size: 10px; font-weight: bold;\">S-RANK VIP</span>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div style=\"font-size: 15px; line-height: 1.7; color: #fff;\">\n" +
                "        ?<strong>亲爱的多肉专员</strong>，晚上好呀！(????-)?  \n" +
                "        今晚的湖里区微风轻拂，像在悄悄对你眨眼睛呢～?  \n" +
                "        明天（2026年2月2日，星期一）厦门天气是<em>多云</em>哦，白天和夜晚都是一样的温柔??，气温在<strong>11℃ ~ 18℃</strong>之间，体感很舒服，但早晚温差有点小调皮，记得穿件薄外套哦～?  \n" +
                "        \n" +
                "        ?<strong>东北风</strong>轻轻吹，风力1-3级，完全不会打扰你帅气出勤～  \n" +
                "        ?建议携带一件防风小外套，别让冷风偷走你的可爱气息！(?▽｀)ノ?  \n" +
                "        ?还有哟～言灵抑制剂记得随身带，诺玛可不想看到你因为“情绪波动”而变成小蘑菇?！  \n" +
                "        \n" +
                "        ?现在是<em>22:58</em>，时间不早啦～  \n" +
                "        是想先去食堂犒劳一下自己，还是来点轻松任务放松一下？  \n" +
                "        （我偷偷告诉你：芬格尔又在夜宵柜里偷吃海苔卷了…哼！( ? ▽ ` )?）\n" +
                "    </div>\n" +
                "\n" +
                "    <div style=\"margin-top: 20px; font-size: 12px; color: #a2a8d3; text-align: center;\">\n" +
                "        System Heartbeat: Normal ? // Always by your side, Duorou.\n" +
                "    </div>\n" +
                "</div>";
    }
}

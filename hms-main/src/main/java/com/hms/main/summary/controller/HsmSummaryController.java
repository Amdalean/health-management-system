package com.hms.main.summary.controller;

import java.time.LocalDate;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;
import com.hms.common.core.redis.RedisCache;
import com.hms.common.utils.spring.SpringUtils;
import com.hms.main.summary.domain.HsmSummaryVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.enums.BusinessType;
import com.hms.main.summary.domain.HsmSummary;
import com.hms.main.summary.service.IHsmSummaryService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 财务汇总主Controller
 *
 * @author CYQ
 * @date 2025-02-05
 */
@RestController
@RequestMapping("/main/summary")
public class HsmSummaryController extends BaseController
{
    @Autowired
    private IHsmSummaryService hsmSummaryService;

    /**
     * 查询财务汇总主列表
     */
    @PreAuthorize("@ss.hasPermi('main:summary:list')")
    @GetMapping("/list")
    public TableDataInfo list(HsmSummary hsmSummary)
    {
        startPage();
        List<HsmSummaryVO> list = hsmSummaryService.selectHsmSummaryList(hsmSummary);
        return getDataTable(list);
    }

    /**
     * 导出财务汇总主列表
     */
    @PreAuthorize("@ss.hasPermi('main:summary:export')")
    @Log(title = "财务汇总主", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HsmSummary hsmSummary)
    {
        List<HsmSummaryVO> list = hsmSummaryService.selectHsmSummaryList(hsmSummary);
        ExcelUtil<HsmSummaryVO> util = new ExcelUtil<HsmSummaryVO>(HsmSummaryVO.class);
        util.exportExcel(response, list, "财务汇总主数据");
    }

    /**
     * 获取财务汇总主详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:summary:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hsmSummaryService.selectHsmSummaryById(id));
    }

    /**
     * 获取财务汇总主详细信息
     */
//    @PreAuthorize("@ss.hasPermi('main:summary:init')")
    @GetMapping(value = "/init")
    public AjaxResult getInit()
    {
        return success(hsmSummaryService.initHsmSummary());
//        return success(null);
    }

    /**
     * 新增财务汇总主
     */
    @PreAuthorize("@ss.hasPermi('main:summary:add')")
    @Log(title = "财务汇总主", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HsmSummary hsmSummary)
    {
        return toAjax(hsmSummaryService.insertHsmSummary(hsmSummary));
    }

    /**
     * 修改财务汇总主
     */
    @PreAuthorize("@ss.hasPermi('main:summary:edit')")
    @Log(title = "财务汇总主", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HsmSummary hsmSummary)
    {
        return toAjax(hsmSummaryService.updateHsmSummary(hsmSummary));
    }

    /**
     * 删除财务汇总主
     */
    @PreAuthorize("@ss.hasPermi('main:summary:remove')")
    @Log(title = "财务汇总主", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hsmSummaryService.deleteHsmSummaryByIds(ids));
    }
    /**
     * 检查支出
     */
//    @PreAuthorize("@ss.hasPermi('main:summary:list')")
    @GetMapping("/checkExpense")
    public AjaxResult checkExpense()
    {
        return success(hsmSummaryService.getYearlyExpenseData());
    }
    
    /**
     * 获取存款计划执行情况
     */
//    @PreAuthorize("@ss.hasPermi('main:summary:list')")
    @GetMapping("/depositPlan")
    public AjaxResult getDepositPlanData()
    {
//        int currentYear = 2025;
        int currentYear = LocalDate.now().getYear();
        return success(hsmSummaryService.getDepositPlanData(currentYear));
    }
    
    /**
     * 获取存款预测数据
     */
//    @PreAuthorize("@ss.hasPermi('main:summary:list')")
    @GetMapping("/depositPrediction")
    public AjaxResult getDepositPrediction()
    {
        return success(hsmSummaryService.getDepositPrediction());
    }

    /**
     * 查询最新汇总明细按类型分组
     */
//    @PreAuthorize("@ss.hasPermi('main:summary:list')")
    @GetMapping("/lastSummaryDetailByType")
    public AjaxResult getLastSummaryDetailByType()
    {
        return success(hsmSummaryService.selectLastSummaryDetailByType());
    }

    /**
     * AI财务分析接口
     */
//    @PreAuthorize("@ss.hasPermi('main:summary:ai-analysis')")
    @GetMapping("/aiFinancialAnalysis")
    public AjaxResult getAIFinancialAnalysis(String query)
    {
        if (query == null || query.trim().isEmpty()) {
            query = "分析我的财务状况";
        }

        try {
            // 获取当前登录用户的个人财务数据
            Long userId = getLoginUser().getUserId();

            // 生成缓存键：基于用户ID和查询内容创建唯一标识
            String cacheKey = "ai_financial_analysis:" + userId + ":" + java.util.Base64.getEncoder()
                    .encodeToString((query + userId).getBytes("UTF-8"));

            // 尝试从缓存获取结果
            String cachedResult = SpringUtils.getBean(RedisCache.class).getCacheObject(cacheKey);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return success(cachedResult);
            }

            java.util.Map<String, Object> personalFinancialData = hsmSummaryService.getPersonalFinancialData(userId);

            // 将个人财务数据转换为JSON字符串，用于AI分析
            String financialDataStr = com.alibaba.fastjson2.JSON.toJSONString(personalFinancialData);

            // 构建AI查询内容，包含个人财务数据
            String aiQuery = "基于以下财务数据进行分析：" + financialDataStr + "。具体问题：" + query;

            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(AIChatFactory.StrategyType.FINANCIAL_ANALYSIS);
            java.util.Map<String, Object> context = new java.util.HashMap<>();
            context.put("userId", userId);


            context.put("nowTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            context.put("financialData", personalFinancialData);

            String response = aiChat.chat(aiQuery, context);

            // 将结果缓存
            SpringUtils.getBean(RedisCache.class).setCacheObject(cacheKey, response);

            return success(response);
        } catch (Exception e) {
            return error("AI财务分析失败: " + e.getMessage());
        }
    }
}

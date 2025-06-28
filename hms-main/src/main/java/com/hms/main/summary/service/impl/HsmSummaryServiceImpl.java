package com.hms.main.summary.service.impl;

import java.time.LocalDate;
import java.util.List;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.hms.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.hms.main.summary.domain.HsmDetail;
import com.hms.main.summary.mapper.HsmSummaryMapper;
import com.hms.main.summary.domain.HsmSummary;
import com.hms.main.summary.service.IHsmSummaryService;
import java.math.BigDecimal;

/**
 * 财务汇总主Service业务层处理
 *
 * @author CYQ
 * @date 2025-02-05
 */
@Service
public class HsmSummaryServiceImpl implements IHsmSummaryService
{
    @Autowired
    private HsmSummaryMapper hsmSummaryMapper;

    /**
     * 查询财务汇总主
     *
     * @param id 财务汇总主主键
     * @return 财务汇总主
     */
    @Override
    public HsmSummary selectHsmSummaryById(Long id)
    {
        return hsmSummaryMapper.selectHsmSummaryById(id);
    }

    /**
     * 查询财务汇总主列表
     *
     * @param hsmSummary 财务汇总主
     * @return 财务汇总主
     */
    @Override
    public List<HsmSummary> selectHsmSummaryList(HsmSummary hsmSummary)
    {
        return hsmSummaryMapper.selectHsmSummaryList(hsmSummary);
    }

    /**
     * 新增财务汇总主
     *
     * @param hsmSummary 财务汇总主
     * @return 结果
     */
    @Transactional
    @Override
    public int insertHsmSummary(HsmSummary hsmSummary)
    {
        hsmSummary.setCreateTime(DateUtils.getNowDate());
        int rows = hsmSummaryMapper.insertHsmSummary(hsmSummary);
        insertHsmDetail(hsmSummary);
        return rows;
    }

    /**
     * 修改财务汇总主
     *
     * @param hsmSummary 财务汇总主
     * @return 结果
     */
    @Transactional
    @Override
    public int updateHsmSummary(HsmSummary hsmSummary)
    {
        hsmSummary.setUpdateTime(DateUtils.getNowDate());
        hsmSummaryMapper.deleteHsmDetailBySummaryId(hsmSummary.getId());
        insertHsmDetail(hsmSummary);
        return hsmSummaryMapper.updateHsmSummary(hsmSummary);
    }

    /**
     * 批量删除财务汇总主
     *
     * @param ids 需要删除的财务汇总主主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteHsmSummaryByIds(Long[] ids)
    {
        hsmSummaryMapper.deleteHsmDetailBySummaryIds(ids);
        return hsmSummaryMapper.deleteHsmSummaryByIds(ids);
    }

    /**
     * 删除财务汇总主信息
     *
     * @param id 财务汇总主主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteHsmSummaryById(Long id)
    {
        hsmSummaryMapper.deleteHsmDetailBySummaryId(id);
        return hsmSummaryMapper.deleteHsmSummaryById(id);
    }

    @Override
    public JSONObject initHsmSummary() {
        LocalDate date = getLastMonth();
//        HsmSummary data = hsmSummaryMapper.selectHsmDetailByDate(date.getYear(),date.getMonth().getValue());
        HsmSummary data = hsmSummaryMapper.selectLastData();
        List<HsmDetail> detail = hsmSummaryMapper.selectLastDetail();
        JSONObject json = new JSONObject();
        String head = JSONObject.toJSONString(data);
        String items = JSONArray.toJSONString(detail);
        json.put("head",JSONObject.parse(head));
        json.put("items",JSONArray.parse(items));
        return json;
    }
    
    @Override
    public JSONArray getYearlyExpenseData() {
        HsmSummary hsmSummary = new HsmSummary();
        List<HsmSummary> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);
        
        // 获取当前年份
        int currentYear = LocalDate.now().getYear();
        
        // 创建全年12个月的数据数组
        JSONArray array = new JSONArray();
        for (int month = 1; month <= 12; month++) {
            JSONObject json = new JSONObject();
            json.put("date", currentYear + "-" + String.format("%02d", month));
            
            // 查找对应月份的数据
            BigDecimal expense = BigDecimal.ZERO;
            for (HsmSummary summary : list) {
                if (summary.getYear() != null && summary.getYear() == currentYear && 
                    summary.getMonth() != null && summary.getMonth() == month) {
                    expense = summary.getExpense() != null ? summary.getExpense() : BigDecimal.ZERO;
                    break;
                }
            }
            json.put("expense", expense);
            array.add(json);
        }
        
        return array;
    }

    @Override
    public JSONArray getDepositPlanData() {
        HsmSummary hsmSummary = new HsmSummary();
        List<HsmSummary> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);
        
        // 获取当前年份
        int currentYear = LocalDate.now().getYear();
        
        // 查找年初存款（第一个月的月初存款）
        BigDecimal startDeposit = BigDecimal.ZERO;
        for (HsmSummary summary : list) {
            if (summary.getYear() != null && summary.getYear() == currentYear && 
                summary.getMonth() != null && summary.getMonth() == 1) {
                startDeposit = summary.getStartDeposit() != null ? summary.getStartDeposit() : BigDecimal.ZERO;
                break;
            }
        }
        
        // 如果找不到年初数据，使用第一个找到的月初存款
        if (startDeposit.compareTo(BigDecimal.ZERO) == 0) {
            for (HsmSummary summary : list) {
                if (summary.getYear() != null && summary.getYear() == currentYear && 
                    summary.getStartDeposit() != null) {
                    startDeposit = summary.getStartDeposit();
                    break;
                }
            }
        }
        
        // 年底目标增长10万
        BigDecimal targetGrowth = new BigDecimal("100000");
        BigDecimal monthlyGrowth = targetGrowth.divide(new BigDecimal("12"), 2, BigDecimal.ROUND_HALF_UP);
        
        // 创建全年12个月的数据数组
        JSONArray array = new JSONArray();
        for (int month = 1; month <= 12; month++) {
            JSONObject json = new JSONObject();
            json.put("date", currentYear + "-" + String.format("%02d", month));
            
            // 查找对应月份的实际存款数据
            BigDecimal actualDeposit = BigDecimal.ZERO;
            for (HsmSummary summary : list) {
                if (summary.getYear() != null && summary.getYear() == currentYear && 
                    summary.getMonth() != null && summary.getMonth() == month) {
                    actualDeposit = summary.getEndDeposit() != null ? summary.getEndDeposit() : BigDecimal.ZERO;
                    break;
                }
            }
            
            // 计算计划存款（年初存款 + 累计增长）
            BigDecimal plannedDeposit = startDeposit.add(monthlyGrowth.multiply(new BigDecimal(month)));
            
            json.put("actualDeposit", actualDeposit);
            json.put("plannedDeposit", plannedDeposit);
            array.add(json);
        }
        
        return array;
    }

    @Override
    public JSONObject getDepositPrediction() {
        HsmSummary hsmSummary = new HsmSummary();
        List<HsmSummary> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);
        
        // 获取当前年份
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        
        // 获取历史数据（过去6个月，如果不足6个月则取所有可用数据）
        List<BigDecimal> monthlyDeposits = new ArrayList<>();
        List<BigDecimal> monthlyGrowths = new ArrayList<>();
        
        BigDecimal lastDeposit = null;
        for (int month = 1; month <= currentMonth; month++) {
            BigDecimal monthDeposit = BigDecimal.ZERO;
            for (HsmSummary summary : list) {
                if (summary.getYear() != null && summary.getYear() == currentYear && 
                    summary.getMonth() != null && summary.getMonth() == month) {
                    monthDeposit = summary.getEndDeposit() != null ? summary.getEndDeposit() : BigDecimal.ZERO;
                    break;
                }
            }
            
            if (monthDeposit.compareTo(BigDecimal.ZERO) > 0) {
                monthlyDeposits.add(monthDeposit);
                if (lastDeposit != null) {
                    BigDecimal growth = monthDeposit.subtract(lastDeposit);
                    monthlyGrowths.add(growth);
                }
                lastDeposit = monthDeposit;
            }
        }
        
        // 计算平均月增长
        BigDecimal avgMonthlyGrowth = BigDecimal.ZERO;
        if (!monthlyGrowths.isEmpty()) {
            BigDecimal totalGrowth = monthlyGrowths.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgMonthlyGrowth = totalGrowth.divide(new BigDecimal(monthlyGrowths.size()), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        // 预测年底存款
        BigDecimal currentDeposit = lastDeposit != null ? lastDeposit : BigDecimal.ZERO;
        int remainingMonths = 12 - currentMonth;
        BigDecimal predictedYearEndDeposit = currentDeposit.add(avgMonthlyGrowth.multiply(new BigDecimal(remainingMonths)));
        
        // 计算目标存款（年初存款 + 10万）
        BigDecimal targetYearEndDeposit = BigDecimal.ZERO;
        if (!monthlyDeposits.isEmpty()) {
            BigDecimal startDeposit = monthlyDeposits.get(0);
            targetYearEndDeposit = startDeposit.add(new BigDecimal("100000"));
        }
        
        // 计算达成率和差距
        BigDecimal gap = predictedYearEndDeposit.subtract(targetYearEndDeposit);
        double achievementRate = 0.0;
        if (targetYearEndDeposit.compareTo(BigDecimal.ZERO) > 0) {
            achievementRate = predictedYearEndDeposit.divide(targetYearEndDeposit, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100")).doubleValue();
        }
        
        // 生成建议
        List<String> recommendations = new ArrayList<>();
        if (gap.compareTo(BigDecimal.ZERO) < 0) {
            recommendations.add("当前趋势下可能无法达成年底目标");
            recommendations.add("建议增加月度储蓄目标");
            recommendations.add("关注支出控制，减少不必要开支");
        } else {
            recommendations.add("当前趋势良好，有望达成目标");
            recommendations.add("继续保持当前的储蓄习惯");
        }
        
        if (avgMonthlyGrowth.compareTo(BigDecimal.ZERO) < 0) {
            recommendations.add("月度存款呈下降趋势，需要调整财务策略");
        }
        
        // 构建返回结果
        JSONObject result = new JSONObject();
        
        JSONObject prediction = new JSONObject();
        prediction.put("yearEndDeposit", predictedYearEndDeposit);
        prediction.put("targetGap", gap);
        prediction.put("achievementRate", Math.round(achievementRate * 100.0) / 100.0);
        prediction.put("targetYearEndDeposit", targetYearEndDeposit);
        result.put("prediction", prediction);
        
        JSONObject trend = new JSONObject();
        trend.put("avgMonthlyGrowth", avgMonthlyGrowth);
        trend.put("currentDeposit", currentDeposit);
        trend.put("remainingMonths", remainingMonths);
        trend.put("dataPoints", monthlyDeposits.size());
        result.put("trend", trend);
        
        result.put("recommendations", recommendations);
        
        return result;
    }

    private static LocalDate getLastMonth() {
        LocalDate date = LocalDate.now();
        // 获取上一个月的日期
        return date.minusMonths(1);
    }

    /**
     * 新增财务明细子表信息
     *
     * @param hsmSummary 财务汇总主对象
     */
    public void insertHsmDetail(HsmSummary hsmSummary)
    {
        List<HsmDetail> hsmDetailList = hsmSummary.getHsmDetailList();
        Long id = hsmSummary.getId();
        if (StringUtils.isNotNull(hsmDetailList))
        {
            List<HsmDetail> list = new ArrayList<HsmDetail>();
            for (HsmDetail hsmDetail : hsmDetailList)
            {
                hsmDetail.setSummaryId(id);
                list.add(hsmDetail);
            }
            if (list.size() > 0)
            {
                hsmSummaryMapper.batchHsmDetail(list);
            }
        }
    }
}

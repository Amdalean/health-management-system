package com.hms.main.summary.service.impl;

import java.time.LocalDate;
import java.util.List;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.annotation.DataScope;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.SecurityUtils;
import com.hms.main.summary.domain.HsmSummaryPlanDTO;
import com.hms.main.summary.domain.HsmSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    @DataScope(userAlias="u")
    public List<HsmSummaryVO> selectHsmSummaryList(HsmSummary hsmSummary)
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
        //add by CYQ 2025年8月29日 增加制单人逻辑
        hsmSummary.setCreateBy(SecurityUtils.getUserId().toString());
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
        //add by CYQ 2025年8月29日 增加制单人
        String userId = SecurityUtils.getUserId()+"";
        hsmSummary.setUpdateBy(userId);
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
        //add by CYQ 2025年8月29日 增加制单人逻辑
        String userId = SecurityUtils.getUserId()+"";
        HsmSummary data = hsmSummaryMapper.selectLastData(userId);
        List<HsmDetail> detail = hsmSummaryMapper.selectLastDetail(userId);
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
        List<HsmSummaryVO> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);
        
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
    public List<HsmSummaryPlanDTO> getDepositPlanData(int currentYear) {
        // 获取当前年份和月份
//        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        HsmSummary hsmSummary = new HsmSummary();
        hsmSummary.setYear((long) currentYear);
        hsmSummary.setCreateBy(SecurityUtils.getUserId()+"");
        List<HsmSummaryVO> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);


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
        
        // 计算历史平均月增长
        BigDecimal avgMonthlyGrowth = calculateAverageMonthlyGrowth(list, currentYear);
        
        // 年底目标增长10万
        BigDecimal targetGrowth = new BigDecimal("100000");
        BigDecimal monthlyGrowth = targetGrowth.divide(new BigDecimal("12"), 2, BigDecimal.ROUND_HALF_UP);
        
        // 创建全年12个月的数据数组
        List<HsmSummaryPlanDTO> array = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            HsmSummaryPlanDTO dto = new HsmSummaryPlanDTO();
            dto.setDate(currentYear + "-" + String.format("%02d", month));
            
            // 查找对应月份的实际存款数据
            BigDecimal actualDeposit = getActualDeposit(list,currentYear,month,"endDeposit");

            // 预测值 = 上月预测存款+前三个月结余的平均值
//            BigDecimal predictedDeposit = startDeposit.add(avgMonthlyGrowth.multiply(new BigDecimal(month)));
            //前面所有月份的历史结余
            List<BigDecimal> allPreviousBalances = new ArrayList<>();
            for (int i = 1; i < month; i++) {
                BigDecimal balance = getActualDeposit(list, currentYear, i, "balance");
                if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
                    allPreviousBalances.add(balance);
                }
            }
            
            BigDecimal predictedDeposit;
            if (!allPreviousBalances.isEmpty()) {
                BigDecimal sum = allPreviousBalances.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                predictedDeposit = sum.divide(
                    new BigDecimal(allPreviousBalances.size()),
                    2, 
                    BigDecimal.ROUND_HALF_UP
                );
            } else {
                //没有历史数据则取本月结余
                predictedDeposit = getActualDeposit(list, currentYear, month, "balance");
            }
            //获取上月预测存款
            BigDecimal startDeposit_loc;
            if (month == 1) {
                startDeposit_loc = startDeposit; // 第一个月使用年初存款作为起始值
            } else {
                startDeposit_loc = array.get(month - 2).getPlannedDeposit(); // 否则使用上个月的计划存款
            }
            predictedDeposit = predictedDeposit.add(startDeposit_loc);
            // 计算计划存款（年初存款 + 累计增长）
            BigDecimal plannedDeposit = startDeposit.add(monthlyGrowth.multiply(new BigDecimal(month)));

            dto.setActualDeposit(actualDeposit);
            dto.setPredictedDeposit(predictedDeposit);
            dto.setPlannedDeposit(plannedDeposit);
            array.add(dto);
        }
        
        return array;
    }
    private BigDecimal getActualDeposit(List<HsmSummaryVO> list,int currentYear,int month,String key){
        // 查找对应月份的实际存款数据
        BigDecimal actualDeposit = null;
        for (HsmSummary summary : list) {
            if (summary.getYear() != null && summary.getYear() == currentYear &&
                    summary.getMonth() != null && summary.getMonth() == month) {
                actualDeposit = summary.getAttributeValue(key) != null ? (BigDecimal) summary.getAttributeValue(key) : null;
                break;
            }
        }
        return actualDeposit;
    }
    /**
     * 计算历史平均月增长
     *
     * @param list 历史数据列表
     * @param currentYear 当前年份
     * @return 平均月增长
     */
    private BigDecimal calculateAverageMonthlyGrowth(List<HsmSummaryVO> list, int currentYear) {
        List<BigDecimal> monthlyDeposits = new ArrayList<>();
        
        // 按月份排序获取本年数据
        for (int month = 1; month <= 12; month++) {
            for (HsmSummary summary : list) {
                if (summary.getYear() != null && summary.getYear() == currentYear && 
                    summary.getMonth() != null && summary.getMonth() == month &&
                    summary.getEndDeposit() != null) {
                    monthlyDeposits.add(summary.getEndDeposit());
                    break;
                }
            }
        }
        
        // 如果没有足够的数据点，返回0
        if (monthlyDeposits.size() < 2) {
            return new BigDecimal("0");
        }
        
        // 计算月度增长
        BigDecimal totalGrowth = BigDecimal.ZERO;
        for (int i = 1; i < monthlyDeposits.size(); i++) {
            BigDecimal growth = monthlyDeposits.get(i).subtract(monthlyDeposits.get(i - 1));
            totalGrowth = totalGrowth.add(growth);
        }
        
        // 计算平均月增长
        BigDecimal avgGrowth = totalGrowth.divide(new BigDecimal(monthlyDeposits.size() - 1), 2, BigDecimal.ROUND_HALF_UP);
        return avgGrowth;
    }

    @Override
    public JSONObject getDepositPrediction() {

        // 获取当前年份
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        HsmSummary hsmSummary = new HsmSummary();
        hsmSummary.setCreateBy(SecurityUtils.getUserId()+"");
        hsmSummary.setYear((long) currentYear);

        List<HsmSummaryVO> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);

        
        // 获取历史数据（过去6个月，如果不足6个月则取所有可用数据）
        List<BigDecimal> monthlyDeposits = new ArrayList<>();//月底存款
        List<BigDecimal> monthlyGrowths = new ArrayList<>();//
        
        BigDecimal lastDeposit = null;
        for (HsmSummary summary : list) {
            BigDecimal monthDeposit = BigDecimal.ZERO;
            if (summary.getYear() != null && summary.getYear() == currentYear &&
                    summary.getMonth() != null) {
                monthDeposit = summary.getEndDeposit() != null ? summary.getEndDeposit() : BigDecimal.ZERO;
                monthlyDeposits.add(monthDeposit);

                BigDecimal growth = summary.getBalance();
                monthlyGrowths.add(growth);
            }
        }
//        for (int month = 1; month <= currentMonth; month++) {
//            BigDecimal monthDeposit = BigDecimal.ZERO;
//
//
////            if (monthDeposit.compareTo(BigDecimal.ZERO) > 0) {
////                monthlyDeposits.add(monthDeposit);
////                if (lastDeposit != null) {
////                    BigDecimal growth = summary.getBalance();
////                    monthlyGrowths.add(growth);
////                }
////                lastDeposit = monthDeposit;
////            }
//        }
        
        // 计算平均月增长
        BigDecimal avgMonthlyGrowth = BigDecimal.ZERO;
        if (!monthlyGrowths.isEmpty()) {
            BigDecimal totalGrowth = monthlyGrowths.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgMonthlyGrowth = totalGrowth.divide(new BigDecimal(monthlyGrowths.size()), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        // 预测年底存款
        BigDecimal currentDeposit = lastDeposit != null ? lastDeposit : BigDecimal.ZERO;//当前存款
        int remainingMonths = 12 - currentMonth;//剩余月数
        List<HsmSummaryPlanDTO> data = getDepositPlanData(currentYear);
        BigDecimal predictedYearEndDeposit = data.get(11).getPredictedDeposit();//预测年底存款
        
        // 计算目标存款（年初存款 + 10万）
        BigDecimal targetYearEndDeposit = data.get(11).getPlannedDeposit();
        
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
        prediction.put("yearEndDeposit", predictedYearEndDeposit);//预测年底存款
        prediction.put("targetGap", gap);//差距
        prediction.put("achievementRate", Math.round(achievementRate * 100.0) / 100.0);//达成率
        prediction.put("targetYearEndDeposit", targetYearEndDeposit);//目标存款
        result.put("prediction", prediction);
        
        JSONObject trend = new JSONObject();
        trend.put("avgMonthlyGrowth", avgMonthlyGrowth);//平均月增长
        trend.put("currentDeposit", currentDeposit);//当前存款
        trend.put("remainingMonths", remainingMonths);//剩余月数
        trend.put("dataPoints", monthlyDeposits.size());//数据点数
        result.put("trend", trend);
        
        result.put("recommendations", recommendations);
        
        return result;
    }

    /**
     * 获取存款结构数据
     *
     * @return 存款结构数据
     */
    @Override
    public Map<String, Object> getDepositStructure() {
        Map<String, Object> result = new HashMap<>();

        //add by CYQ 2025年8月29日 增加制单人逻辑
        String userId = SecurityUtils.getUserId()+"";
        // 查询最新的财务明细数据
        List<HsmDetail> detailList = hsmSummaryMapper.selectLastDetail(userId);
        if (detailList == null || detailList.isEmpty()) {
            result.put("current", 0);
            result.put("fixed", 0);
            result.put("wealth", 0);
            result.put("total", 0);
            return result;
        }
        
        BigDecimal currentDeposit = BigDecimal.ZERO; // 活期存款
        BigDecimal fixedDeposit = BigDecimal.ZERO;   // 定期存款
        BigDecimal wealthDeposit = BigDecimal.ZERO;  // 理财存款
        
        // 遍历明细数据，根据category分类统计
        for (HsmDetail detail : detailList) {
            if (detail.getCategory() != null) {
                switch (detail.getCategory()) {
                    case "活期存款":
                        currentDeposit = currentDeposit.add(detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                        break;
                    case "定期存款":
                        fixedDeposit = fixedDeposit.add(detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                        break;
                    case "理财存款":
                        wealthDeposit = wealthDeposit.add(detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                        break;
                    default:
                        break;
                }
            }
        }
        
        BigDecimal totalDeposit = currentDeposit.add(fixedDeposit.add(wealthDeposit));
        
        result.put("current", currentDeposit);
        result.put("fixed", fixedDeposit);
        result.put("wealth", wealthDeposit);
        result.put("total", totalDeposit);
        
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

    @Override
    public List<com.hms.main.summary.domain.SummaryDetailVo> selectLastSummaryDetailByType() {
        //add by CYQ 2025年11月18日 增加制单人逻辑
        Long userId = SecurityUtils.getUserId();
//        Long userId = 101L;
        return hsmSummaryMapper.selectLastSummaryDetailByType(userId);
    }

    @Override
    public Map<String, Object> getPersonalFinancialData(Long userId) {
        Map<String, Object> financialData = new HashMap<>();

        // 获取用户最新的财务汇总数据
        HsmSummary latestSummary = hsmSummaryMapper.selectLastData(userId.toString());
        financialData.put("latestSummary", latestSummary);

        // 获取用户最新的财务明细数据
        List<HsmDetail> detailList = hsmSummaryMapper.selectLastDetail(userId.toString());
        financialData.put("detailList", detailList);

        // 获取用户的存款结构数据
        Map<String, Object> depositStructure = getDepositStructureData(userId);
        financialData.put("depositStructure", depositStructure);

        // 获取全年的支出数据
        JSONArray yearlyExpenseData = getYearlyExpenseDataForUser(userId);
        financialData.put("yearlyExpenseData", yearlyExpenseData);

        // 获取存款计划执行数据
        JSONArray depositPlanData = getDepositPlanDataForUser(userId);
        financialData.put("depositPlanData", depositPlanData);

        return financialData;
    }

    /**
     * 获取指定用户的存款结构数据
     *
     * @param userId 用户ID
     * @return 存款结构数据
     */
    private Map<String, Object> getDepositStructureData(Long userId) {
        //add by CYQ 2025年8月29日 增加制单人逻辑
        String userIdStr = userId.toString();
        // 查询最新的财务明细数据
        List<HsmDetail> detailList = hsmSummaryMapper.selectLastDetail(userIdStr);
        if (detailList == null || detailList.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("current", 0);
            result.put("fixed", 0);
            result.put("wealth", 0);
            result.put("total", 0);
            return result;
        }

        BigDecimal currentDeposit = BigDecimal.ZERO; // 活期存款
        BigDecimal fixedDeposit = BigDecimal.ZERO;   // 定期存款
        BigDecimal wealthDeposit = BigDecimal.ZERO;  // 理财存款

        // 遍历明细数据，根据category分类统计
        for (HsmDetail detail : detailList) {
            if (detail.getCategory() != null) {
                switch (detail.getCategory()) {
                    case "活期存款":
                        currentDeposit = currentDeposit.add(detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                        break;
                    case "定期存款":
                        fixedDeposit = fixedDeposit.add(detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                        break;
                    case "理财存款":
                        wealthDeposit = wealthDeposit.add(detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                        break;
                    default:
                        break;
                }
            }
        }

        BigDecimal totalDeposit = currentDeposit.add(fixedDeposit.add(wealthDeposit));

        Map<String, Object> result = new HashMap<>();
        result.put("current", currentDeposit);
        result.put("fixed", fixedDeposit);
        result.put("wealth", wealthDeposit);
        result.put("total", totalDeposit);

        return result;
    }

    /**
     * 获取指定用户的全年支出数据
     *
     * @param userId 用户ID
     * @return 全年支出数据
     */
    private JSONArray getYearlyExpenseDataForUser(Long userId) {
        // 根据用户ID查询相关数据，这里暂时使用通用方法，实际可能需要添加按用户查询的方法
        HsmSummary hsmSummary = new HsmSummary();
        // 可能需要在mapper中添加按userId查询的方法，暂时使用现有方法
        List<HsmSummaryVO> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);

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

    /**
     * 获取指定用户的存款计划执行数据
     *
     * @param userId 用户ID
     * @return 存款计划执行数据
     */
    private JSONArray getDepositPlanDataForUser(Long userId) {
        HsmSummary hsmSummary = new HsmSummary();
        List<HsmSummaryVO> list = hsmSummaryMapper.selectHsmSummaryList(hsmSummary);

        // 获取当前年份和月份
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

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

        // 计算历史平均月增长
        BigDecimal avgMonthlyGrowth = calculateAverageMonthlyGrowth(list, currentYear);

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
            boolean hasActualData = false;
            for (HsmSummary summary : list) {
                if (summary.getYear() != null && summary.getYear() == currentYear &&
                    summary.getMonth() != null && summary.getMonth() == month) {
                    actualDeposit = summary.getEndDeposit() != null ? summary.getEndDeposit() : BigDecimal.ZERO;
                    hasActualData = true;
                    break;
                }
            }

            // 如果是当前月之后的月份且没有实际数据，则预测数据
            BigDecimal predictedDeposit = actualDeposit;
            if (!hasActualData && month >= currentMonth) {
                // 预测值 = 年初存款 + 平均月增长 * 月份数
                predictedDeposit = startDeposit.add(avgMonthlyGrowth.multiply(new BigDecimal(month)));
            }

            // 计算计划存款（年初存款 + 累计增长）
            BigDecimal plannedDeposit = startDeposit.add(monthlyGrowth.multiply(new BigDecimal(month)));

            json.put("actualDeposit", actualDeposit);
            json.put("predictedDeposit", predictedDeposit);
            json.put("plannedDeposit", plannedDeposit);
            array.add(json);
        }

        return array;
    }
}

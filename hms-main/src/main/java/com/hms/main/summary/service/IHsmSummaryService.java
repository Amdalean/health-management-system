package com.hms.main.summary.service;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.hms.main.summary.domain.HsmSummary;

/**
 * 财务汇总主Service接口
 *
 * @author CYQ
 * @date 2025-02-05
 */
//@Component
public interface IHsmSummaryService
{
    /**
     * 查询财务汇总主
     *
     * @param id 财务汇总主主键
     * @return 财务汇总主
     */
    public HsmSummary selectHsmSummaryById(Long id);

    /**
     * 查询财务汇总主列表
     *
     * @param hsmSummary 财务汇总主
     * @return 财务汇总主集合
     */
    public List<HsmSummary> selectHsmSummaryList(HsmSummary hsmSummary);

    /**
     * 新增财务汇总主
     *
     * @param hsmSummary 财务汇总主
     * @return 结果
     */
    public int insertHsmSummary(HsmSummary hsmSummary);

    /**
     * 修改财务汇总主
     *
     * @param hsmSummary 财务汇总主
     * @return 结果
     */
    public int updateHsmSummary(HsmSummary hsmSummary);

    /**
     * 批量删除财务汇总主
     *
     * @param ids 需要删除的财务汇总主主键集合
     * @return 结果
     */
    public int deleteHsmSummaryByIds(Long[] ids);

    /**
     * 删除财务汇总主信息
     *
     * @param id 财务汇总主主键
     * @return 结果
     */
    public int deleteHsmSummaryById(Long id);
    /**
     * 初始化财务汇总主信息
     *
     * @return 财务汇总主
     */
    public JSONObject initHsmSummary();
    
    /**
     * 获取全年支出数据
     *
     * @return 全年支出数据
     */
    public JSONArray getYearlyExpenseData();
    
    /**
     * 获取存款计划执行数据
     *
     * @return 存款计划执行数据
     */
    public JSONArray getDepositPlanData();
    
    /**
     * 获取存款预测数据
     *
     * @return 存款预测数据
     */
    public JSONObject getDepositPrediction();

    Map<String, Object> getDepositStructure();
}

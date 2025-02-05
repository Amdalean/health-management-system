package com.hms.main.summary.service;

import java.util.List;
import com.hms.main.summary.domain.HsmSummary;

/**
 * 财务汇总主Service接口
 * 
 * @author CYQ
 * @date 2025-01-28
 */
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
}

package com.hms.main.summary.mapper;

import java.util.List;
import com.hms.main.summary.domain.HsmSummary;
import com.hms.main.summary.domain.HsmDetail;
import com.hms.main.summary.domain.HsmSummaryVO;
import com.hms.main.summary.domain.SummaryDetailVo;
import org.apache.ibatis.annotations.Param;

/**
 * 财务汇总主Mapper接口
 * 
 * @author CYQ
 * @date 2025-02-05
 */
public interface HsmSummaryMapper 
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
    public List<HsmSummaryVO> selectHsmSummaryList(HsmSummary hsmSummary);

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
     * 删除财务汇总主
     * 
     * @param id 财务汇总主主键
     * @return 结果
     */
    public int deleteHsmSummaryById(Long id);

    /**
     * 批量删除财务汇总主
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHsmSummaryByIds(Long[] ids);

    /**
     * 批量删除财务明细子表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHsmDetailBySummaryIds(Long[] ids);
    
    /**
     * 批量新增财务明细子表
     * 
     * @param hsmDetailList 财务明细子表列表
     * @return 结果
     */
    public int batchHsmDetail(List<HsmDetail> hsmDetailList);
    

    /**
     * 通过财务汇总主主键删除财务明细子表信息
     * 
     * @param id 财务汇总主ID
     * @return 结果
     */
    public int deleteHsmDetailBySummaryId(Long id);
    /** 查询待维护的最新期间 */
    public String getLastData();
    /**
     * 根据年月查询数据
     * */
    public HsmSummary selectHsmDetailByDate(@Param("year")Integer year, @Param("month")Integer month);
    /** 查询最新数据 */
    public HsmSummary selectLastData(String createBy);
    /** 查询最新明细 */
    public List<HsmDetail> selectLastDetail(String createBy);

    /**
     * 查询最新汇总明细按类型分组
     *
     * @param createBy 创建人ID
     * @return 按类型分组的汇总明细列表
     */
    public List<SummaryDetailVo> selectLastSummaryDetailByType(Long createBy);
}

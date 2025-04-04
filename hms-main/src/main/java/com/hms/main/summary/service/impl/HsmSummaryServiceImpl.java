package com.hms.main.summary.service.impl;

import java.time.LocalDate;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

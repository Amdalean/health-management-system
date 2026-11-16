package com.hms.main.jotdown.mapper;

import java.util.List;
import java.util.Map;
import com.hms.main.jotdown.domain.HsmJotdown;

/**
 * 随手记Mapper接口
 * 
 * @author ruoyi
 * @date 2024-11-16
 */
public interface HsmJotdownMapper 
{
    /**
     * 查询随手记
     * 
     * @param id 随手记主键
     * @return 随手记
     */
    public HsmJotdown selectHsmJotdownById(Long id);

    /**
     * 查询随手记列表
     * 
     * @param hsmJotdown 随手记
     * @return 随手记集合
     */
    public List<HsmJotdown> selectHsmJotdownList(HsmJotdown hsmJotdown);

    /**
     * 新增随手记
     * 
     * @param hsmJotdown 随手记
     * @return 结果
     */
    public int insertHsmJotdown(HsmJotdown hsmJotdown);

    /**
     * 修改随手记
     * 
     * @param hsmJotdown 随手记
     * @return 结果
     */
    public int updateHsmJotdown(HsmJotdown hsmJotdown);

    /**
     * 删除随手记
     * 
     * @param id 随手记主键
     * @return 结果
     */
    public int deleteHsmJotdownById(Long id);

    /**
     * 批量删除随手记
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHsmJotdownByIds(Long[] ids);

    /**
     * 查询随手记统计（总金额、总条数），不受分页影响
     *
     * @param hsmJotdown 查询条件
     * @return Map {totalAmount, totalCount}
     */
    public Map<String, Object> selectJotdownTotalStats(HsmJotdown hsmJotdown);

    /**
     * 按费用类型分组统计金额
     *
     * @param hsmJotdown 查询条件
     * @return List<Map> {expenseType, total}
     */
    public List<Map<String, Object>> selectJotdownTypeStats(HsmJotdown hsmJotdown);
}

package com.hms.main.jotdown.mapper;

import java.util.List;
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
}

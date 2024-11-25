package com.hms.main.jotdown.service;

import java.util.List;
import com.hms.main.jotdown.domain.HsmJotdown;

/**
 * 随手记Service接口
 * 
 * @author ruoyi
 * @date 2024-11-16
 */
public interface IHsmJotdownService 
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
     * 批量删除随手记
     * 
     * @param ids 需要删除的随手记主键集合
     * @return 结果
     */
    public int deleteHsmJotdownByIds(Long[] ids);

    /**
     * 删除随手记信息
     * 
     * @param id 随手记主键
     * @return 结果
     */
    public int deleteHsmJotdownById(Long id);
}

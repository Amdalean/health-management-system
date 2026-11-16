package com.hms.main.jotdown.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.common.annotation.DataScope;
import com.hms.main.jotdown.mapper.HsmJotdownMapper;
import com.hms.main.jotdown.domain.HsmJotdown;
import com.hms.main.jotdown.service.IHsmJotdownService;

/**
 * 随手记Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-11-16
 */
@Service
public class HsmJotdownServiceImpl implements IHsmJotdownService 
{
    @Autowired
    private HsmJotdownMapper hsmJotdownMapper;

    /**
     * 查询随手记
     * 
     * @param id 随手记主键
     * @return 随手记
     */
    @Override
    public HsmJotdown selectHsmJotdownById(Long id)
    {
        return hsmJotdownMapper.selectHsmJotdownById(id);
    }

    /**
     * 查询随手记列表
     * 
     * @param hsmJotdown 随手记
     * @return 随手记
     */
    @Override
    @DataScope(userAlias = "u")
    public List<HsmJotdown> selectHsmJotdownList(HsmJotdown hsmJotdown)
    {
        return hsmJotdownMapper.selectHsmJotdownList(hsmJotdown);
    }

    /**
     * 新增随手记
     * 
     * @param hsmJotdown 随手记
     * @return 结果
     */
    @Override
    public int insertHsmJotdown(HsmJotdown hsmJotdown)
    {
        return hsmJotdownMapper.insertHsmJotdown(hsmJotdown);
    }

    /**
     * 修改随手记
     * 
     * @param hsmJotdown 随手记
     * @return 结果
     */
    @Override
    public int updateHsmJotdown(HsmJotdown hsmJotdown)
    {
        return hsmJotdownMapper.updateHsmJotdown(hsmJotdown);
    }

    /**
     * 批量删除随手记
     * 
     * @param ids 需要删除的随手记主键
     * @return 结果
     */
    @Override
    public int deleteHsmJotdownByIds(Long[] ids)
    {
        return hsmJotdownMapper.deleteHsmJotdownByIds(ids);
    }

    /**
     * 删除随手记信息
     *
     * @param id 随手记主键
     * @return 结果
     */
    @Override
    public int deleteHsmJotdownById(Long id)
    {
        return hsmJotdownMapper.deleteHsmJotdownById(id);
    }

    /**
     * 查询随手记统计（总金额、总条数），不受分页影响
     *
     * add by CYQ 2026年7月2日
     */
    @Override
    @DataScope(userAlias = "u")
    public Map<String, Object> selectJotdownTotalStats(HsmJotdown hsmJotdown)
    {
        return hsmJotdownMapper.selectJotdownTotalStats(hsmJotdown);
    }

    /**
     * 按费用类型分组统计金额
     *
     * add by CYQ 2026年7月2日
     */
    @Override
    @DataScope(userAlias = "u")
    public List<Map<String, Object>> selectJotdownTypeStats(HsmJotdown hsmJotdown)
    {
        return hsmJotdownMapper.selectJotdownTypeStats(hsmJotdown);
    }
}

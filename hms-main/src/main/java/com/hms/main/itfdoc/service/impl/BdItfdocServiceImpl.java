package com.hms.main.itfdoc.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.main.itfdoc.mapper.BdItfdocMapper;
import com.hms.main.itfdoc.domain.BdItfdoc;
import com.hms.main.itfdoc.service.IBdItfdocService;

/**
 * 接口档案Service业务层处理
 * 
 * @author CYQ
 * @date 2025-04-08
 */
@Service
public class BdItfdocServiceImpl implements IBdItfdocService 
{
    @Autowired
    private BdItfdocMapper bdItfdocMapper;

    /**
     * 查询接口档案
     * 
     * @param pkItfdoc 接口档案主键
     * @return 接口档案
     */
    @Override
    public BdItfdoc selectBdItfdocByPkItfdoc(Long pkItfdoc)
    {
        return bdItfdocMapper.selectBdItfdocByPkItfdoc(pkItfdoc);
    }

    /**
     * 查询接口档案列表
     * 
     * @param bdItfdoc 接口档案
     * @return 接口档案
     */
    @Override
    public List<BdItfdoc> selectBdItfdocList(BdItfdoc bdItfdoc)
    {
        return bdItfdocMapper.selectBdItfdocList(bdItfdoc);
    }

    /**
     * 新增接口档案
     * 
     * @param bdItfdoc 接口档案
     * @return 结果
     */
    @Override
    public int insertBdItfdoc(BdItfdoc bdItfdoc)
    {
        return bdItfdocMapper.insertBdItfdoc(bdItfdoc);
    }

    /**
     * 修改接口档案
     * 
     * @param bdItfdoc 接口档案
     * @return 结果
     */
    @Override
    public int updateBdItfdoc(BdItfdoc bdItfdoc)
    {
        return bdItfdocMapper.updateBdItfdoc(bdItfdoc);
    }

    /**
     * 批量删除接口档案
     * 
     * @param pkItfdocs 需要删除的接口档案主键
     * @return 结果
     */
    @Override
    public int deleteBdItfdocByPkItfdocs(Long[] pkItfdocs)
    {
        return bdItfdocMapper.deleteBdItfdocByPkItfdocs(pkItfdocs);
    }

    /**
     * 删除接口档案信息
     * 
     * @param pkItfdoc 接口档案主键
     * @return 结果
     */
    @Override
    public int deleteBdItfdocByPkItfdoc(Long pkItfdoc)
    {
        return bdItfdocMapper.deleteBdItfdocByPkItfdoc(pkItfdoc);
    }
}

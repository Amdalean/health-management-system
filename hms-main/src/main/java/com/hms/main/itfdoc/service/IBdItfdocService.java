package com.hms.main.itfdoc.service;

import java.util.List;
import com.hms.main.itfdoc.domain.BdItfdoc;

/**
 * 接口档案Service接口
 * 
 * @author CYQ
 * @date 2025-04-08
 */
public interface IBdItfdocService 
{
    /**
     * 查询接口档案
     * 
     * @param pkItfdoc 接口档案主键
     * @return 接口档案
     */
    public BdItfdoc selectBdItfdocByPkItfdoc(Long pkItfdoc);

    /**
     * 查询接口档案列表
     * 
     * @param bdItfdoc 接口档案
     * @return 接口档案集合
     */
    public List<BdItfdoc> selectBdItfdocList(BdItfdoc bdItfdoc);

    /**
     * 新增接口档案
     * 
     * @param bdItfdoc 接口档案
     * @return 结果
     */
    public int insertBdItfdoc(BdItfdoc bdItfdoc);

    /**
     * 修改接口档案
     * 
     * @param bdItfdoc 接口档案
     * @return 结果
     */
    public int updateBdItfdoc(BdItfdoc bdItfdoc);

    /**
     * 批量删除接口档案
     * 
     * @param pkItfdocs 需要删除的接口档案主键集合
     * @return 结果
     */
    public int deleteBdItfdocByPkItfdocs(Long[] pkItfdocs);

    /**
     * 删除接口档案信息
     * 
     * @param pkItfdoc 接口档案主键
     * @return 结果
     */
    public int deleteBdItfdocByPkItfdoc(Long pkItfdoc);
}

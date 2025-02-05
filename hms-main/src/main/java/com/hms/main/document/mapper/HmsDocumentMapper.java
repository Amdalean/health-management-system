package com.hms.main.document.mapper;

import java.util.List;
import com.hms.main.document.domain.HmsDocument;

/**
 * 资料库管理Mapper接口
 * 
 * @author CYQ
 * @date 2025-01-28
 */
public interface HmsDocumentMapper 
{
    /**
     * 查询资料库管理
     * 
     * @param id 资料库管理主键
     * @return 资料库管理
     */
    public HmsDocument selectHmsDocumentById(Long id);

    /**
     * 查询资料库管理列表
     * 
     * @param hmsDocument 资料库管理
     * @return 资料库管理集合
     */
    public List<HmsDocument> selectHmsDocumentList(HmsDocument hmsDocument);

    /**
     * 新增资料库管理
     * 
     * @param hmsDocument 资料库管理
     * @return 结果
     */
    public int insertHmsDocument(HmsDocument hmsDocument);

    /**
     * 修改资料库管理
     * 
     * @param hmsDocument 资料库管理
     * @return 结果
     */
    public int updateHmsDocument(HmsDocument hmsDocument);

    /**
     * 删除资料库管理
     * 
     * @param id 资料库管理主键
     * @return 结果
     */
    public int deleteHmsDocumentById(Long id);

    /**
     * 批量删除资料库管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHmsDocumentByIds(Long[] ids);
}

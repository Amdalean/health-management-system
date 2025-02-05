package com.hms.main.document.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.main.document.mapper.HmsDocumentMapper;
import com.hms.main.document.domain.HmsDocument;
import com.hms.main.document.service.IHmsDocumentService;

/**
 * 资料库管理Service业务层处理
 * 
 * @author CYQ
 * @date 2025-01-28
 */
@Service
public class HmsDocumentServiceImpl implements IHmsDocumentService 
{
    @Autowired
    private HmsDocumentMapper hmsDocumentMapper;

    /**
     * 查询资料库管理
     * 
     * @param id 资料库管理主键
     * @return 资料库管理
     */
    @Override
    public HmsDocument selectHmsDocumentById(Long id)
    {
        return hmsDocumentMapper.selectHmsDocumentById(id);
    }

    /**
     * 查询资料库管理列表
     * 
     * @param hmsDocument 资料库管理
     * @return 资料库管理
     */
    @Override
    public List<HmsDocument> selectHmsDocumentList(HmsDocument hmsDocument)
    {
        return hmsDocumentMapper.selectHmsDocumentList(hmsDocument);
    }

    /**
     * 新增资料库管理
     * 
     * @param hmsDocument 资料库管理
     * @return 结果
     */
    @Override
    public int insertHmsDocument(HmsDocument hmsDocument)
    {
        return hmsDocumentMapper.insertHmsDocument(hmsDocument);
    }

    /**
     * 修改资料库管理
     * 
     * @param hmsDocument 资料库管理
     * @return 结果
     */
    @Override
    public int updateHmsDocument(HmsDocument hmsDocument)
    {
        return hmsDocumentMapper.updateHmsDocument(hmsDocument);
    }

    /**
     * 批量删除资料库管理
     * 
     * @param ids 需要删除的资料库管理主键
     * @return 结果
     */
    @Override
    public int deleteHmsDocumentByIds(Long[] ids)
    {
        return hmsDocumentMapper.deleteHmsDocumentByIds(ids);
    }

    /**
     * 删除资料库管理信息
     * 
     * @param id 资料库管理主键
     * @return 结果
     */
    @Override
    public int deleteHmsDocumentById(Long id)
    {
        return hmsDocumentMapper.deleteHmsDocumentById(id);
    }
}

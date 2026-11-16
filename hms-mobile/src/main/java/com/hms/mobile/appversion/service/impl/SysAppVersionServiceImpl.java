package com.hms.mobile.appversion.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.mobile.appversion.mapper.SysAppVersionMapper;
import com.hms.mobile.appversion.domain.SysAppVersion;
import com.hms.mobile.appversion.service.ISysAppVersionService;

/**
 * APP版本管理 服务层实现
 *
 * @author ruoyi
 */
@Service
public class SysAppVersionServiceImpl implements ISysAppVersionService
{
    @Autowired
    private SysAppVersionMapper appVersionMapper;

    /**
     * 查询APP版本信息
     *
     * @param id 主键ID
     * @return APP版本信息
     */
    @Override
    public SysAppVersion selectAppVersionById(Long id)
    {
        return appVersionMapper.selectAppVersionById(id);
    }

    /**
     * 查询最新版本
     *
     * @param platform 平台
     * @return 最新版本信息
     */
    @Override
    public SysAppVersion selectLatestAppVersion(String platform)
    {
        return appVersionMapper.selectLatestAppVersion(platform);
    }

    /**
     * 查询APP版本列表
     *
     * @param appVersion APP版本信息
     * @return APP版本集合
     */
    @Override
    public List<SysAppVersion> selectAppVersionList(SysAppVersion appVersion)
    {
        return appVersionMapper.selectAppVersionList(appVersion);
    }

    /**
     * 新增APP版本
     *
     * @param appVersion APP版本信息
     * @return 结果
     */
    @Override
    public int insertAppVersion(SysAppVersion appVersion)
    {
        return appVersionMapper.insertAppVersion(appVersion);
    }

    /**
     * 修改APP版本
     *
     * @param appVersion APP版本信息
     * @return 结果
     */
    @Override
    public int updateAppVersion(SysAppVersion appVersion)
    {
        return appVersionMapper.updateAppVersion(appVersion);
    }

    /**
     * 删除APP版本
     *
     * @param id 主键ID
     * @return 结果
     */
    @Override
    public int deleteAppVersionById(Long id)
    {
        return appVersionMapper.deleteAppVersionById(id);
    }

    /**
     * 批量删除APP版本
     *
     * @param ids 需要删除的ID
     * @return 结果
     */
    @Override
    public int deleteAppVersionByIds(Long[] ids)
    {
        return appVersionMapper.deleteAppVersionByIds(ids);
    }
}

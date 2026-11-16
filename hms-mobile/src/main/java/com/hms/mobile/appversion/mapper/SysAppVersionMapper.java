package com.hms.mobile.appversion.mapper;

import java.util.List;
import com.hms.mobile.appversion.domain.SysAppVersion;

/**
 * APP版本管理 数据层
 *
 * @author ruoyi
 */
public interface SysAppVersionMapper
{
    /**
     * 查询APP版本信息
     *
     * @param appVersion APP版本信息
     * @return APP版本信息
     */
    public SysAppVersion selectAppVersion(SysAppVersion appVersion);

    /**
     * 通过ID查询
     *
     * @param id 主键ID
     * @return APP版本信息
     */
    public SysAppVersion selectAppVersionById(Long id);

    /**
     * 查询最新版本
     *
     * @param platform 平台
     * @return 最新版本信息
     */
    public SysAppVersion selectLatestAppVersion(String platform);

    /**
     * 查询APP版本列表
     *
     * @param appVersion APP版本信息
     * @return APP版本集合
     */
    public List<SysAppVersion> selectAppVersionList(SysAppVersion appVersion);

    /**
     * 新增APP版本
     *
     * @param appVersion APP版本信息
     * @return 结果
     */
    public int insertAppVersion(SysAppVersion appVersion);

    /**
     * 修改APP版本
     *
     * @param appVersion APP版本信息
     * @return 结果
     */
    public int updateAppVersion(SysAppVersion appVersion);

    /**
     * 删除APP版本
     *
     * @param id 主键ID
     * @return 结果
     */
    public int deleteAppVersionById(Long id);

    /**
     * 批量删除APP版本
     *
     * @param ids 需要删除的ID
     * @return 结果
     */
    public int deleteAppVersionByIds(Long[] ids);
}

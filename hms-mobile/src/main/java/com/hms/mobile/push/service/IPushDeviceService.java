package com.hms.mobile.push.service;

import java.util.List;
import com.hms.mobile.push.domain.PushDevice;

/**
 * 推送设备信息 服务层
 *
 * @author ruoyi
 */
public interface IPushDeviceService {
    /**
     * 查询推送设备信息
     *
     * @param id 主键ID
     * @return 推送设备信息
     */
    public PushDevice selectPushDeviceById(Long id);

    /**
     * 根据客户端ID查询推送设备信息
     *
     * @param clientId 客户端ID
     * @return 推送设备信息
     */
    public PushDevice selectPushDeviceByClientId(String clientId);

    /**
     * 查询推送设备信息列表
     *
     * @param pushDevice 推送设备信息
     * @return 推送设备信息集合
     */
    public List<PushDevice> selectPushDeviceList(PushDevice pushDevice);

    /**
     * 新增推送设备信息
     *
     * @param pushDevice 推送设备信息
     * @return 结果
     */
    public int insertPushDevice(PushDevice pushDevice);

    /**
     * 修改推送设备信息
     *
     * @param pushDevice 推送设备信息
     * @return 结果
     */
    public int updatePushDevice(PushDevice pushDevice);

    /**
     * 删除推送设备信息
     *
     * @param id 主键ID
     * @return 结果
     */
    public int deletePushDeviceById(Long id);

    /**
     * 批量删除推送设备信息
     *
     * @param ids 需要删除的ID
     * @return 结果
     */
    public int deletePushDeviceByIds(Long[] ids);

    /**
     * 绑定微信小程序 openid
     * add by CYQ 2026-06-10
     */
    public int bindWxOpenid(PushDevice pushDevice);
}

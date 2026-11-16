package com.hms.mobile.push.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.hms.mobile.push.domain.PushDevice;

/**
 * 推送设备信息 数据层
 *
 * @author ruoyi
 */
public interface PushDeviceMapper {

    public PushDevice selectPushDeviceById(Long id);

    public PushDevice selectPushDeviceByClientId(String clientId);

    public List<PushDevice> selectPushDeviceList(PushDevice pushDevice);

    public int insertPushDevice(PushDevice pushDevice);

    public int updatePushDevice(PushDevice pushDevice);

    public int deletePushDeviceById(Long id);

    public int deletePushDeviceByIds(Long[] ids);

    /**
     * 根据用户ID查询推送设备
     * add by CYQ 2026-06-10
     */
    public List<PushDevice> selectPushDeviceByUserId(Long userId);

    /**
     * 绑定/更新微信小程序 openid
     * add by CYQ 2026-06-10
     */
    public int bindWxOpenid(PushDevice pushDevice);

    /**
     * 根据微信小程序 openid 查询最新一条设备绑定（微信一键登录用）
     * add by CYQ 2026-09-06 微信小程序一键登录：按 openid 反查用户
     */
    public PushDevice selectPushDeviceByWxOpenid(String wxOpenid);

    /**
     * 清除指定 openid 在其他用户下的绑定（账号换绑微信时用）
     * add by CYQ 2026-09-06 微信小程序一键登录：保证 openid 同一时间只归属一个用户
     */
    public int clearWxOpenid(@Param("wxOpenid") String wxOpenid, @Param("userId") Long userId);

    /**
     * 按用户ID清除推送设备表微信绑定（设置页手动解绑用）
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     */
    public int clearWxOpenidByUserId(Long userId);
}

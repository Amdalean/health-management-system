package com.hms.mobile.push.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.mobile.push.mapper.PushDeviceMapper;
import com.hms.mobile.push.domain.PushDevice;
import com.hms.mobile.push.service.IPushDeviceService;

/**
 * 推送设备信息 服务层实现
 *
 * @author ruoyi
 */
@Service
public class PushDeviceServiceImpl implements IPushDeviceService {
    @Autowired
    private PushDeviceMapper pushDeviceMapper;

    @Override
    public PushDevice selectPushDeviceById(Long id) {
        return pushDeviceMapper.selectPushDeviceById(id);
    }

    @Override
    public PushDevice selectPushDeviceByClientId(String clientId) {
        return pushDeviceMapper.selectPushDeviceByClientId(clientId);
    }

    @Override
    public List<PushDevice> selectPushDeviceList(PushDevice pushDevice) {
        return pushDeviceMapper.selectPushDeviceList(pushDevice);
    }

    @Override
    public int insertPushDevice(PushDevice pushDevice) {
        return pushDeviceMapper.insertPushDevice(pushDevice);
    }

    @Override
    public int updatePushDevice(PushDevice pushDevice) {
        return pushDeviceMapper.updatePushDevice(pushDevice);
    }

    @Override
    public int deletePushDeviceById(Long id) {
        return pushDeviceMapper.deletePushDeviceById(id);
    }

    @Override
    public int deletePushDeviceByIds(Long[] ids) {
        return pushDeviceMapper.deletePushDeviceByIds(ids);
    }

    /**
     * 绑定微信小程序 openid
     * add by CYQ 2026-06-10
     */
    @Override
    public int bindWxOpenid(PushDevice pushDevice) {
        return pushDeviceMapper.bindWxOpenid(pushDevice);
    }
}

package com.hms.mobile.officelocation.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.mobile.officelocation.domain.OfficeLocation;
import com.hms.mobile.officelocation.mapper.OfficeLocationMapper;
import com.hms.mobile.officelocation.service.IOfficeLocationService;

/**
 * 办公地点 服务层实现
 * add by CYQ 2026-06-06 说明: 办公地点业务实现
 */
@Service
public class OfficeLocationServiceImpl implements IOfficeLocationService {

    @Autowired
    private OfficeLocationMapper officeLocationMapper;

    @Override
    public OfficeLocation selectOfficeLocationById(Long id) {
        return officeLocationMapper.selectOfficeLocationById(id);
    }

    @Override
    public List<OfficeLocation> selectOfficeLocationList(OfficeLocation officeLocation) {
        return officeLocationMapper.selectOfficeLocationList(officeLocation);
    }

    @Override
    public List<OfficeLocation> selectAllEnabledLocations() {
        return officeLocationMapper.selectAllEnabledLocations();
    }

    @Override
    public int insertOfficeLocation(OfficeLocation officeLocation) {
        return officeLocationMapper.insertOfficeLocation(officeLocation);
    }

    @Override
    public int updateOfficeLocation(OfficeLocation officeLocation) {
        return officeLocationMapper.updateOfficeLocation(officeLocation);
    }

    @Override
    public int deleteOfficeLocationById(Long id) {
        return officeLocationMapper.deleteOfficeLocationById(id);
    }

    @Override
    public int deleteOfficeLocationByIds(Long[] ids) {
        return officeLocationMapper.deleteOfficeLocationByIds(ids);
    }
}

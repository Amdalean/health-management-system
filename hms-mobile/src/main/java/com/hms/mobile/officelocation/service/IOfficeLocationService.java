package com.hms.mobile.officelocation.service;

import java.util.List;
import com.hms.mobile.officelocation.domain.OfficeLocation;

/**
 * 办公地点 服务层
 * add by CYQ 2026-06-06 说明: 办公地点业务接口
 * @author 崔永琦
 */
public interface IOfficeLocationService {

    public OfficeLocation selectOfficeLocationById(Long id);

    public List<OfficeLocation> selectOfficeLocationList(OfficeLocation officeLocation);

    public List<OfficeLocation> selectAllEnabledLocations();

    public int insertOfficeLocation(OfficeLocation officeLocation);

    public int updateOfficeLocation(OfficeLocation officeLocation);

    public int deleteOfficeLocationById(Long id);

    public int deleteOfficeLocationByIds(Long[] ids);
}

package com.hms.mobile.officelocation.mapper;

import java.util.List;
import com.hms.mobile.officelocation.domain.OfficeLocation;

/**
 * 办公地点 数据层
 * add by CYQ 2026-06-06 说明: 办公地点数据访问接口
 * @author 崔永琦
 */
public interface OfficeLocationMapper {

    public OfficeLocation selectOfficeLocationById(Long id);

    public List<OfficeLocation> selectOfficeLocationList(OfficeLocation officeLocation);

    public List<OfficeLocation> selectAllEnabledLocations();

    public int insertOfficeLocation(OfficeLocation officeLocation);

    public int updateOfficeLocation(OfficeLocation officeLocation);

    public int deleteOfficeLocationById(Long id);

    public int deleteOfficeLocationByIds(Long[] ids);
}

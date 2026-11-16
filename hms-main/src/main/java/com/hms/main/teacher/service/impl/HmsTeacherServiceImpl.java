package com.hms.main.teacher.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.main.teacher.mapper.HmsTeacherMapper;
import com.hms.main.teacher.domain.HmsTeacher;
import com.hms.main.teacher.service.IHmsTeacherService;

/**
 * 教师建档 Service 业务层处理
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 教师建档业务层实现，委托 Mapper 完成数据访问。
 */
@Service
public class HmsTeacherServiceImpl implements IHmsTeacherService
{
    @Autowired
    private HmsTeacherMapper hmsTeacherMapper;

    /**
     * 查询教师信息
     *
     * @param teacherId 教师信息主键
     * @return 教师信息
     */
    @Override
    public HmsTeacher selectHmsTeacherById(Long teacherId)
    {
        return hmsTeacherMapper.selectHmsTeacherById(teacherId);
    }

    /**
     * 按工号查询教师信息
     *
     * @param staffNo 教师工号
     * @return 教师信息
     */
    @Override
    public HmsTeacher selectHmsTeacherByNo(String staffNo)
    {
        return hmsTeacherMapper.selectHmsTeacherByNo(staffNo);
    }

    /**
     * 查询教师信息列表
     *
     * @param hmsTeacher 教师信息
     * @return 教师信息集合
     */
    @Override
    public List<HmsTeacher> selectHmsTeacherList(HmsTeacher hmsTeacher)
    {
        return hmsTeacherMapper.selectHmsTeacherList(hmsTeacher);
    }

    /**
     * 新增教师信息
     *
     * @param hmsTeacher 教师信息
     * @return 结果
     */
    @Override
    public int insertHmsTeacher(HmsTeacher hmsTeacher)
    {
        return hmsTeacherMapper.insertHmsTeacher(hmsTeacher);
    }

    /**
     * 修改教师信息
     *
     * @param hmsTeacher 教师信息
     * @return 结果
     */
    @Override
    public int updateHmsTeacher(HmsTeacher hmsTeacher)
    {
        return hmsTeacherMapper.updateHmsTeacher(hmsTeacher);
    }

    /**
     * 批量删除教师信息
     *
     * @param teacherIds 需要删除的教师信息主键集合
     * @return 结果
     */
    @Override
    public int deleteHmsTeacherByIds(Long[] teacherIds)
    {
        return hmsTeacherMapper.deleteHmsTeacherByIds(teacherIds);
    }

    /**
     * 删除教师信息
     *
     * @param teacherId 教师信息主键
     * @return 结果
     */
    @Override
    public int deleteHmsTeacherById(Long teacherId)
    {
        return hmsTeacherMapper.deleteHmsTeacherById(teacherId);
    }
}

package com.hms.main.student.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.main.student.mapper.HmsStudentMapper;
import com.hms.main.student.domain.HmsStudent;
import com.hms.main.student.service.IHmsStudentService;

/**
 * 学生信息Service业务层处理
 * 
 * @author CYQ
 * @date 2024-09-13
 */
@Service
public class HmsStudentServiceImpl implements IHmsStudentService 
{
    @Autowired
    private HmsStudentMapper hmsStudentMapper;

    /**
     * 查询学生信息
     * 
     * @param id 学生信息主键
     * @return 学生信息
     */
    @Override
    public HmsStudent selectHmsStudentById(Long id)
    {
        return hmsStudentMapper.selectHmsStudentById(id);
    }

    /**
     * 查询学生信息列表
     * 
     * @param hmsStudent 学生信息
     * @return 学生信息
     */
    @Override
    public List<HmsStudent> selectHmsStudentList(HmsStudent hmsStudent)
    {
        return hmsStudentMapper.selectHmsStudentList(hmsStudent);
    }

    /**
     * 新增学生信息
     * 
     * @param hmsStudent 学生信息
     * @return 结果
     */
    @Override
    public int insertHmsStudent(HmsStudent hmsStudent)
    {
        return hmsStudentMapper.insertHmsStudent(hmsStudent);
    }

    /**
     * 修改学生信息
     * 
     * @param hmsStudent 学生信息
     * @return 结果
     */
    @Override
    public int updateHmsStudent(HmsStudent hmsStudent)
    {
        return hmsStudentMapper.updateHmsStudent(hmsStudent);
    }

    /**
     * 批量删除学生信息
     * 
     * @param ids 需要删除的学生信息主键
     * @return 结果
     */
    @Override
    public int deleteHmsStudentByIds(Long[] ids)
    {
        return hmsStudentMapper.deleteHmsStudentByIds(ids);
    }

    /**
     * 删除学生信息信息
     * 
     * @param id 学生信息主键
     * @return 结果
     */
    @Override
    public int deleteHmsStudentById(Long id)
    {
        return hmsStudentMapper.deleteHmsStudentById(id);
    }
}

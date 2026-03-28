package com.hms.main.student.mapper;

import java.util.List;
import com.hms.main.student.domain.HmsStudent;

/**
 * 学生信息Mapper接口
 * 
 * @author CYQ
 * @date 2024-09-13
 */
public interface HmsStudentMapper 
{
    /**
     * 查询学生信息
     * 
     * @param id 学生信息主键
     * @return 学生信息
     */
    public HmsStudent selectHmsStudentById(Long id);

    /**
     * 查询学生信息列表
     * 
     * @param hmsStudent 学生信息
     * @return 学生信息集合
     */
    public List<HmsStudent> selectHmsStudentList(HmsStudent hmsStudent);

    /**
     * 新增学生信息
     * 
     * @param hmsStudent 学生信息
     * @return 结果
     */
    public int insertHmsStudent(HmsStudent hmsStudent);

    /**
     * 修改学生信息
     * 
     * @param hmsStudent 学生信息
     * @return 结果
     */
    public int updateHmsStudent(HmsStudent hmsStudent);

    /**
     * 删除学生信息
     * 
     * @param id 学生信息主键
     * @return 结果
     */
    public int deleteHmsStudentById(Long id);

    /**
     * 批量删除学生信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHmsStudentByIds(Long[] ids);
}

package com.hms.main.student.service;

import java.util.List;
import com.hms.main.student.domain.HmsStudent;

/**
 * 学生建档 Service 接口
 *
 * @author 崔永琦
 * @date 2024-09-13
 * add by CYQ 2026-10-18 说明: 配合论文表1重建学生建档模块，补充按学号查询（唯一性校验用）。
 */
public interface IHmsStudentService
{
    /**
     * 查询学生信息
     *
     * @param studentId 学生信息主键
     * @return 学生信息
     */
    public HmsStudent selectHmsStudentById(Long studentId);

    /**
     * 按学号查询学生信息（用于新增时的学号唯一性校验）
     *
     * @param studentNo 学号
     * @return 学生信息
     */
    public HmsStudent selectHmsStudentByNo(String studentNo);

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
     * 批量删除学生信息
     *
     * @param studentIds 需要删除的学生信息主键集合
     * @return 结果
     */
    public int deleteHmsStudentByIds(Long[] studentIds);

    /**
     * 删除学生信息
     *
     * @param studentId 学生信息主键
     * @return 结果
     */
    public int deleteHmsStudentById(Long studentId);
}

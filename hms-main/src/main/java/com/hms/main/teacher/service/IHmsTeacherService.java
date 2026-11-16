package com.hms.main.teacher.service;

import java.util.List;
import com.hms.main.teacher.domain.HmsTeacher;

/**
 * 教师建档 Service 接口
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 教师建档业务层接口，与 Mapper 方法一一对应。
 */
public interface IHmsTeacherService
{
    /**
     * 查询教师信息
     *
     * @param teacherId 教师信息主键
     * @return 教师信息
     */
    public HmsTeacher selectHmsTeacherById(Long teacherId);

    /**
     * 按工号查询教师信息（用于新增时的工号唯一性校验）
     *
     * @param staffNo 教师工号
     * @return 教师信息
     */
    public HmsTeacher selectHmsTeacherByNo(String staffNo);

    /**
     * 查询教师信息列表
     *
     * @param hmsTeacher 教师信息
     * @return 教师信息集合
     */
    public List<HmsTeacher> selectHmsTeacherList(HmsTeacher hmsTeacher);

    /**
     * 新增教师信息
     *
     * @param hmsTeacher 教师信息
     * @return 结果
     */
    public int insertHmsTeacher(HmsTeacher hmsTeacher);

    /**
     * 修改教师信息
     *
     * @param hmsTeacher 教师信息
     * @return 结果
     */
    public int updateHmsTeacher(HmsTeacher hmsTeacher);

    /**
     * 批量删除教师信息
     *
     * @param teacherIds 需要删除的教师信息主键集合
     * @return 结果
     */
    public int deleteHmsTeacherByIds(Long[] teacherIds);

    /**
     * 删除教师信息
     *
     * @param teacherId 教师信息主键
     * @return 结果
     */
    public int deleteHmsTeacherById(Long teacherId);
}

package com.hms.main.teacher.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.hms.main.teacher.domain.HmsTeacher;

/**
 * 教师建档 Mapper 接口
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 教师建档模块数据层，支撑教师增删改查与「生成账号」回写。
 */
public interface HmsTeacherMapper
{
    /**
     * 查询教师信息
     *
     * @param teacherId 教师信息主键
     * @return 教师信息
     */
    public HmsTeacher selectHmsTeacherById(Long teacherId);

    /**
     * 按工号查询教师信息
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
     * 回写教师关联的系统账号 ID
     *
     * @param teacherId 教师ID
     * @param userId    系统用户ID
     * @return 结果
     */
    public int updateTeacherUserId(@Param("teacherId") Long teacherId, @Param("userId") Long userId);

    /**
     * 删除教师信息
     *
     * @param teacherId 教师信息主键
     * @return 结果
     */
    public int deleteHmsTeacherById(Long teacherId);

    /**
     * 批量删除教师信息
     *
     * @param teacherIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHmsTeacherByIds(Long[] teacherIds);
}

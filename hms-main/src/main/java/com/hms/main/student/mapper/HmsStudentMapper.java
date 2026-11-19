package com.hms.main.student.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.hms.main.student.domain.HmsStudent;

/**
 * 学生建档 Mapper 接口
 *
 * @author 崔永琦
 * @date 2024-09-13
 * add by CYQ 2026-10-18 说明: 配合论文表1重建，补充按学号查询与回写系统账号 ID 的方法。
 */
public interface HmsStudentMapper
{
    /**
     * 查询学生信息
     *
     * @param studentId 学生信息主键
     * @return 学生信息
     */
    public HmsStudent selectHmsStudentById(Long studentId);

    /**
     * 按学号查询学生信息
     *
     * @param studentNo 学号
     * @return 学生信息
     */
    public HmsStudent selectHmsStudentByNo(String studentNo);

    /**
     * 按关联的系统用户ID查询学生信息
     * add by CYQ 2026-10-18 说明: 供「成绩智能分析」等模块把登录账号解析为学生档案
     *
     * @param userId 系统用户ID
     * @return 学生信息
     */
    public HmsStudent selectHmsStudentByUserId(Long userId);

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
     * 回写学生关联的系统账号 ID
     *
     * @param studentId 学生ID
     * @param userId    系统用户ID
     * @return 结果
     */
    public int updateStudentUserId(@Param("studentId") Long studentId, @Param("userId") Long userId);

    /**
     * 删除学生信息
     *
     * @param studentId 学生信息主键
     * @return 结果
     */
    public int deleteHmsStudentById(Long studentId);

    /**
     * 批量删除学生信息
     *
     * @param studentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHmsStudentByIds(Long[] studentIds);
}

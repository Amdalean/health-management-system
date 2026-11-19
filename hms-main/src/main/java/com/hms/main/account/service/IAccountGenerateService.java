package com.hms.main.account.service;

import com.hms.main.account.domain.UserGenerateResult;

/**
 * 系统账号生成 Service 接口
 *
 * @author 崔永琦
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 根据学生学号 / 教师工号为建档记录生成 sys_user 系统账号，
 *   并回写 hms_student.user_id / hms_teacher.user_id，实现「建档 → 开通账号」一体化。
 *   用户名 = 学号 / 工号；初始密码取自系统配置 sys.user.initPassword（默认 123456）；
 *   角色自动分配为「学生」/「教师」。
 */
public interface IAccountGenerateService
{
    /**
     * 为单个学生生成系统账号
     *
     * @param studentId 学生ID
     * @return 生成的系统用户ID
     */
    public Long generateStudentAccount(Long studentId);

    /**
     * 批量为学生生成系统账号（逐条独立事务，允许部分成功）
     *
     * @param studentIds 学生ID集合
     * @return 生成结果统计
     */
    public UserGenerateResult generateStudentAccounts(Long[] studentIds);

    /**
     * 为单个教师生成系统账号
     *
     * @param teacherId 教师ID
     * @return 生成的系统用户ID
     */
    public Long generateTeacherAccount(Long teacherId);

    /**
     * 批量为教师生成系统账号（逐条独立事务，允许部分成功）
     *
     * @param teacherIds 教师ID集合
     * @return 生成结果统计
     */
    public UserGenerateResult generateTeacherAccounts(Long[] teacherIds);
}

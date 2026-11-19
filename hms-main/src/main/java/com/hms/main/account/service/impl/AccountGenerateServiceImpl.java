package com.hms.main.account.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hms.common.core.domain.entity.SysRole;
import com.hms.common.core.domain.entity.SysUser;
import com.hms.common.exception.ServiceException;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.spring.SpringUtils;
import com.hms.main.account.domain.UserGenerateResult;
import com.hms.main.account.service.IAccountGenerateService;
import com.hms.main.student.domain.HmsStudent;
import com.hms.main.student.mapper.HmsStudentMapper;
import com.hms.main.teacher.domain.HmsTeacher;
import com.hms.main.teacher.mapper.HmsTeacherMapper;
import com.hms.system.mapper.SysRoleMapper;
import com.hms.system.service.ISysConfigService;
import com.hms.system.service.ISysUserService;

/**
 * 系统账号生成 Service 业务层处理
 *
 * @author 崔永琦
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 「生成账号」核心实现。
 *   1) 用户名 = 学生学号 / 教师工号，初始密码取系统配置 sys.user.initPassword（默认 123456）；
 *   2) 自动确保「学生」「教师」角色存在（不存在则创建），并授予该角色；
 *   3) 账号创建成功后回写 hms_student.user_id / hms_teacher.user_id，实现幂等（已生成则拒绝重复生成）；
 *   4) 批量生成时逐条独立事务，允许部分成功并汇总失败原因。
 */
@Service
public class AccountGenerateServiceImpl implements IAccountGenerateService
{
    private static final Logger log = LoggerFactory.getLogger(AccountGenerateServiceImpl.class);

    /** 学生角色标识 */
    private static final String ROLE_KEY_STUDENT = "student";

    /** 教师角色标识 */
    private static final String ROLE_KEY_TEACHER = "teacher";

    /** 默认初始密码（系统配置缺失时兜底） */
    private static final String DEFAULT_INIT_PASSWORD = "123456";

    @Autowired
    private HmsStudentMapper hmsStudentMapper;

    @Autowired
    private HmsTeacherMapper hmsTeacherMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    /** 系统用户初始密码配置项 */
    private static final String CONFIG_INIT_PASSWORD = "sys.user.initPassword";

    /**
     * 为单个学生生成系统账号
     *
     * @param studentId 学生ID
     * @return 生成的系统用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateStudentAccount(Long studentId)
    {
        HmsStudent student = hmsStudentMapper.selectHmsStudentById(studentId);
        if (student == null)
        {
            throw new ServiceException("学生不存在，ID：" + studentId);
        }
        if (student.getUserId() != null)
        {
            throw new ServiceException("学生【" + student.getName() + "】已生成系统账号，无需重复生成");
        }
        Long userId = createAccount(student.getStudentNo(), student.getName(), student.getPhone(),
                student.getGender(), ROLE_KEY_STUDENT, "学生");
        hmsStudentMapper.updateStudentUserId(studentId, userId);
        log.info("为学生生成系统账号成功：studentId={}, studentNo={}, userId={}", studentId, student.getStudentNo(), userId);
        return userId;
    }

    /**
     * 批量为学生生成系统账号（逐条独立事务）
     *
     * @param studentIds 学生ID集合
     * @return 生成结果统计
     */
    @Override
    public UserGenerateResult generateStudentAccounts(Long[] studentIds)
    {
        UserGenerateResult result = new UserGenerateResult(studentIds == null ? 0 : studentIds.length);
        if (studentIds == null || studentIds.length == 0)
        {
            result.addFailure("未选择任何学生记录");
            return result;
        }
        for (Long studentId : studentIds)
        {
            try
            {
                // 通过 AOP 代理调用，保证每条记录独立开启/提交事务
                SpringUtils.getAopProxy(this).generateStudentAccount(studentId);
                HmsStudent student = hmsStudentMapper.selectHmsStudentById(studentId);
                result.addSuccess("学生【" + student.getName() + "】账号 " + student.getStudentNo() + " 已生成");
            }
            catch (ServiceException e)
            {
                result.addFailure("学生ID " + studentId + "：" + e.getMessage());
            }
            catch (Exception e)
            {
                log.error("为学生生成系统账号失败：studentId=" + studentId, e);
                result.addFailure("学生ID " + studentId + "：" + e.getMessage());
            }
        }
        return result;
    }

    /**
     * 为单个教师生成系统账号
     *
     * @param teacherId 教师ID
     * @return 生成的系统用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateTeacherAccount(Long teacherId)
    {
        HmsTeacher teacher = hmsTeacherMapper.selectHmsTeacherById(teacherId);
        if (teacher == null)
        {
            throw new ServiceException("教师不存在，ID：" + teacherId);
        }
        if (teacher.getUserId() != null)
        {
            throw new ServiceException("教师【" + teacher.getName() + "】已生成系统账号，无需重复生成");
        }
        Long userId = createAccount(teacher.getStaffNo(), teacher.getName(), teacher.getPhone(),
                teacher.getGender(), ROLE_KEY_TEACHER, "教师");
        hmsTeacherMapper.updateTeacherUserId(teacherId, userId);
        log.info("为教师生成系统账号成功：teacherId={}, staffNo={}, userId={}", teacherId, teacher.getStaffNo(), userId);
        return userId;
    }

    /**
     * 批量为教师生成系统账号（逐条独立事务）
     *
     * @param teacherIds 教师ID集合
     * @return 生成结果统计
     */
    @Override
    public UserGenerateResult generateTeacherAccounts(Long[] teacherIds)
    {
        UserGenerateResult result = new UserGenerateResult(teacherIds == null ? 0 : teacherIds.length);
        if (teacherIds == null || teacherIds.length == 0)
        {
            result.addFailure("未选择任何教师记录");
            return result;
        }
        for (Long teacherId : teacherIds)
        {
            try
            {
                SpringUtils.getAopProxy(this).generateTeacherAccount(teacherId);
                HmsTeacher teacher = hmsTeacherMapper.selectHmsTeacherById(teacherId);
                result.addSuccess("教师【" + teacher.getName() + "】账号 " + teacher.getStaffNo() + " 已生成");
            }
            catch (ServiceException e)
            {
                result.addFailure("教师ID " + teacherId + "：" + e.getMessage());
            }
            catch (Exception e)
            {
                log.error("为教师生成系统账号失败：teacherId=" + teacherId, e);
                result.addFailure("教师ID " + teacherId + "：" + e.getMessage());
            }
        }
        return result;
    }

    /**
     * 创建系统账号（学生/教师共用）
     *
     * @param userName 用户名（学号 / 工号）
     * @param nickName 昵称（姓名）
     * @param phone    手机号
     * @param gender   性别（0=男 1=女 2=未知）
     * @param roleKey  角色标识
     * @param roleName 角色名称
     * @return 生成的系统用户ID
     */
    private Long createAccount(String userName, String nickName, String phone, String gender,
                               String roleKey, String roleName)
    {
        if (StringUtils.isEmpty(userName))
        {
            throw new ServiceException("学号/工号为空，无法生成账号");
        }
        if (sysUserService.selectUserByUserName(userName) != null)
        {
            throw new ServiceException("用户名【" + userName + "】已被占用，请先核对学号/工号");
        }
        Long roleId = ensureRole(roleKey, roleName);

        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setNickName(StringUtils.isEmpty(nickName) ? userName : nickName);
        user.setPhonenumber(phone);
        user.setSex(StringUtils.isEmpty(gender) ? "2" : gender);
        user.setStatus("0");
        user.setPassword(SecurityUtils.encryptPassword(getInitPassword()));
        user.setRoleIds(new Long[] { roleId });
        user.setPostIds(new Long[] {});
        user.setCreateBy(SecurityUtils.getUsername());
        user.setRemark("由「" + roleName + "建档」生成账号，角色：" + roleName);

        sysUserService.insertUser(user);
        if (user.getUserId() == null)
        {
            throw new ServiceException("账号创建失败，用户名：" + userName);
        }
        return user.getUserId();
    }

    /**
     * 确保角色存在，返回角色ID（不存在则自动创建）
     *
     * @param roleKey  角色标识
     * @param roleName 角色名称
     * @return 角色ID
     */
    private Long ensureRole(String roleKey, String roleName)
    {
        SysRole role = sysRoleMapper.checkRoleKeyUnique(roleKey);
        if (role != null && role.getRoleId() != null)
        {
            return role.getRoleId();
        }
        SysRole add = new SysRole();
        add.setRoleName(roleName);
        add.setRoleKey(roleKey);
        add.setRoleSort(10);
        add.setDataScope("1");
        add.setMenuCheckStrictly(true);
        add.setDeptCheckStrictly(true);
        add.setStatus("0");
        add.setCreateBy(SecurityUtils.getUsername());
        add.setRemark("系统自动创建：生成" + roleName + "账号时分配");
        sysRoleMapper.insertRole(add);
        if (add.getRoleId() == null)
        {
            // 并发场景下可能已被其他线程创建，重新查询一次
            SysRole again = sysRoleMapper.checkRoleKeyUnique(roleKey);
            if (again == null || again.getRoleId() == null)
            {
                throw new ServiceException("角色【" + roleName + "】创建失败");
            }
            return again.getRoleId();
        }
        return add.getRoleId();
    }

    /**
     * 读取系统配置的初始密码
     *
     * @return 初始密码
     */
    private String getInitPassword()
    {
        String password = sysConfigService.selectConfigByKey(CONFIG_INIT_PASSWORD);
        return StringUtils.isEmpty(password) ? DEFAULT_INIT_PASSWORD : password;
    }
}

package com.hms.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.hms.common.core.domain.entity.SysUser;

/**
 * 用户表 数据层
 * 
 * @author ruoyi
 */
public interface SysUserMapper
{
    /**
     * 根据条件分页查询用户列表
     * 
     * @param sysUser 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser sysUser);

    /**
     * 根据条件分页查询已配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    public SysUser selectUserByUserName(String userName);

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public SysUser selectUserById(Long userId);

    /**
     * 通过微信小程序openid查询用户（微信一键登录用）
     * add by CYQ 2026-09-06 微信小程序一键登录
     *
     * @param wxOpenid 微信小程序openid
     * @return 用户对象信息
     */
    public SysUser selectUserByWxOpenid(String wxOpenid);

    /**
     * 清除指定 openid 在其他用户下的绑定（账号换绑微信时用）
     * add by CYQ 2026-09-06 微信小程序一键登录
     *
     * @param user 用户信息（userId 保留归属用户，wxOpenid 待解绑的openid）
     * @return 结果
     */
    public int clearUserWxOpenid(SysUser user);

    /**
     * 按用户ID解绑微信小程序openid（设置页手动解绑用）
     * add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int clearWxOpenidByUserId(Long userId);

    /**
     * 新增用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(SysUser user);

    /**
     * 修改用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(SysUser user);

    /**
     * 修改用户头像
     * 
     * @param userName 用户名
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userName") String userName, @Param("avatar") String avatar);

    /**
     * 更新用户当前所在城市（移动端定位上报）
     * add by Qwen 2026-10-26
     *
     * @param user 用户（userId + 城市字段）
     * @return 结果
     */
    public int updateUserLocation(SysUser user);

    /**
     * 重置用户密码
     * 
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userName") String userName, @Param("password") String password);

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     * 
     * @param userName 用户名称
     * @return 结果
     */
    public SysUser checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public SysUser checkEmailUnique(String email);
}

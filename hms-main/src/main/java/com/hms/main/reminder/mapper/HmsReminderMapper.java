package com.hms.main.reminder.mapper;

import java.util.List;
import com.hms.main.reminder.domain.HmsReminder;

/**
 * 提醒管理Mapper接口
 * 
 * @author CYQ
 * @date 2026-01-26
 */
public interface HmsReminderMapper 
{
    /**
     * 查询提醒管理
     * 
     * @param id 提醒管理主键
     * @return 提醒管理
     */
    public HmsReminder selectHmsReminderById(Long id);

    /**
     * 查询提醒管理列表
     * 
     * @param hmsReminder 提醒管理
     * @return 提醒管理集合
     */
    public List<HmsReminder> selectHmsReminderList(HmsReminder hmsReminder);

    /**
     * 根据ID查询用户名
     * */
    public String selectUserNameById(String id);
    /**
     * 新增提醒管理
     * 
     * @param hmsReminder 提醒管理
     * @return 结果
     */
    public int insertHmsReminder(HmsReminder hmsReminder);

    /**
     * 修改提醒管理
     * 
     * @param hmsReminder 提醒管理
     * @return 结果
     */
    public int updateHmsReminder(HmsReminder hmsReminder);

    /**
     * 删除提醒管理
     * 
     * @param id 提醒管理主键
     * @return 结果
     */
    public int deleteHmsReminderById(Long id);

    /**
     * 批量删除提醒管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteHmsReminderByIds(Long[] ids);
}

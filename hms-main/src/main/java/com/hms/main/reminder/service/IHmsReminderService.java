package com.hms.main.reminder.service;

import java.util.List;
import com.hms.main.reminder.domain.HmsReminder;
import com.hms.main.reminder.domain.HmsReminderVO;

/**
 * 提醒管理Service接口
 * 
 * @author CYQ
 * @date 2026-01-26
 */
public interface IHmsReminderService 
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
    public List<HmsReminderVO> selectHmsReminderVOList(HmsReminder hmsReminder);

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
     * 批量删除提醒管理
     * 
     * @param ids 需要删除的提醒管理主键集合
     * @return 结果
     */
    public int deleteHmsReminderByIds(Long[] ids);

    /**
     * 删除提醒管理信息
     * 
     * @param id 提醒管理主键
     * @return 结果
     */
    public int deleteHmsReminderById(Long id);
}

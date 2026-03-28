package com.hms.main.reminder.service.impl;

import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.Page;
import com.hms.common.annotation.DataScope;
import com.hms.common.core.redis.RedisCache;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.LunarDateDistanceCalculator;
import com.hms.common.utils.SecurityUtils;
import com.hms.main.reminder.domain.HmsReminderVO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.main.reminder.mapper.HmsReminderMapper;
import com.hms.main.reminder.domain.HmsReminder;
import com.hms.main.reminder.service.IHmsReminderService;

/**
 * 提醒管理Service业务层处理
 * 
 * @author CYQ
 * @date 2026-01-26
 */
@Service
@Log4j2
public class HmsReminderServiceImpl implements IHmsReminderService 
{
    @Autowired
    private HmsReminderMapper hmsReminderMapper;
    @Autowired
    private RedisCache redisCache;

    /**
     * 查询提醒管理
     * 
     * @param id 提醒管理主键
     * @return 提醒管理
     */
    @Override
    public HmsReminder selectHmsReminderById(Long id)
    {
        return hmsReminderMapper.selectHmsReminderById(id);
    }

    /**
     * 查询提醒管理列表
     * 
     * @param hmsReminder 提醒管理
     * @return 提醒管理
     */
    @Override
    public List<HmsReminder> selectHmsReminderList(HmsReminder hmsReminder)
    {
        return hmsReminderMapper.selectHmsReminderList(hmsReminder);
    }
    /**
     * 查询加工后的提醒管理列表
     * @param hmsReminder 提醒管理
     * @return 提醒管理
     */
    @Override
    @DataScope(userAlias="u")
    public List<HmsReminderVO> selectHmsReminderVOList(HmsReminder hmsReminder)
    {
        new Page<>();
        Page<HmsReminder> hmsReminders = (Page<HmsReminder>)hmsReminderMapper.selectHmsReminderList(hmsReminder);
        Page<HmsReminderVO> list = (Page<HmsReminderVO>)hmsReminders.clone();
        list.clear();
        // 处理数据转换
        for (HmsReminder v : hmsReminders) {
            HmsReminderVO vo = new HmsReminderVO();
            BeanUtil.copyProperties(v, vo);
            try {
                //农历倒计时计算逻辑
                if("1".equals(vo.getCalendarType())){
                    long countdown = LunarDateDistanceCalculator.getNextDay(vo.getRemindDate());
                    vo.setCountdown(countdown);
                }else{//公历倒计时计算逻辑
                    String date = vo.getRemindDate();//MM-dd格式的日期
                    long countdown = DateUtils.getNextDay(date);
                    vo.setCountdown(countdown);
                }
                vo.setCreateByName(getUserName(vo.getCreateBy()));
                vo.setUpdateByName(getUserName(vo.getUpdateBy()));
            } catch (Exception e) {
                log.error("查询加工后的提醒管理列表异常",e);
            }
            list.add(vo);
        }

        // 如果原始列表是分页结果，则需要保持分页信息
//        if (hmsReminders instanceof Page) {
//            Page<HmsReminder> page = (Page<HmsReminder>) hmsReminders;
//
//            // 创建一个新的Page对象来保存处理后的数据
//            Page<HmsReminderVO> resultPage = new Page<>();
//            resultPage.addAll(list); // 添加处理后的数据
//
//            // 保留分页信息
//            resultPage.setTotal(page.getTotal());
//            resultPage.setPages(page.getPages());
//            resultPage.setPageNum(page.getPageNum());
//            resultPage.setPageSize(page.getPageSize());
//
//            return resultPage;
//        }

        return list;
    }
    //add by CYQ 2026年1月29日 获取用户名
    private String getUserName(String id){
        if(ObjectUtil.isEmpty(id)){
            return null;
        }
        String userName = redisCache.getCacheObject("user:id:"+id);
        if(ObjectUtil.isEmpty(userName)){
            redisCache.setCacheObject("user:id:"+id,hmsReminderMapper.selectUserNameById(id));
            userName = redisCache.getCacheObject("user:id:"+id);
        }
        return userName;
    }
    /**
     * 新增提醒管理
     * 
     * @param hmsReminder 提醒管理
     * @return 结果
     */
    @Override
    public int insertHmsReminder(HmsReminder hmsReminder)
    {
        hmsReminder.setCreateBy(SecurityUtils.getUserId().toString());
        hmsReminder.setCreateTime(DateUtils.getNowDate());
        return hmsReminderMapper.insertHmsReminder(hmsReminder);
    }

    /**
     * 修改提醒管理
     * 
     * @param hmsReminder 提醒管理
     * @return 结果
     */
    @Override
    public int updateHmsReminder(HmsReminder hmsReminder)
    {
        hmsReminder.setUpdateBy(SecurityUtils.getUserId().toString());
        hmsReminder.setUpdateTime(DateUtils.getNowDate());
        return hmsReminderMapper.updateHmsReminder(hmsReminder);
    }

    /**
     * 批量删除提醒管理
     * 
     * @param ids 需要删除的提醒管理主键
     * @return 结果
     */
    @Override
    public int deleteHmsReminderByIds(Long[] ids)
    {
        return hmsReminderMapper.deleteHmsReminderByIds(ids);
    }

    /**
     * 删除提醒管理信息
     * 
     * @param id 提醒管理主键
     * @return 结果
     */
    @Override
    public int deleteHmsReminderById(Long id)
    {
        return hmsReminderMapper.deleteHmsReminderById(id);
    }
}

package com.hms.main.timediary.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.common.annotation.DataScope;
import com.hms.main.timediary.mapper.HsmTimeDiaryMapper;
import com.hms.main.timediary.domain.HsmTimeDiary;
import com.hms.main.timediary.service.IHsmTimeDiaryService;

/**
 * 时间管理局Service业务层处理
 *
 * add by CYQ 2026年8月4日
 */
@Service
public class HsmTimeDiaryServiceImpl implements IHsmTimeDiaryService
{
    @Autowired
    private HsmTimeDiaryMapper hsmTimeDiaryMapper;

    @Override
    public HsmTimeDiary selectHsmTimeDiaryById(Long diaryId)
    {
        return hsmTimeDiaryMapper.selectHsmTimeDiaryById(diaryId);
    }

    @Override
    @DataScope(userAlias = "u")
    public List<HsmTimeDiary> selectHsmTimeDiaryList(HsmTimeDiary hsmTimeDiary)
    {
        return hsmTimeDiaryMapper.selectHsmTimeDiaryList(hsmTimeDiary);
    }

    @Override
    public int insertHsmTimeDiary(HsmTimeDiary hsmTimeDiary)
    {
        return hsmTimeDiaryMapper.insertHsmTimeDiary(hsmTimeDiary);
    }

    @Override
    public int updateHsmTimeDiary(HsmTimeDiary hsmTimeDiary)
    {
        return hsmTimeDiaryMapper.updateHsmTimeDiary(hsmTimeDiary);
    }

    @Override
    public int deleteHsmTimeDiaryByIds(Long[] diaryIds)
    {
        return hsmTimeDiaryMapper.deleteHsmTimeDiaryByIds(diaryIds);
    }

    @Override
    public int deleteHsmTimeDiaryById(Long diaryId)
    {
        return hsmTimeDiaryMapper.deleteHsmTimeDiaryById(diaryId);
    }

    @Override
    @DataScope(userAlias = "u")
    public List<Map<String, Object>> selectTimeDiaryCategoryStats(HsmTimeDiary hsmTimeDiary)
    {
        return hsmTimeDiaryMapper.selectTimeDiaryCategoryStats(hsmTimeDiary);
    }

    @Override
    @DataScope(userAlias = "u")
    public List<Map<String, Object>> selectTimeDiaryDailyStats(HsmTimeDiary hsmTimeDiary)
    {
        return hsmTimeDiaryMapper.selectTimeDiaryDailyStats(hsmTimeDiary);
    }

    @Override
    @DataScope(userAlias = "u")
    public List<Map<String, Object>> selectTimeDiaryDailyCategoryStats(HsmTimeDiary hsmTimeDiary)
    {
        return hsmTimeDiaryMapper.selectTimeDiaryDailyCategoryStats(hsmTimeDiary);
    }

    @Override
    @DataScope(userAlias = "u")
    public List<HsmTimeDiary> selectTimeDiaryByDate(HsmTimeDiary hsmTimeDiary)
    {
        return hsmTimeDiaryMapper.selectTimeDiaryByDate(hsmTimeDiary);
    }
}

package com.hms.main.timediary.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.enums.BusinessType;
import com.hms.main.timediary.domain.HsmTimeDiary;
import com.hms.main.timediary.service.IHsmTimeDiaryService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 时间管理局Controller
 *
 * add by CYQ 2026年8月4日
 */
@RestController
@RequestMapping("/main/timeDiary")
public class HsmTimeDiaryController extends BaseController
{
    @Autowired
    private IHsmTimeDiaryService hsmTimeDiaryService;

    /**
     * 查询时间管理局列表
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:list')")
    @GetMapping("/list")
    public TableDataInfo list(HsmTimeDiary hsmTimeDiary)
    {
        startPage();
        startOrderBy();
        List<HsmTimeDiary> list = hsmTimeDiaryService.selectHsmTimeDiaryList(hsmTimeDiary);
        return getDataTable(list);
    }

    /**
     * 查询时间管理局统计（分类占比 + 每日时长趋势）
     *
     * add by CYQ 2026年8月4日
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:list')")
    @GetMapping("/stats")
    public AjaxResult stats(HsmTimeDiary hsmTimeDiary)
    {
        List<Map<String, Object>> categoryStats = hsmTimeDiaryService.selectTimeDiaryCategoryStats(hsmTimeDiary);
        List<Map<String, Object>> dailyStats = hsmTimeDiaryService.selectTimeDiaryDailyStats(hsmTimeDiary);
        List<Map<String, Object>> dailyCategoryStats = hsmTimeDiaryService.selectTimeDiaryDailyCategoryStats(hsmTimeDiary);
        Map<String, Object> result = new HashMap<>();
        result.put("categoryDistribution", categoryStats);
        result.put("dailyTrend", dailyStats);
        result.put("dailyCategoryTrend", dailyCategoryStats);
        return success(result);
    }

    /**
     * 查询某天的时间管理局（用于时间轴展示）
     *
     * add by CYQ 2026年8月4日
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:list')")
    @GetMapping("/timeline")
    public AjaxResult timeline(HsmTimeDiary hsmTimeDiary)
    {
        List<HsmTimeDiary> list = hsmTimeDiaryService.selectTimeDiaryByDate(hsmTimeDiary);
        return success(list);
    }

    /**
     * 导出时间管理局列表
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:export')")
    @Log(title = "时间管理局", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HsmTimeDiary hsmTimeDiary)
    {
        List<HsmTimeDiary> list = hsmTimeDiaryService.selectHsmTimeDiaryList(hsmTimeDiary);
        ExcelUtil<HsmTimeDiary> util = new ExcelUtil<HsmTimeDiary>(HsmTimeDiary.class);
        util.exportExcel(response, list, "时间管理局数据");
    }

    /**
     * 获取时间管理局详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:query')")
    @GetMapping(value = "/{diaryId}")
    public AjaxResult getInfo(@PathVariable("diaryId") Long diaryId)
    {
        return success(hsmTimeDiaryService.selectHsmTimeDiaryById(diaryId));
    }

    /**
     * 新增时间管理局
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:add')")
    @Log(title = "时间管理局", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HsmTimeDiary hsmTimeDiary)
    {
        hsmTimeDiary.setCreateBy(getUsername());
        return toAjax(hsmTimeDiaryService.insertHsmTimeDiary(hsmTimeDiary));
    }

    /**
     * 修改时间管理局
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:edit')")
    @Log(title = "时间管理局", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HsmTimeDiary hsmTimeDiary)
    {
        return toAjax(hsmTimeDiaryService.updateHsmTimeDiary(hsmTimeDiary));
    }

    /**
     * 删除时间管理局
     */
    @PreAuthorize("@ss.hasPermi('main:timeDiary:remove')")
    @Log(title = "时间管理局", businessType = BusinessType.DELETE)
    @DeleteMapping("/{diaryIds}")
    public AjaxResult remove(@PathVariable Long[] diaryIds)
    {
        return toAjax(hsmTimeDiaryService.deleteHsmTimeDiaryByIds(diaryIds));
    }
}

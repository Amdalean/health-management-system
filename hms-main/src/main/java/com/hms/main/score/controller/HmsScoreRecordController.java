package com.hms.main.score.controller;

import java.util.List;
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
import com.hms.main.score.domain.HmsScoreRecord;
import com.hms.main.score.service.IHmsScoreRecordService;
import com.hms.common.utils.poi.ExcelUtil;
import com.hms.common.core.page.TableDataInfo;

/**
 * 成绩分析记录 Controller
 *
 * @author CYQ
 * @date 2026-03-01
 */
@RestController
@RequestMapping("/main/scoreRecord")
public class HmsScoreRecordController extends BaseController
{
    @Autowired
    private IHmsScoreRecordService hmsScoreRecordService;

    /**
     * 查询成绩分析记录列表
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmsScoreRecord hmsScoreRecord)
    {
        startPage();
        List<HmsScoreRecord> list = hmsScoreRecordService.selectHmsScoreRecordList(hmsScoreRecord);
        return getDataTable(list);
    }

    /**
     * 导出成绩分析记录列表
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:export')")
    @Log(title = "成绩分析记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, HmsScoreRecord hmsScoreRecord)
    {
        List<HmsScoreRecord> list = hmsScoreRecordService.selectHmsScoreRecordList(hmsScoreRecord);
        ExcelUtil<HmsScoreRecord> util = new ExcelUtil<HmsScoreRecord>(HmsScoreRecord.class);
        util.exportExcel(response, list, "成绩分析记录数据");
    }

    /**
     * 获取成绩分析记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(hmsScoreRecordService.selectHmsScoreRecordById(id));
    }

    /**
     * 新增成绩分析记录
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:add')")
    @Log(title = "成绩分析记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmsScoreRecord hmsScoreRecord)
    {
        return toAjax(hmsScoreRecordService.insertHmsScoreRecord(hmsScoreRecord));
    }

    /**
     * 修改成绩分析记录
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:edit')")
    @Log(title = "成绩分析记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmsScoreRecord hmsScoreRecord)
    {
        return toAjax(hmsScoreRecordService.updateHmsScoreRecord(hmsScoreRecord));
    }

    /**
     * 删除成绩分析记录
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:remove')")
    @Log(title = "成绩分析记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(hmsScoreRecordService.deleteHmsScoreRecordByIds(ids));
    }

    /**
     * 提交视频进行 AI 分析
     * 提交后状态自动设置为 1(分析中)，并异步调用八段锦 AI 打分
     * 打分完成后状态更新为 2(已完成)，失败则为 3(分析失败)
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:submit')")
    @Log(title = "提交 AI 分析", businessType = BusinessType.OTHER)
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody HmsScoreRecord hmsScoreRecord)
    {
        HmsScoreRecord result = hmsScoreRecordService.submitForAnalysis(hmsScoreRecord);
        return success(result);
    }

    /**
     * 查询 AI 分析任务状态
     */
    @PreAuthorize("@ss.hasPermi('main:scoreRecord:query')")
    @GetMapping("/task/{taskId}")
    public AjaxResult getTaskStatus(@PathVariable String taskId)
    {
        // 根据 taskId 查询记录
        HmsScoreRecord record = hmsScoreRecordService.selectHmsScoreRecordByTaskId(taskId);
        if (record == null)
        {
            return error("任务不存在");
        }
        
        AjaxResult result = success(record);
        result.put("taskId", taskId);
        result.put("status", record.getStatus());
        
        // 状态说明：0-待分析，1-分析中，2-已完成，3-分析失败
        String statusDesc;
        switch (record.getStatus())
        {
            case "0": statusDesc = "待分析"; break;
            case "1": statusDesc = "分析中"; break;
            case "2": statusDesc = "已完成"; break;
            case "3": statusDesc = "分析失败"; break;
            default: statusDesc = "未知状态";
        }
        result.put("statusDesc", statusDesc);
        
        return result;
    }
}

package com.hms.main.score.service;

import java.util.HashMap;
import java.util.Map;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.DateUtils;
import com.hms.main.score.domain.HmsScoreRecord;
import com.hms.main.score.mapper.HmsScoreRecordMapper;
import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;
import com.hms.ai.util.AiJsonCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 成绩智能分析 - AI 打分异步执行任务
 *
 * add by CYQ 2026-11-15 说明:
 *   原来 @Async 直接写在 HmsScoreRecordServiceImpl 内部并被同类方法调用，
 *   Spring 代理不生效（自调用 bypass），AI 调用实际同步阻塞请求线程，
 *   前端 10s 超时 → 报「系统接口请求超时」，且列表拿不到「分析中」状态。
 *   这里独立成 Bean，确保调用走代理、真正异步执行。
 *
 * @author CYQ
 * @date 2026-11-15
 */
@Component
public class ScoreAnalysisTask
{
    private static final Logger log = LoggerFactory.getLogger(ScoreAnalysisTask.class);

    @Autowired
    private HmsScoreRecordMapper hmsScoreRecordMapper;

    /** 服务器端口 */
    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 异步调用 AI 进行八段锦打分
     * 调用成功后状态改为 2(已完成)，失败则改为 3(分析失败)
     *
     * @param record 成绩智能分析
     */
    @Async("threadPoolTaskExecutor")
    public void asyncBaduanjinAnalysis(HmsScoreRecord record)
    {
        Long id = record.getId();
        String taskId = record.getTaskId();

        log.info("开始异步调用八段锦 AI 打分，任务 ID: {}, 记录 ID: {}", taskId, id);

        try
        {
            // 1. 构建视频分析输入
            String videoAnalysisInput = buildVideoAnalysisInput(record);

            // 2. 创建 AI 聊天实例（使用八段锦打分策略）
            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(
                AIChatFactory.ServiceType.OPENCODE_GO,
                AIChatFactory.StrategyType.BADUANJIN_SCORING
            );

            // 3. 准备上下文信息
            Map<String, Object> context = new HashMap<>();
            context.put("studentId", record.getStudentId());
            context.put("courseId", record.getCourseId());
            context.put("type", "video");
            context.put("filePatch", getServerUrl() + record.getFilePath());
            // add by CYQ 2026-10-19 业务单号带入集成日志
            context.put("billId", "score-" + id);

            // 4. 调用 AI 服务
            String aiResponse = aiChat.chat(videoAnalysisInput, context);

            log.info("AI 打分结果：{}", aiResponse);

            // 5. 解析 AI 返回的 JSON 结果
            aiResponse = cleanJsonResponse(aiResponse);
            JSONObject result = JSONObject.parseObject(aiResponse);

            // 6. 更新成绩记录
            HmsScoreRecord updateRecord = new HmsScoreRecord();
            updateRecord.setId(id);

            // 设置分数 (overall_score: 0-100)
            if (result != null && result.containsKey("overall_score"))
            {
                updateRecord.setScore(result.getBigDecimal("overall_score"));
            }

            // 设置状态为已完成 (2)
            updateRecord.setStatus("2");

            // 设置教练寄语作为整体评语 (coach_message)
            if (result != null && result.containsKey("coach_message"))
            {
                updateRecord.setFeedback(result.getString("coach_message"));
            }

            // 设置分析详情（完整的 JSON 结果）
            updateRecord.setAnalysisDetails(aiResponse);

            updateRecord.setUpdateBy("SYSTEM");
            updateRecord.setUpdateTime(DateUtils.getNowDate());

            // 7. 更新数据库
            hmsScoreRecordMapper.updateHmsScoreRecord(updateRecord);

            log.info("八段锦 AI 打分完成，记录 ID: {}, 总分：{}", id, updateRecord.getScore());
        }
        catch (Exception e)
        {
            log.error("八段锦 AI 打分失败，任务 ID: {}, 记录 ID: {}", taskId, id, e);

            // 更新状态为分析失败 (3)
            HmsScoreRecord updateRecord = new HmsScoreRecord();
            updateRecord.setId(id);
            updateRecord.setStatus("3");
            updateRecord.setFeedback("AI 分析失败：" + e.getMessage());
            updateRecord.setUpdateBy("SYSTEM");
            updateRecord.setUpdateTime(DateUtils.getNowDate());

            try
            {
                hmsScoreRecordMapper.updateHmsScoreRecord(updateRecord);
            }
            catch (Exception ex)
            {
                log.error("回写分析失败状态异常，记录 ID: {}", id, ex);
            }
        }
    }

    /**
     * 获取服务器地址（从配置文件读取）
     *
     * @return 服务器地址，例如：http://localhost:8080
     */
    private String getServerUrl()
    {
        String ip = System.getProperty("java.rmi.server.hostname");
        ip = ip == null ? "localhost" : ip;
        return "http://" + ip + ":" + serverPort;
    }

    /**
     * 清理 AI 响应中的 Markdown 标记，确保 JSON 可以被正确解析
     *
     * @param response AI 响应字符串
     * @return 清理后的 JSON 字符串
     */
    private String cleanJsonResponse(String response)
    {
        // update by CYQ 2026-11-16 清洗规则抽到 AiJsonCleaner，业务与单元测试共用同一实现
        return AiJsonCleaner.clean(response);
    }

    /**
     * 构建视频分析输入
     *
     * @param record 成绩智能分析
     * @return 视频分析文本
     */
    private String buildVideoAnalysisInput(HmsScoreRecord record)
    {
        StringBuilder input = new StringBuilder();

        input.append("视频文件路径：").append(record.getFilePath()).append("\n");
        input.append("视频名称：").append(record.getFileName()).append("\n");
        input.append("分析类型：").append(record.getAnalysisType()).append("\n");

        // TODO: 这里可以调用视频分析服务
        // 例如：提取关键帧、识别八段锦八个动作、分析动作质量等
        // 目前暂时使用文件信息作为输入

        input.append("\n请对该八段锦练习视频进行专业评分。\n");

        return input.toString();
    }
}

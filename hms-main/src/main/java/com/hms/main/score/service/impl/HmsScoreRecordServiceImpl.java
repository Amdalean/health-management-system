package com.hms.main.score.service.impl;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.io.File;
import javax.servlet.http.HttpServletRequest;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.utils.ServletUtils;
import com.hms.common.annotation.DataScope;
import com.hms.common.config.RuoYiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.hms.main.score.mapper.HmsScoreRecordMapper;
import com.hms.main.score.domain.HmsScoreRecord;
import com.hms.main.score.service.IHmsScoreRecordService;
import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.template.AbstractAIChatTemplate;

/**
 * 成绩分析记录 Service 业务层处理
 *
 * @author CYQ
 * @date 2026-03-01
 */
@Service
public class HmsScoreRecordServiceImpl implements IHmsScoreRecordService
{
    private static final Logger log = LoggerFactory.getLogger(HmsScoreRecordServiceImpl.class);

    @Autowired
    private HmsScoreRecordMapper hmsScoreRecordMapper;

    /**
     * 查询成绩分析记录
     *
     * @param id 成绩分析记录主键
     * @return 成绩分析记录
     */
    @Override
    public HmsScoreRecord selectHmsScoreRecordById(Long id)
    {
        return hmsScoreRecordMapper.selectHmsScoreRecordById(id);
    }

    /**
     * 查询成绩分析记录列表
     *
     * @param hmsScoreRecord 成绩分析记录
     * @return 成绩分析记录
     */
    @DataScope(userAlias="u")
    @Override
    public List<HmsScoreRecord> selectHmsScoreRecordList(HmsScoreRecord hmsScoreRecord)
    {
        return hmsScoreRecordMapper.selectHmsScoreRecordList(hmsScoreRecord);
    }

    /**
     * 新增成绩分析记录
     *
     * @param hmsScoreRecord 成绩分析记录
     * @return 结果
     */
    @Override
    public int insertHmsScoreRecord(HmsScoreRecord hmsScoreRecord)
    {
        hmsScoreRecord.setCreateBy(SecurityUtils.getUserId().toString());
        hmsScoreRecord.setCreateTime(DateUtils.getNowDate());
        hmsScoreRecord.setStudentId(SecurityUtils.getUserId());
        return hmsScoreRecordMapper.insertHmsScoreRecord(hmsScoreRecord);
    }

    /**
     * 修改成绩分析记录
     *
     * @param hmsScoreRecord 成绩分析记录
     * @return 结果
     */
    @Override
    public int updateHmsScoreRecord(HmsScoreRecord hmsScoreRecord)
    {
        hmsScoreRecord.setUpdateBy(SecurityUtils.getUserId().toString());
        hmsScoreRecord.setUpdateTime(DateUtils.getNowDate());
        return hmsScoreRecordMapper.updateHmsScoreRecord(hmsScoreRecord);
    }

    /**
     * 批量删除成绩分析记录
     *
     * @param ids 需要删除的成绩分析记录主键
     * @return 结果
     */
    @Override
    public int deleteHmsScoreRecordByIds(Long[] ids)
    {
        return hmsScoreRecordMapper.deleteHmsScoreRecordByIds(ids);
    }

    /**
     * 删除成绩分析记录信息
     *
     * @param id 成绩分析记录主键
     * @return 结果
     */
    @Override
    public int deleteHmsScoreRecordById(Long id)
    {
        return hmsScoreRecordMapper.deleteHmsScoreRecordById(id);
    }

    /**
     * 根据任务 ID 查询成绩分析记录
     *
     * @param taskId 任务 ID
     * @return 成绩分析记录
     */
    @Override
    public HmsScoreRecord selectHmsScoreRecordByTaskId(String taskId)
    {
        return hmsScoreRecordMapper.selectHmsScoreRecordByTaskId(taskId);
    }

    /**
     * 提交视频进行 AI 分析
     * 提交时将状态设置为 1(分析中)，并异步调用八段锦 AI 打分
     *
     * @param hmsScoreRecord 成绩分析记录
     * @return 结果
     */
    @Override
    public HmsScoreRecord submitForAnalysis(HmsScoreRecord hmsScoreRecord)
    {
        // 1. 校验附件是否存在，并获取完整路径
        String fullPath = getFullFilePath(hmsScoreRecord.getFilePath());
        validateAttachmentByPath(fullPath);
        // 2. 环境校验：只在生产环境允许调用
        checkEnvironment();
        // 生成任务 ID
        String taskId = UUID.randomUUID().toString().replace("-", "");
        hmsScoreRecord.setTaskId(taskId);
        
        // 设置状态为分析中 (1)
        hmsScoreRecord.setStatus("1");
        
        // 设置默认分数和评语
        hmsScoreRecord.setScore(null);
        hmsScoreRecord.setFeedback(null);
        hmsScoreRecord.setAnalysisDetails(null);
        
        // 设置创建信息
        hmsScoreRecord.setCreateBy(SecurityUtils.getUserId().toString());
        hmsScoreRecord.setCreateTime(DateUtils.getNowDate());
        hmsScoreRecord.setStudentId(SecurityUtils.getUserId());
        
        // 插入记录
        int rows = hmsScoreRecordMapper.updateHmsScoreRecord(hmsScoreRecord);
        
        if (rows > 0)
        {
            // 异步调用 AI 进行八段锦打分
            asyncBaduanjinAnalysis(hmsScoreRecord);
        }
        
        return hmsScoreRecord;
    }

    /**
     * 异步调用 AI 进行八段锦打分
     * 调用成功后状态改为 2(已完成)，失败则改为 3(分析失败)
     *
     * @param record 成绩分析记录
     */
    @Async
    public void asyncBaduanjinAnalysis(HmsScoreRecord record)
    {
        Long id = record.getId();
        String taskId = record.getTaskId();

        log.info("开始异步调用八段锦 AI 打分，任务 ID: {}, 记录 ID: {}", taskId, id);

        try
        {
            // 1. 构建视频分析输入（这里可以调用视频分析服务提取关键信息）
            String videoAnalysisInput = buildVideoAnalysisInput(record);

            // 2. 创建 AI 聊天实例（使用八段锦打分策略）
            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(
                AIChatFactory.StrategyType.BADUANJIN_SCORING
            );

            // 3. 准备上下文信息
            Map<String, Object> context = new HashMap<>();
            context.put("studentId", record.getStudentId());
            context.put("courseId", record.getCourseId());
            context.put("type", "video");
            context.put("filePatch", getServerUrl() + record.getFilePath());

            // 4. 调用 AI 服务
            String aiResponse = aiChat.chat(videoAnalysisInput, context);

            log.info("AI 打分结果：{}", aiResponse);

            // 5. 解析 AI 返回的 JSON 结果
            // 清理可能的 Markdown 标记
            aiResponse = cleanJsonResponse(aiResponse);
            JSONObject result = JSONObject.parseObject(aiResponse);

            // 6. 更新成绩记录
            HmsScoreRecord updateRecord = new HmsScoreRecord();
            updateRecord.setId(id);

            // 设置分数 (overall_score: 0-100)
            if (result.containsKey("overall_score"))
            {
                updateRecord.setScore(result.getBigDecimal("overall_score"));
            }

            // 设置状态为已完成 (2)
            updateRecord.setStatus("2");

            // 设置教练寄语作为整体评语 (coach_message)
            if (result.containsKey("coach_message"))
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

            hmsScoreRecordMapper.updateHmsScoreRecord(updateRecord);
        }
    }

    /**
     * 获取完整的文件路径
     *
     * @param filePath 文件路径（如：/profile/upload/2026/03/01/xxx.mp4）
     * @return 完整文件路径
     */
    private String getFullFilePath(String filePath)
    {
        if (filePath == null || filePath.trim().isEmpty())
        {
            throw new RuntimeException("视频文件路径不能为空");
        }
        
        // 如果是 URL，直接返回
        if (filePath.startsWith("http://") || filePath.startsWith("https://"))
        {
            return filePath;
        }
        
        // 处理 /profile 开头的路径
        // 例如：/profile/upload/2026/03/01/xxx.mp4
        // 需要去掉 /profile 前缀，拼接 ruoyi.profile 配置
        String relativePath = filePath;
        if (filePath.startsWith("/profile"))
        {
            relativePath = filePath.substring(8); // 去掉 "/profile"
        }
        else if (filePath.startsWith("profile"))
        {
            relativePath = filePath.substring(7); // 去掉 "profile"
        }
        
        // 获取配置的上传根目录（如：E:\space\IDE\hms\\uploadPath）
        String uploadPath = RuoYiConfig.getProfile();
        
        // 拼接完整路径（如：E:\space\IDE\hms\\uploadPath/upload/2026/03/01/xxx.mp4）
        String fullPath = uploadPath + relativePath;
        
        // 规范化路径（处理混合斜杠）
        fullPath = fullPath.replace("\\", "/");
        
        log.info("文件路径转换：{} -> {}", filePath, fullPath);
        
        return fullPath;
    }

    /**
     * 校验附件是否存在（通过完整路径）
     *
     * @param fullPath 完整文件路径
     */
    private void validateAttachmentByPath(String fullPath)
    {
        // 如果是 URL，暂时认为有效
        if (fullPath.startsWith("http://") || fullPath.startsWith("https://"))
        {
            log.info("视频文件为 URL 格式：{}", fullPath);
            return;
        }
        
        // 规范化路径（处理混合斜杠）
        fullPath = fullPath.replace("\\", "/");
        
        // 检查文件是否存在
        File file = new File(fullPath);
        if (!file.exists())
        {
            log.error("视频文件不存在，路径：{}", fullPath);
            throw new RuntimeException("视频文件不存在：" + fullPath);
        }
        
        if (!file.isFile())
        {
            throw new RuntimeException("路径不是有效文件：" + fullPath);
        }
        
        if (file.length() == 0)
        {
            throw new RuntimeException("视频文件大小为 0：" + fullPath);
        }
        
        log.info("视频文件校验通过：{}, 大小：{} bytes", fullPath, file.length());
    }

    /**
     * 环境校验：只在生产环境允许调用 AI 服务
     * 通过服务器 IP 地址判断是否为本地环境
     */
    private void checkEnvironment()
    {
        try
        {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null)
            {
                String serverName = request.getServerName();
                log.info("当前服务器名称/IP: {}", serverName);
                
                // 判断是否为本地环境（localhost 或 127 开头的 IP）
                if ("localhost".equalsIgnoreCase(serverName) || 
                    serverName.startsWith("127."))
                {
                    throw new RuntimeException("AI 打分服务仅在生产环境可用，当前为本地环境：" + serverName);
                }
            }
        }
        catch (RuntimeException e)
        {
            // 直接抛出运行时异常
            throw e;
        }
        catch (Exception e)
        {
            log.warn("获取服务器信息失败", e);
            throw new RuntimeException("无法获取服务器信息，无法验证环境");
        }
    }

    /**
     * 清理 AI 响应中的 Markdown 标记，确保 JSON 可以被正确解析
     *
     * @param response AI 响应字符串
     * @return 清理后的 JSON 字符串
     */
    private String cleanJsonResponse(String response)
    {
        if (response == null)
        {
            return null;
        }

        // 移除 ```json 和 ``` 标记
        response = response.replaceAll("```json\\s*", "");
        response = response.replaceAll("```\\s*", "");

        // 移除前后的空白字符
        response = response.trim();

        return response;
    }

    /**
     * 获取服务器地址
     *
     * @return 服务器地址，例如：http://82.156.236.247:8080
     */
    private String getServerUrl()
    {
        try
        {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null)
            {
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = request.getServletContext().getContextPath();
                
                StringBuilder url = new StringBuilder();
                url.append(scheme).append("://");
                url.append(serverName);
                if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443))
                {
                    url.append(":").append(serverPort);
                }
                url.append(contextPath);
                return url.toString();
            }
        }
        catch (Exception e)
        {
            log.warn("获取服务器地址失败，使用默认地址", e);
        }
        // 默认地址
        return "http://82.156.236.247:8080";
    }

    /**
     * 构建视频分析输入
     * 这里可以调用视频分析服务，提取视频关键帧、动作识别等信息
     *
     * @param record 成绩分析记录
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

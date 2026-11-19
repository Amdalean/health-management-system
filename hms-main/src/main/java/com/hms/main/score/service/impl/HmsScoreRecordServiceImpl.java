package com.hms.main.score.service.impl;

import java.util.List;
import java.util.UUID;
import java.io.File;
import javax.servlet.http.HttpServletRequest;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.SecurityUtils;
import com.hms.common.utils.ServletUtils;
import com.hms.common.annotation.DataScope;
import com.hms.common.config.RuoYiConfig;
import com.hms.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hms.main.course.domain.HmsCourse;
import com.hms.main.course.mapper.HmsCourseMapper;
import com.hms.main.score.mapper.HmsScoreRecordMapper;
import com.hms.main.score.domain.HmsScoreRecord;
import com.hms.main.score.service.IHmsScoreRecordService;
import com.hms.main.score.service.ScoreAnalysisTask;
import com.hms.main.student.domain.HmsStudent;
import com.hms.main.student.mapper.HmsStudentMapper;

/**
 * 成绩智能分析 Service 业务层处理
 *
 * @author 崔永琦
 * @date 2026-03-01
 * add by CYQ 2026-10-18 说明: 与学生建档、课程建档、学生选课联动。
 *   提交/新增/修改成绩智能分析时统一校验「学生存在 + 课程存在 + 学生已选修该课程」；
 *   学生未显式指定时按当前登录账号关联的学生档案自动解析。
 */
@Service
public class HmsScoreRecordServiceImpl implements IHmsScoreRecordService
{
    private static final Logger log = LoggerFactory.getLogger(HmsScoreRecordServiceImpl.class);

    @Autowired
    private HmsScoreRecordMapper hmsScoreRecordMapper;

    @Autowired
    private HmsStudentMapper hmsStudentMapper;

    @Autowired
    private HmsCourseMapper hmsCourseMapper;

    @Autowired
    private ScoreAnalysisTask scoreAnalysisTask;

    /**
     * 查询成绩智能分析
     *
     * @param id 成绩智能分析主键
     * @return 成绩智能分析
     */
    @Override
    public HmsScoreRecord selectHmsScoreRecordById(Long id)
    {
        return hmsScoreRecordMapper.selectHmsScoreRecordById(id);
    }

    /**
     * 查询成绩智能分析列表
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 成绩智能分析
     */
    @DataScope(userAlias="u")
    @Override
    public List<HmsScoreRecord> selectHmsScoreRecordList(HmsScoreRecord hmsScoreRecord)
    {
        return hmsScoreRecordMapper.selectHmsScoreRecordList(hmsScoreRecord);
    }

    /**
     * 新增成绩智能分析
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 结果
     */
    @Override
    public int insertHmsScoreRecord(HmsScoreRecord hmsScoreRecord)
    {
        // 与学生建档 / 课程建档 / 选课关系联动校验
        validateScoreRelation(hmsScoreRecord);
        hmsScoreRecord.setCreateBy(SecurityUtils.getUserId().toString());
        hmsScoreRecord.setCreateTime(DateUtils.getNowDate());
        return hmsScoreRecordMapper.insertHmsScoreRecord(hmsScoreRecord);
    }

    /**
     * 修改成绩智能分析
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 结果
     */
    @Override
    public int updateHmsScoreRecord(HmsScoreRecord hmsScoreRecord)
    {
        // 只有在同时提交了学生与课程时才做联动校验（异步回写等局部更新不触发）
        if (hmsScoreRecord.getStudentId() != null && hmsScoreRecord.getCourseId() != null)
        {
            validateScoreRelation(hmsScoreRecord);
        }
        hmsScoreRecord.setUpdateBy(SecurityUtils.getUserId().toString());
        hmsScoreRecord.setUpdateTime(DateUtils.getNowDate());
        return hmsScoreRecordMapper.updateHmsScoreRecord(hmsScoreRecord);
    }

    /**
     * 批量删除成绩智能分析
     *
     * @param ids 需要删除的成绩智能分析主键
     * @return 结果
     */
    @Override
    public int deleteHmsScoreRecordByIds(Long[] ids)
    {
        return hmsScoreRecordMapper.deleteHmsScoreRecordByIds(ids);
    }

    /**
     * 删除成绩智能分析信息
     *
     * @param id 成绩智能分析主键
     * @return 结果
     */
    @Override
    public int deleteHmsScoreRecordById(Long id)
    {
        return hmsScoreRecordMapper.deleteHmsScoreRecordById(id);
    }

    /**
     * 根据任务 ID 查询成绩智能分析
     *
     * @param taskId 任务 ID
     * @return 成绩智能分析
     */
    @Override
    public HmsScoreRecord selectHmsScoreRecordByTaskId(String taskId)
    {
        return hmsScoreRecordMapper.selectHmsScoreRecordByTaskId(taskId);
    }

    /**
     * 学生下拉选项（学号 + 姓名）
     *
     * @return 学生集合
     */
    @Override
    public List<HmsStudent> selectStudentOptions()
    {
        return hmsScoreRecordMapper.selectStudentOptions();
    }

    /**
     * 查询某学生已选课程（studentId 为空时返回全部课程）
     *
     * @param studentId 学生ID
     * @return 课程集合
     */
    @Override
    public List<HmsCourse> selectCourseOptions(Long studentId)
    {
        return hmsScoreRecordMapper.selectCourseOptionsByStudent(studentId);
    }

    /**
     * 查询当前登录用户可选的课程
     * 学生账号返回其已选课程；未关联学生档案的账号（教师/管理员）返回全部课程
     *
     * @return 课程集合
     */
    @Override
    public List<HmsCourse> selectMyCourses()
    {
        HmsStudent self = hmsStudentMapper.selectHmsStudentByUserId(SecurityUtils.getUserId());
        return hmsScoreRecordMapper.selectCourseOptionsByStudent(self == null ? null : self.getStudentId());
    }

    /**
     * 校验「学生 — 课程 — 选课」联动关系
     * add by CYQ 2026-10-18 说明:
     *   1) 课程必须来自 hms_course（课程建档）；
     *   2) 学生未指定时，按当前登录账号关联的学生档案（hms_student.user_id）自动解析；
     *   3) 学生必须已选修该课程（hms_student_course 存在对应记录）。
     *
     * @param record 成绩智能分析
     */
    private void validateScoreRelation(HmsScoreRecord record)
    {
        if (record.getCourseId() == null)
        {
            throw new ServiceException("请选择课程");
        }
        HmsCourse course = hmsCourseMapper.selectHmsCourseById(record.getCourseId());
        if (course == null)
        {
            throw new ServiceException("所选课程不存在，请重新选择");
        }

        if (record.getStudentId() == null)
        {
            HmsStudent self = hmsStudentMapper.selectHmsStudentByUserId(SecurityUtils.getUserId());
            if (self == null)
            {
                throw new ServiceException("当前登录账号未关联学生档案，请先在「教学管理 → 学生管理」中生成账号");
            }
            record.setStudentId(self.getStudentId());
        }
        else if (hmsStudentMapper.selectHmsStudentById(record.getStudentId()) == null)
        {
            throw new ServiceException("所选学生不存在，请重新选择");
        }

        if (hmsScoreRecordMapper.countStudentCourse(record.getStudentId(), record.getCourseId()) <= 0)
        {
            throw new ServiceException("该学生未选修课程【" + course.getCourseName()
                    + "】，请先在「教学管理 → 课程管理 → 选课」中完成选课");
        }
    }

    /**
     * 提交视频进行 AI 分析
     * 提交时将状态设置为 1(分析中)，并异步调用八段锦 AI 打分
     *
     * @param hmsScoreRecord 成绩智能分析
     * @return 结果
     */
    @Override
    public HmsScoreRecord submitForAnalysis(HmsScoreRecord hmsScoreRecord)
    {
        if (hmsScoreRecord.getId() == null)
        {
            throw new ServiceException("请选择要提交分析的记录");
        }
        // 1. 校验附件是否存在，并获取完整路径
        String fullPath = getFullFilePath(hmsScoreRecord.getFilePath());
        validateAttachmentByPath(fullPath);
        // 2. 环境校验：只在生产环境允许调用
        checkEnvironment();
        // 生成任务 ID
        String taskId = UUID.randomUUID().toString().replace("-", "");
        hmsScoreRecord.setTaskId(taskId);

        // 与学生建档 / 课程建档 / 选课关系联动校验（未指定学生时按登录账号自动解析）
        validateScoreRelation(hmsScoreRecord);

        // 设置状态为分析中 (1)，并清空上一轮的评分/评语/详情
        hmsScoreRecord.setStatus("1");
        hmsScoreRecord.setUpdateBy(SecurityUtils.getUserId().toString());
        hmsScoreRecord.setUpdateTime(DateUtils.getNowDate());

        // add by CYQ 2026-11-15 说明: 状态先落库再异步分析，接口立即返回，避免前端 10s 超时
        int rows = hmsScoreRecordMapper.resetForAnalysis(hmsScoreRecord);

        if (rows > 0)
        {
            // 异步调用 AI 进行八段锦打分（跨 Bean 调用，@Async 代理生效）
            scoreAnalysisTask.asyncBaduanjinAnalysis(hmsScoreRecord);
        }

        return hmsScoreRecord;
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
}

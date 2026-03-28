# 八段锦 AI 打分策略使用说明

## 创建时间
2026-03-01

## 策略类
`com.hms.ai.strategy.BaduanjinScoringStrategy`

## 功能描述
专门用于八段锦健身气功视频动作的 AI 智能打分，输出标准 JSON 格式的评分结果。

## 使用方式

### 1. 通过 AIChatFactory 调用

```java
import com.hms.ai.factory.AIChatFactory;
import com.hms.ai.factory.AIChatFactory.StrategyType;
import com.hms.ai.template.AbstractAIChatTemplate;

// 创建八段锦打分 AI 实例
AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(StrategyType.BADUANJIN_SCORING);

// 准备上下文信息
Map<String, Object> context = new HashMap<>();
context.put("studentId", "2024001");
context.put("courseId", "1001");
context.put("analysisType", "八段锦完整套路");

// 调用 AI 打分（input 可以是视频分析后的文本描述）
String result = aiChat.chat(videoAnalysisText, context);

// 解析 JSON 结果
JSONObject jsonResult = JSON.parseObject(result);
Double totalScore = jsonResult.getDouble("totalScore");
String level = jsonResult.getString("level");
JSONArray movements = jsonResult.getJSONArray("movements");
```

### 2. 在 Service 层调用

```java
@Service
public class ScoreRecordServiceImpl implements IScoreRecordService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 异步调用 AI 进行八段锦视频打分
     */
    @Async
    public void analyzeBaduanjinVideo(ScoreRecord record) {
        try {
            // 1. 调用视频分析服务（帧提取、动作识别等）
            String videoAnalysisText = analyzeVideoFrames(record.getFilePath());
            
            // 2. 调用 AI 打分服务
            AbstractAIChatTemplate aiChat = AIChatFactory.createAIChat(StrategyType.BADUANJIN_SCORING);
            Map<String, Object> context = new HashMap<>();
            context.put("studentId", record.getStudentId());
            context.put("courseId", record.getCourseId());
            context.put("analysisType", "八段锦");
            
            String aiResponse = aiChat.chat(videoAnalysisText, context);
            
            // 3. 解析 AI 返回的 JSON 结果
            JSONObject result = JSON.parseObject(aiResponse);
            
            // 4. 更新成绩记录
            record.setScore(result.getBigDecimal("totalScore"));
            record.setStatus("2"); // 已完成
            record.setFeedback(result.getString("overallFeedback"));
            record.setAnalysisDetails(result.toJSONString());
            
            scoreRecordMapper.updateScoreRecord(record);
            
        } catch (Exception e) {
            log.error("AI 打分失败", e);
            record.setStatus("3"); // 分析失败
            scoreRecordMapper.updateScoreRecord(record);
        }
    }
    
    /**
     * 视频帧分析（伪代码）
     */
    private String analyzeVideoFrames(String videoPath) {
        // 这里调用视频分析服务
        // 提取关键帧、识别动作、生成分析文本
        return "视频分析结果文本...";
    }
}
```

## JSON 输出格式

```json
{
  "totalScore": 95.5,
  "pass": true,
  "level": "优秀",
  "movements": [
    {
      "name": "两手托天理三焦",
      "score": 95,
      "accuracy": 92,
      "completeness": 96,
      "timing": 97,
      "posture": 95,
      "feedback": "动作舒展，托举到位"
    },
    {
      "name": "左右开弓似射雕",
      "score": 93,
      "accuracy": 90,
      "completeness": 94,
      "timing": 95,
      "posture": 93,
      "feedback": "开弓姿势标准，眼神到位"
    }
    // ... 其他六式
  ],
  "overallFeedback": "整体表现优秀，动作流畅自然，呼吸配合良好",
  "suggestions": [
    "第三式'调理脾胃须单举'时，上举手下按的力度可以更强一些",
    "注意呼吸与动作的配合，特别是在起势和收势时"
  ],
  "highlights": [
    "起势收势规范",
    "动作连贯流畅",
    "眼神与动作配合良好"
  ]
}
```

## 评分标准

### 动作评分维度（每个维度满分 100 分）

1. **准确度 (accuracy)**：动作路线、方向、角度与标准动作的符合程度
2. **完整度 (completeness)**：动作是否完整到位，有无缺失或简化
3. **节奏 (timing)**：动作快慢适中，停顿恰当，与呼吸配合良好
4. **姿势 (posture)**：身体姿态端正，手型、步型规范

### 综合评分计算

- 每个动作得分 = (准确度 + 完整度 + 节奏 + 姿势) / 4
- 总分 = 八个动作得分的平均值

### 等级判定

| 分数范围 | 等级 | 是否及格 |
|---------|------|---------|
| 90-100  | 优秀 | pass: true |
| 80-89   | 良好 | pass: true |
| 60-79   | 及格 | pass: true |
| 60 以下  | 不及格 | pass: false |

## 八式动作名称

1. 两手托天理三焦
2. 左右开弓似射雕
3. 调理脾胃须单举
4. 五劳七伤往后瞧
5. 摇头摆尾去心火
6. 两手攀足固肾腰
7. 攒拳怒目增气力
8. 背后七颠百病消

## 注意事项

1. **JSON 格式**：AI 输出为纯 JSON，不包含 Markdown 标记
2. **异常处理**：调用方需要处理 AI 服务异常和 JSON 解析异常
3. **异步处理**：建议在异步任务中调用，避免阻塞主线程
4. **视频预处理**：需要先对视频进行帧提取和动作识别，将结果转为文本输入

## 相关文件

- 策略类：`hms-main/src/main/java/com/hms/ai/strategy/BaduanjinScoringStrategy.java`
- 工厂类：`hms-main/src/main/java/com/hms/ai/factory/AIChatFactory.java`
- 模板类：`hms-main/src/main/java/com/hms/ai/template/QwenAIChat.java`

package com.hms.test;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

/**
 * 八段锦评分 —— 真实接口端到端测试（与前台操作路径完全一致）
 * <p>
 * 流程：登录 → 取得视频（本地上传 或 复用服务器已有视频）→ 新增成绩记录
 * → 提交分析（/main/scoreRecord/submit）→ 每 5 秒轮询任务状态 → 校验分数与逐式结果 → 清理记录。
 * <p>
 * 默认不执行，只在显式开启时运行。三种取视频方式，优先级从上到下：
 * <pre>
 * 1) 复用服务器已有视频（最省事，不传文件）：
 *    mvn -pl hms-admin test -Dtest=BaduanjinApiE2ETest -De2e=true \
 *        -De2e.baseUrl=http://localhost:8080
 *
 * 2) 指定服务器上某个视频路径：
 *    ... -De2e.filePath=/profile/upload/2026/03/01/xxx.mp4
 *
 * 3) 上传本地视频（走 /common/upload，和前台选文件一样）：
 *    ... -De2e.video=D:\video\baduanjin.mp4
 * </pre>
 * 说明：
 * <ul>
 *   <li>必须指向服务器地址（localhost / 127.* 会被 checkEnvironment() 拒绝，这是生产保护）；</li>
 *   <li>服务器关闭图形验证码时用 -De2e.username/-De2e.password 自动登录，否则用 -De2e.token；</li>
 *   <li>测试会新建一条记录并在结束时删除，不修改既有数据。</li>
 * </ul>
 *
 * @author CYQ 2026-11-16
 */
@Tag("e2e")
@EnabledIfSystemProperty(named = "e2e", matches = "true")
@DisplayName("八段锦真实接口端到端测试")
public class BaduanjinApiE2ETest {

    private static final String BASE = System.getProperty("e2e.baseUrl", "http://localhost:8080")
            .replaceAll("/+$", "");
    private static final String USERNAME = System.getProperty("e2e.username", "user01");
    private static final String PASSWORD = System.getProperty("e2e.password", "user01");
    private static final String TOKEN_PROP = System.getProperty("e2e.token", "");
    private static final String FILE_PATH_PROP = System.getProperty("e2e.filePath", "");
    private static final String VIDEO_PATH = System.getProperty("e2e.video", "");
    private static final int TIMEOUT_SEC = Integer.getInteger("e2e.timeout", 600);
    private static final int POLL_INTERVAL_SEC = Integer.getInteger("e2e.interval", 5);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private String token;

    @Test
    @DisplayName("提交八段锦视频并完成一次真实评分")
    public void submitAndScore() throws Exception {
        token = resolveToken();

        // ① 确定视频来源
        JSONObject source = resolveSource();
        String fileUrl = source.getString("filePath");
        String fileName = source.getString("fileName");
        long fileSize = source.getLongValue("fileSize");
        Assertions.assertNotNull(fileUrl, "未取到视频路径");
        System.out.println("① 使用视频：" + fileUrl);

        // ② 学生与课程：复用服务器视频时直接沿用其归属，否则从下拉里挑一个已选课的
        Long studentId = source.getLong("studentId");
        Long courseId = source.getLong("courseId");
        if (studentId == null || courseId == null) {
            long[] sc = resolveStudentAndCourse();
            studentId = sc[0];
            courseId = sc[1];
        }
        System.out.println("② 学生 " + studentId + " / 课程 " + courseId);

        long recordId = 0;
        try {
            // ③ 新增成绩记录（等价于前台点「新增」并保存）
            JSONObject body = new JSONObject();
            body.put("studentId", studentId);
            body.put("courseId", courseId);
            body.put("filePath", fileUrl);
            body.put("fileName", fileName == null ? "" : fileName);
            body.put("fileSize", fileSize);
            body.put("analysisType", "八段锦");
            assertOk(post("/main/scoreRecord", body.toJSONString()), "新增成绩记录失败");

            recordId = findRecordId(studentId, courseId, fileUrl);
            Assertions.assertTrue(recordId > 0, "未找到刚新增的记录");
            System.out.println("③ 记录已创建：id=" + recordId);

            // ④ 提交分析（前台点「提交分析」）
            JSONObject submitBody = new JSONObject();
            submitBody.put("id", recordId);
            submitBody.put("filePath", fileUrl);
            submitBody.put("studentId", studentId);
            submitBody.put("courseId", courseId);
            long begin = System.currentTimeMillis();
            JSONObject submitResp = post("/main/scoreRecord/submit", submitBody.toJSONString());
            assertOk(submitResp, "提交分析失败");
            String taskId = submitResp.getJSONObject("data").getString("taskId");
            Assertions.assertNotNull(taskId, "提交后未返回 taskId");
            System.out.println("④ 已提交，taskId=" + taskId + "，开始轮询……");

            // ⑤ 轮询任务状态（状态机 0→1→2 / 3）
            JSONObject record = poll(taskId);
            long cost = (System.currentTimeMillis() - begin) / 1000;
            String status = record.getString("status");
            Assertions.assertEquals("2", status,
                    "分析未成功完成，状态=" + status + "，反馈=" + record.getString("feedback"));

            // ⑥ 校验结果
            Assertions.assertNotNull(record.getBigDecimal("score"), "已完成的记录应写入分数");
            String details = record.getString("analysisDetails");
            Assertions.assertNotNull(details, "已完成的记录应写入逐式分析详情");
            JSONObject parsed = JSONObject.parseObject(details);
            Assertions.assertTrue(parsed.containsKey("overall_score"), "缺少 overall_score");
            JSONArray movement = parsed.getJSONArray("movement_analysis");
            Assertions.assertNotNull(movement, "缺少 movement_analysis");
            Assertions.assertTrue(movement.size() > 0, "逐式分析为空");

            System.out.println("⑥ 评分完成：耗时 " + cost + " 秒，总分=" + record.getBigDecimal("score")
                    + "，等级=" + parsed.getString("grade_level")
                    + "，逐式条数=" + movement.size());
            System.out.println("   评语：" + record.getString("feedback"));
            System.out.println("   前三问题：" + parsed.getJSONArray("top_3_issues"));
        } finally {
            // ⑦ 清理：删除本次测试产生的记录，避免污染业务数据
            if (recordId > 0) {
                try {
                    HttpResponse<String> del = request("DELETE", "/main/scoreRecord/" + recordId, null, null);
                    System.out.println("⑦ 已清理测试记录：" + recordId + "（HTTP " + del.statusCode() + "）");
                } catch (Exception e) {
                    System.out.println("⑦ 清理失败（可手工删除 id=" + recordId + "）：" + e.getMessage());
                }
            }
        }
    }

    // ==================== 视频来源 ====================

    /**
     * 三种取视频方式：指定路径 &gt; 本地上传 &gt; 复用服务器已有视频
     */
    private JSONObject resolveSource() throws Exception {
        if (!FILE_PATH_PROP.isEmpty()) {
            JSONObject row = new JSONObject();
            row.put("filePath", FILE_PATH_PROP);
            return row;
        }

        if (!VIDEO_PATH.isEmpty()) {
            Path video = Paths.get(VIDEO_PATH);
            Assumptions.assumeTrue(Files.isReadable(video), "本地视频不可读：" + VIDEO_PATH);
            JSONObject uploaded = upload(video);
            uploaded.put("fileSize", Files.size(video));
            return uploaded;
        }

        // 复用服务器上已经存在、且评级成功的视频
        JSONObject list = get("/main/scoreRecord/list?pageNum=1&pageSize=100");
        JSONArray rows = list.getJSONArray("rows");
        Assumptions.assumeTrue(rows != null && !rows.isEmpty(), "服务器上没有成绩记录，无法复用视频");

        JSONObject best = null;
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String fp = row.getString("filePath");
            if (fp == null || !fp.toLowerCase().matches(".*\\.(mp4|mov|avi|m4v)$")) {
                continue;
            }
            // 优先取已成功评分过的，说明该视频可被 AI 正常拉取
            if (!"2".equals(row.getString("status"))) {
                continue;
            }
            if (best == null || row.getLongValue("id") > best.getLongValue("id")) {
                best = row;
            }
        }
        if (best == null) {
            // 没有成功样本就退而取任意视频
            for (int i = 0; i < rows.size(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String fp = row.getString("filePath");
                if (fp != null && fp.toLowerCase().matches(".*\\.(mp4|mov|avi|m4v)$")) {
                    if (best == null || row.getLongValue("id") > best.getLongValue("id")) {
                        best = row;
                    }
                }
            }
        }
        Assumptions.assumeTrue(best != null, "服务器上找不到可复用的视频");
        System.out.println("   复用服务器视频，来源记录 id=" + best.getLongValue("id"));
        return best;
    }

    /**
     * 取 token：优先使用 -De2e.token；否则在验证码关闭时用账号密码登录
     */
    private String resolveToken() throws Exception {
        if (!TOKEN_PROP.isEmpty()) {
            System.out.println("使用外部传入的 token");
            return TOKEN_PROP;
        }

        JSONObject captcha = get("/captchaImage");
        boolean captchaEnabled = captcha.getBooleanValue("captchaEnabled");
        Assumptions.assumeFalse(captchaEnabled,
                "服务器开启了图形验证码，无法自动登录：请用 -De2e.token=<token> 运行");

        JSONObject body = new JSONObject();
        body.put("username", USERNAME);
        body.put("password", PASSWORD);
        JSONObject login = post("/login", body.toJSONString());
        assertOk(login, "登录失败");
        String t = login.getString("token");
        Assertions.assertNotNull(t, "登录未返回 token");
        System.out.println("登录成功：" + USERNAME);
        return t;
    }

    // ==================== 业务步骤 ====================

    private JSONObject upload(Path video) throws Exception {
        String boundary = "----HmsBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] fileBytes = Files.readAllBytes(video);
        String fileName = video.getFileName().toString();

        byte[] head = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: video/mp4\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] payload = new byte[head.length + fileBytes.length + tail.length];
        System.arraycopy(head, 0, payload, 0, head.length);
        System.arraycopy(fileBytes, 0, payload, head.length, fileBytes.length);
        System.arraycopy(tail, 0, payload, head.length + fileBytes.length, tail.length);

        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/common/upload"))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
        JSONObject resp = JSONObject.parseObject(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
        assertOk(resp, "视频上传失败");
        System.out.println("   本地上传成功：" + resp.getString("url"));
        return resp;
    }

    private long[] resolveStudentAndCourse() throws Exception {
        JSONObject options = get("/main/scoreRecord/options");
        assertOk(options, "查询学生/课程下拉失败");
        JSONArray students = options.getJSONArray("students");
        Assumptions.assumeTrue(students != null && !students.isEmpty(), "系统内没有学生档案，无法测试");

        for (int i = 0; i < students.size(); i++) {
            long studentId = students.getJSONObject(i).getLongValue("studentId");
            JSONObject courses = get("/main/scoreRecord/coursesByStudent/" + studentId);
            JSONArray list = courses.getJSONArray("data");
            if (list != null && !list.isEmpty()) {
                return new long[]{studentId, list.getJSONObject(0).getLongValue("courseId")};
            }
        }
        Assumptions.assumeTrue(false, "没有找到「已选课」的学生，请先在课程管理里给学生选课");
        return null;
    }

    private long findRecordId(long studentId, long courseId, String fileUrl) throws Exception {
        String path = "/main/scoreRecord/list?pageNum=1&pageSize=50&studentId=" + studentId + "&courseId=" + courseId;
        JSONObject list = get(path);
        JSONArray rows = list.getJSONArray("rows");
        if (rows == null) {
            return 0;
        }
        long latest = 0;
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (fileUrl.equals(row.getString("filePath"))) {
                latest = Math.max(latest, row.getLongValue("id"));
            }
        }
        return latest;
    }

    private JSONObject poll(String taskId) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_SEC * 1000L;
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            JSONObject resp = get("/main/scoreRecord/task/" + taskId);
            assertOk(resp, "查询任务状态失败");
            JSONObject data = resp.getJSONObject("data");
            String status = resp.getString("status");
            if (!status.equals(last)) {
                System.out.println("   状态：" + status + "（" + resp.getString("statusDesc") + "）");
                last = status;
            }
            if ("2".equals(status) || "3".equals(status)) {
                return data;
            }
            Thread.sleep(POLL_INTERVAL_SEC * 1000L);
        }
        Assertions.fail("等待 " + TIMEOUT_SEC + " 秒仍未完成，最后状态：" + last);
        return null;
    }

    // ==================== HTTP 基础方法 ====================

    private JSONObject get(String path) throws Exception {
        return parse(request("GET", path, null, null));
    }

    private JSONObject post(String path, String json) throws Exception {
        return parse(request("POST", path, json, "application/json"));
    }

    private HttpResponse<String> request(String method, String path, String body, String contentType) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + token);
        if (body != null) {
            b.header("Content-Type", contentType == null ? "application/json" : contentType);
            b.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JSONObject parse(HttpResponse<String> resp) {
        String body = resp.body();
        Assumptions.assumeFalse(body == null || body.isEmpty(), "接口返回为空，HTTP " + resp.statusCode());
        return JSONObject.parseObject(body);
    }

    private void assertOk(JSONObject resp, String message) {
        Integer code = resp.getInteger("code");
        Assertions.assertEquals(200, code, message + "：" + resp.getString("msg"));
    }
}

package com.hms.quartz.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hms.common.utils.hms.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * 社区打卡定时任务 - 通过 Playwright 脚本自动登录 + 签到
 *
 * add by CYQ 2024年9月12日 原始版本（ncc http打卡）
 * add by CYQ 2026年5月20日 改造为社区登录 + js打卡
 * */
@Component("NCCPunchCard")
public class NCCPunchCard {
    private static final Logger log = LoggerFactory.getLogger(NCCPunchCard.class);

    /** 打卡 */
    public String task() throws Exception {
        log.info("正在调用社区打卡功能");
        String result = communityLogin(true);
        log.info("社区打卡完成: {}", result);
        return result;
    }

    /**
     * 社区登录（不含打卡）
     * */
    public String communityLogin() throws Exception {
        return communityLogin(false);
    }

    /**
     * 社区登录 + 签到打卡
     * 
     * @param doClock true=登录后自动签到
     * @return JSON 结果字符串
     *
     * add by CYQ 2026年5月20日
     * */
    public String communityLogin(boolean doClock) throws Exception {
        try {
            String nodePath = findNodeExecutable();
            String scriptPath = resolvePlaywrightScript();
            log.info("node: {}, script: {}", nodePath, scriptPath);

            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(nodePath);
            cmd.add(scriptPath);
            if (doClock) {
                cmd.add("--clock");
            }

            File workDir = resolveWorkDir(scriptPath);
            log.info("工作目录: {}", workDir.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("脚本执行失败, exitCode=" + exitCode + ", output=" + output);
            }

            String resultStr = output.toString().trim();
            log.info("脚本返回: {}", resultStr);
            JSONObject result = JSON.parseObject(resultStr);

            if (!result.getBooleanValue("success")) {
                throw new Exception("登录失败: " + result.getString("error"));
            }

            if (doClock) {
                boolean clocked = result.getBooleanValue("clocked");
                String clockMsg = result.getString("clockMsg");
                log.info("打卡结果: clocked={}, msg={}", clocked, clockMsg);
            }

            return resultStr;
        } catch (Exception e) {
            throw new Exception("communityLogin失败," + e.getMessage());
        }
    }

    /**
     * 查找 node 可执行文件路径
     * NODE_PATH -> login shell -> sh shell -> nvm路径 -> 系统路径
     *
     * add by CYQ 2026年5月20日
     * */
    private String findNodeExecutable() throws Exception {
        String envNode = System.getenv("NODE_PATH");
        if (envNode != null && !envNode.isEmpty() && new File(envNode).exists()) {
            return envNode;
        }

        String whichNode = execShell("bash -l -c 'which node'");
        if (!whichNode.isEmpty() && new File(whichNode).exists()) {
            return whichNode;
        }

        whichNode = execShell("which node");
        if (!whichNode.isEmpty() && new File(whichNode).exists()) {
            return whichNode;
        }

        String home = System.getProperty("user.home");
        String[] nvmBases = {
            home + "/.nvm/versions/node",
            "/www/server/nvm/versions/node",
            "/usr/local/nvm/versions/node"
        };
        for (String base : nvmBases) {
            File nvmDir = new File(base);
            if (nvmDir.exists() && nvmDir.isDirectory()) {
                File[] versions = nvmDir.listFiles();
                if (versions != null) {
                    for (File v : versions) {
                        File nodeBin = new File(v, "bin/node");
                        if (nodeBin.exists()) return nodeBin.getAbsolutePath();
                    }
                }
            }
        }

        for (String p : new String[]{"/usr/local/bin/node", "/usr/bin/node", "/opt/homebrew/bin/node"}) {
            if (new File(p).exists()) return p;
        }

        throw new Exception("未找到node，请设置NODE_PATH环境变量指向node可执行文件");
    }

    /**
     * 解析脚本路径：PLAYWRIGHT_SCRIPT 环境变量 > jar同级 playwright/
     *
     * add by CYQ 2026年5月20日
     * */
    private String resolvePlaywrightScript() throws Exception {
        String envScript = System.getenv("PLAYWRIGHT_SCRIPT");
        if (envScript != null && !envScript.isEmpty() && new File(envScript).exists()) {
            return envScript;
        }

        File localScript = new File("playwright/login-community.js");
        if (localScript.exists()) {
            return localScript.getAbsolutePath();
        }

        throw new Exception("未找到playwright/login-community.js，"
                + "请在jar同级目录创建playwright/并放入login-community.js + package.json，"
                + "执行 npm install 安装依赖。或设置 PLAYWRIGHT_SCRIPT 环境变量指定脚本路径");
    }

    private File resolveWorkDir(String scriptPath) {
        return new File(scriptPath).getParentFile();
    }

    private String execShell(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            p.waitFor();
            return CommonUtil.initstr(line);
        } catch (Exception e) {
            return "";
        }
    }
}
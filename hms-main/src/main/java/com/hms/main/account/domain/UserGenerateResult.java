package com.hms.main.account.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量生成系统账号结果对象
 *
 * @author CYQ
 * @date 2026-10-18
 * add by CYQ 2026-10-18 说明: 「一键生成账号」批量执行的结果统计，
 *   逐条记录成功/失败原因，便于前端提示与问题排查。
 */
public class UserGenerateResult
{
    /** 总条数 */
    private int total;

    /** 成功条数 */
    private int success;

    /** 失败条数 */
    private int failure;

    /** 明细信息（成功/失败逐条说明） */
    private List<String> details = new ArrayList<>();

    public UserGenerateResult()
    {
    }

    public UserGenerateResult(int total)
    {
        this.total = total;
    }

    public void addSuccess(String message)
    {
        this.success++;
        this.details.add("【成功】" + message);
    }

    public void addFailure(String message)
    {
        this.failure++;
        this.details.add("【失败】" + message);
    }

    /** 汇总提示语，供前端直接展示 */
    public String getSummary()
    {
        return "共处理 " + total + " 条，成功 " + success + " 条，失败 " + failure + " 条";
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal(int total)
    {
        this.total = total;
    }

    public int getSuccess()
    {
        return success;
    }

    public void setSuccess(int success)
    {
        this.success = success;
    }

    public int getFailure()
    {
        return failure;
    }

    public void setFailure(int failure)
    {
        this.failure = failure;
    }

    public List<String> getDetails()
    {
        return details;
    }

    public void setDetails(List<String> details)
    {
        this.details = details;
    }
}

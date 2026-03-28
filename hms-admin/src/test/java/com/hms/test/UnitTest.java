package com.hms.test;

import cn.hutool.core.date.ChineseDate;
import com.hms.common.config.RuoYiConfig;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.Lunar;
import com.hms.common.utils.SecurityUtils;
import com.hms.main.summary.controller.HsmSummaryController;
import com.hms.main.summary.domain.HsmSummary;
import com.hms.main.summary.service.IHsmSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.Date;

import com.hms.common.core.domain.AjaxResult;

@SpringBootTest
@DisplayName("单元测试案例")
public class UnitTest {

    @Autowired
    private RuoYiConfig ruoYiConfig;
    @Autowired
    private IHsmSummaryService hsmSummaryService;
    @Autowired
    private HsmSummaryController hsmSummaryController;

    @DisplayName("检查配置")
    @Test
    public void test1(){
        System.out.println(ruoYiConfig);
    }
    @DisplayName("查询财务汇总主列表")
    @Test
    public void selectHsmSummaryList(){
        HsmSummary hsmSummary = new HsmSummary();
        System.out.println(hsmSummaryService.selectHsmSummaryList(hsmSummary));
    }
    @DisplayName("检查支出")
    @Test
    public void checkExpense(){
        System.out.println(hsmSummaryController.checkExpense());
    }

//    @DisplayName("获取存款计划执行情况")
//    @Test
//    public void testDepositPlanData(){
//        System.out.println("=== 存款计划执行情况测试 ===");
//        System.out.println(hsmSummaryController.getDepositPlanData());
//    }

//    @DisplayName("获取存款预测数据")
//    @Test
//    public void testDepositPrediction(){
//        System.out.println("=== 存款预测数据测试 ===");
//        System.out.println(hsmSummaryController.getDepositPrediction());
//    }

    @DisplayName("测试年度支出数据补零功能")
    @Test
    public void testYearlyExpenseData(){
        System.out.println("=== 年度支出数据补零测试 ===");
        System.out.println(hsmSummaryService.getYearlyExpenseData());
    }

    @DisplayName("测试财务汇总初始化")
    @Test
    public void testInitHsmSummary(){
        System.out.println("=== 财务汇总初始化测试 ===");
        System.out.println(hsmSummaryService.initHsmSummary());
    }

    @DisplayName("测试根据ID查询财务汇总")
    @Test
    public void testSelectHsmSummaryById(){
        System.out.println("=== 根据ID查询财务汇总测试 ===");
        // 假设存在ID为1的记录，实际测试时可能需要调整
        try {
            System.out.println(hsmSummaryService.selectHsmSummaryById(1L));
        } catch (Exception e) {
            System.out.println("查询失败，可能ID不存在: " + e.getMessage());
        }
    }

    @DisplayName("测试新增财务汇总")
    @Test
    public void testInsertHsmSummary(){
        System.out.println("=== 新增财务汇总测试 ===");
        HsmSummary hsmSummary = new HsmSummary();
        // 设置测试数据
        hsmSummary.setYear(2025L);
        hsmSummary.setMonth(1L);
        hsmSummary.setIncome(new BigDecimal("10000.00"));
        hsmSummary.setExpense(new BigDecimal("8000.00"));
        hsmSummary.setBalance(new BigDecimal("2000.00"));
        hsmSummary.setStartDeposit(new BigDecimal("50000.00"));
        hsmSummary.setEndDeposit(new BigDecimal("52000.00"));
        
        try {
            int result = hsmSummaryService.insertHsmSummary(hsmSummary);
            System.out.println("新增结果: " + result);
        } catch (Exception e) {
            System.out.println("新增失败: " + e.getMessage());
        }
    }

    @DisplayName("测试更新财务汇总")
    @Test
    public void testUpdateHsmSummary(){
        System.out.println("=== 更新财务汇总测试 ===");
        HsmSummary hsmSummary = new HsmSummary();
        hsmSummary.setId(1L); // 假设存在ID为1的记录
        hsmSummary.setIncome(new BigDecimal("12000.00"));
        hsmSummary.setExpense(new BigDecimal("9000.00"));
        hsmSummary.setBalance(new BigDecimal("3000.00"));
        hsmSummary.setStartDeposit(new BigDecimal("52000.00"));
        hsmSummary.setEndDeposit(new BigDecimal("55000.00"));
        
        try {
            int result = hsmSummaryService.updateHsmSummary(hsmSummary);
            System.out.println("更新结果: " + result);
        } catch (Exception e) {
            System.out.println("更新失败: " + e.getMessage());
        }
    }

    @DisplayName("测试删除财务汇总")
    @Test
    public void testDeleteHsmSummaryByIds(){
        System.out.println("=== 删除财务汇总测试 ===");
        Long[] ids = {1L}; // 假设存在ID为1的记录
        
        try {
            int result = hsmSummaryService.deleteHsmSummaryByIds(ids);
            System.out.println("删除结果: " + result);
        } catch (Exception e) {
            System.out.println("删除失败: " + e.getMessage());
        }
    }

//    @DisplayName("测试Controller层存款计划接口")
//    @Test
//    public void testControllerDepositPlan(){
//        System.out.println("=== Controller存款计划接口测试 ===");
//        try {
//            AjaxResult result = hsmSummaryController.getDepositPlanData();
//            System.out.println("Controller返回结果: " + result);
//        } catch (Exception e) {
//            System.out.println("Controller调用失败: " + e.getMessage());
//        }
//    }

//    @DisplayName("测试Controller层预测接口")
//    @Test
//    public void testControllerPrediction(){
//        System.out.println("=== Controller预测接口测试 ===");
//        try {
//            AjaxResult result = hsmSummaryController.getDepositPrediction();
//            System.out.println("Controller返回结果: " + result);
//        } catch (Exception e) {
//            System.out.println("Controller调用失败: " + e.getMessage());
//        }
//    }
    @DisplayName("查询最新汇总明细按类型分组")
    @Test
    public void getLastSummaryDetailByType(){
        System.out.println("=== Controller查询最新汇总明细按类型分组测试 ===");
        try {
//            Long userId = SecurityUtils.getUserId();
            AjaxResult result = hsmSummaryController.getLastSummaryDetailByType();
            System.out.println("Controller返回结果: " + result);
        } catch (Exception e) {
            System.out.println("Controller调用失败: " + e.getMessage());
        }
    }
    @DisplayName("测试日历逻辑")
    @Test
    public void testDate(){
        Date currentDate = new Date();
        String gl_date = DateUtils.parseDateToStr("MM-dd", currentDate);//公历校验时间
        String nl_date = Lunar.getDate(currentDate);//农历校验时间
        ChineseDate chineseDate = new ChineseDate(currentDate);
        System.out.println(chineseDate.toString());
    }
}
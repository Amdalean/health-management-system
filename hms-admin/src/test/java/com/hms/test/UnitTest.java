package com.hms.test;

import com.hms.common.config.RuoYiConfig;
import com.hms.main.summary.controller.HsmSummaryController;
import com.hms.main.summary.domain.HsmSummary;
import com.hms.main.summary.service.IHsmSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
        System.out.println(hsmSummaryController.forms());
    }

}
package com.hms.main.summary.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HsmSummaryPlanDTO {
    String year;
    String month;
    String date;
    BigDecimal actualDeposit;//实际存款
    BigDecimal predictedDeposit;//预测存款
    BigDecimal plannedDeposit;//计划存款
}

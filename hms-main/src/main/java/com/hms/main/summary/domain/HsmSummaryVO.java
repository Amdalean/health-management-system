package com.hms.main.summary.domain;

import lombok.Data;

@Data
public class HsmSummaryVO extends HsmSummary{
    private String createByName;//创建人名称
    private String updateByName;//修改人名称
}

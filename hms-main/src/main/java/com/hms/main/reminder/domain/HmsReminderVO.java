package com.hms.main.reminder.domain;

import lombok.Data;

@Data
public class HmsReminderVO extends HmsReminder{
    private Long countdown;//倒计时
    private String createByName;//创建人名称
    private String updateByName;//修改人名称
}

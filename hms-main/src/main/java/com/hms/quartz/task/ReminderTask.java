package com.hms.quartz.task;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.util.ObjectUtil;
import com.hms.ai.factory.AIChatFactory;
import com.hms.common.config.MailConfig;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.hms.MDUtil;
import com.hms.main.reminder.domain.HmsReminder;
import com.hms.main.reminder.service.IHmsReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;

@Component("ReminderTask")
public class ReminderTask {

    private static final Logger log = LoggerFactory.getLogger(ReminderTask.class);

    @Autowired
    private IHmsReminderService hmsReminderService;

    @Autowired
    private JavaMailSender mailSender;
//    public static void main(String[] args){
////        廿五
//        Date checkDate = DateUtils.addDays(new Date(), 15); // 获取明天的日期
//        ChineseDate chineseDate = new ChineseDate(checkDate);
//        String nlDate = chineseDate.getChineseMonth()+chineseDate.getChineseDay();//农历校验时间
//        String nlNameDate = chineseDate.getChineseMonthName()+chineseDate.getChineseDay();//农历校验时间
//        System.out.println(nlDate);
//        System.out.println(nlNameDate);
//    }
    public String excute() {
        try {
            log.info("开始执行提醒任务...");

            // 获取当前日期

            Date checkDate = DateUtils.addDays(new Date(), 1); // 获取明天的日期
            String gl_date = DateUtils.parseDateToStr("MM-dd", checkDate);//公历校验时间
            ChineseDate chineseDate = new ChineseDate(checkDate);
            String nlDate = chineseDate.getChineseMonth()+chineseDate.getChineseDay();//农历校验时间
            String nlNameDate = chineseDate.getChineseMonthName()+chineseDate.getChineseDay();//农历校验时间
            String currentDateString = DateUtils.dateTime(checkDate);
            log.info("校验日期: {}", currentDateString);

            // 查询所有启用的提醒记录
            HmsReminder reminderQuery = new HmsReminder();
            reminderQuery.setStatus("1"); // 只查询启用的提醒
            List<HmsReminder> reminderList = hmsReminderService.selectHmsReminderList(reminderQuery);

            log.info("找到 {} 条启用的提醒记录", reminderList.size());

            int successCount = 0;
            int failCount = 0;

            for (HmsReminder reminder : reminderList) {
                try {
                    boolean isMatch = false;

                    String reminderDateStr = reminder.getRemindDate();
                    // 根据历法类型判断是否匹配当前日期
                    if ("0".equals(reminder.getCalendarType())) {
                        // 公历比较
                        if (gl_date.equals(reminderDateStr)) {
                            isMatch = true;
                        }
                    } else if ("1".equals(reminder.getCalendarType())) {
                        // 农历比较
                        if (nlDate.equals(reminderDateStr) || nlNameDate.equals(reminderDateStr)) {
                            isMatch = true;
                        }
                    }

                    if (isMatch) {
                        log.info("发现匹配的提醒: ID={}, 标题={}, 类型={}",
                                reminder.getId(), reminder.getTitle(),
                                "1".equals(reminder.getCalendarType()) ? "农历" : "公历");

                        // 调用AI生成提醒内容
                        String aiPrompt = reminder.getAiPrompt();
                        String req = "明天("+reminder.getRemindDate()+")是：" + reminder.getTitle()+";";
                        if(!ObjectUtil.isEmpty(aiPrompt)){
                            req += "\n" + aiPrompt;
                        }
                        String aiResponse = AIChatFactory.createAIChat(AIChatFactory.StrategyType.Reminder).chat(req, null);
                        String html = MDUtil.mdtohtml(aiResponse);
                        // 发送邮件
                        sendEmail(reminder.getRemindEmail(), reminder.getTitle(), html);

                        successCount++;
                        log.info("成功发送提醒邮件给: {}", reminder.getRemindEmail());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("处理提醒记录时发生错误，ID: {}, 错误: {}", reminder.getId(), e.getMessage(), e);
                }
            }

            String result = String.format("提醒任务执行完成，成功发送: %d 封邮件，失败: %d 封", successCount, failCount);
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("执行提醒任务时发生异常", e);
            return "执行提醒任务失败: " + e.getMessage();
        }
//        return "执行提醒任务成功";
    }

    /**
     * 发送邮件
     */
    private void sendEmail(String toEmail, String subject, String content) throws Exception {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(MailConfig.getMe());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); // true 表示内容为HTML格式

            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送邮件失败，收件人: {}, 主题: {}", toEmail, subject, e);
            throw e;
        }
    }
}

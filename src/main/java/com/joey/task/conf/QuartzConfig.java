package com.joey.task.conf;

import com.joey.task.task.PterDelMsgJob;
import com.joey.task.task.PterJob;
import com.joey.task.task.SHTJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Value("${quartz.cron}")
    private String cron; // corn表达式

    @Value("${quartz.shtcron}")
    private String shtcron;

    @Value("${quartz.delptermsgcron}")
    private String delptermsgcron;

    @Bean
    public JobDetail pterJobDetail() {
        return JobBuilder.newJob(PterJob.class).withIdentity("PterJob").storeDurably().build();
    }

    @Bean
    public JobDetail shtJobDetail() {
        return JobBuilder.newJob(SHTJob.class).withIdentity("SHTJob").storeDurably().build();
    }

    @Bean
    public JobDetail pterDelMsgJobDetail() {
        return JobBuilder.newJob(PterDelMsgJob.class).withIdentity("PterDelMsgJob").storeDurably().build();
    }

    @Bean
    public Trigger restartTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
        CronScheduleBuilder shtScheduleBuilder = CronScheduleBuilder.cronSchedule(shtcron);
        CronScheduleBuilder delptermsgScheduleBuilder = CronScheduleBuilder.cronSchedule(delptermsgcron);
        return TriggerBuilder.newTrigger()
                .forJob(pterJobDetail()).withIdentity("PterJob").withSchedule(scheduleBuilder)
                .forJob(pterDelMsgJobDetail()).withIdentity("pterDelMsgJobDetail").withSchedule(delptermsgScheduleBuilder)
                .forJob(shtJobDetail()).withIdentity("SHTJob").withSchedule(shtScheduleBuilder).build();
    }

}

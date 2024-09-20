package com.cyt.community.config;

import com.cyt.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetailFactoryBean postScoreJobDetail(){
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        jobDetailFactoryBean.setDurability(true);
        jobDetailFactoryBean.setGroup("communityJobGroup");
        jobDetailFactoryBean.setName("postScoreRefreshJob");
        jobDetailFactoryBean.setJobClass(PostScoreRefreshJob.class);
        return jobDetailFactoryBean;
    }
    @Bean
    public SimpleTriggerFactoryBean postScoreTrigger(JobDetail postScoreJobDetail){
        SimpleTriggerFactoryBean simpleTriggerFactoryBean = new SimpleTriggerFactoryBean();
        simpleTriggerFactoryBean.setJobDetail(postScoreJobDetail);
        simpleTriggerFactoryBean.setGroup("communityTriggerGroup");
        simpleTriggerFactoryBean.setName("postScoreRefreshTrigger");
        simpleTriggerFactoryBean.setJobDataMap(new JobDataMap());
        simpleTriggerFactoryBean.setRepeatInterval(1000 * 60 * 5);
        return simpleTriggerFactoryBean;
    }

}

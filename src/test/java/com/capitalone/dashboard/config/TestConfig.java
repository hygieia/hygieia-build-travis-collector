package com.capitalone.dashboard.config;

import com.capitalone.dashboard.collector.TravisBuildSettings;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;

/**
 * Spring context configuration for Testing purposes
 */
@Order(1)
@Configuration
@ComponentScan(basePackages ={ "com.capitalone.dashboard.model","com.capitalone.dashboard.collector", "com.capitalone.dashboard.repository"}, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value=com.capitalone.dashboard.collector.TravisBuildSettings.class))
public class TestConfig {

    @Bean
    public TravisBuildSettings travisBuildSettings() {
        TravisBuildSettings settings = new TravisBuildSettings();
        settings.setCron("* * * * * *");
        settings.setHost("travis-ci.com");


        return settings;
    }
    @Bean
    public TaskScheduler taskScheduler() { return Mockito.mock(TaskScheduler.class); }
}

package com.starfish.test.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(value = {"xxl.job.enabled"}, havingValue = "true")
@EnableConfigurationProperties({XxlJobProperties.class})
public class XxlJobAutoConfiguration {

    @Autowired
    private XxlJobProperties properties;

    @Bean
    public XxlJobNoNettyExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job (no-netty) config init.");
        XxlJobNoNettyExecutor executor = new XxlJobNoNettyExecutor();
        XxlJobProperties.Admin admin = properties.getAdmin();
        XxlJobProperties.Executor executorProps = properties.getExecutor();
        executor.setAdminAddresses(admin.getAddresses());
        executor.setAddress(executorProps.getAddress());
        executor.setAppname(executorProps.getAppname());
        executor.setIp(executorProps.getIp());
        executor.setPort(executorProps.getPort());
        executor.setAccessToken(properties.getAccessToken());
        executor.setLogPath(executorProps.getLogpath());
        executor.setLogRetentionDays(executorProps.getLogretentiondays());
        log.info(">>>>>>>>>>> xxl-job (no-netty) config init complete.");
        return executor;
    }

    @Bean
    public XxlJobExecutorController xxlJobExecutorController(XxlJobNoNettyExecutor executor) {
        return new XxlJobExecutorController(executor);
    }
}
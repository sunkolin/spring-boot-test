package com.starfish.test.config;

import com.starfish.common.task.XxlJobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * TaskAutoConfiguration
 *
 * @author sunkolin
 * @version 1.0.0
 * @since 2015-08-03
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(value = {"starfish.xxl.job.enabled"}, havingValue = "true")
@EnableConfigurationProperties({XxlJobProperties.class})
public class XxlJobAutoConfiguration {

    @Autowired
    private XxlJobProperties properties;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        XxlJobProperties.Admin admin = properties.getAdmin();
        XxlJobProperties.Executor executor = properties.getExecutor();
        xxlJobSpringExecutor.setAdminAddresses(admin.getAddresses());
        xxlJobSpringExecutor.setAddress(executor.getAddress());
        xxlJobSpringExecutor.setAppname(executor.getAppname());
        xxlJobSpringExecutor.setIp(executor.getIp());
        xxlJobSpringExecutor.setPort(executor.getPort());
        xxlJobSpringExecutor.setAccessToken(properties.getAccessToken());
        xxlJobSpringExecutor.setLogPath(executor.getLogpath());
        xxlJobSpringExecutor.setLogRetentionDays(executor.getLogretentiondays());
        log.info(">>>>>>>>>>> xxl-job config init complete.");
        return xxlJobSpringExecutor;
    }

}

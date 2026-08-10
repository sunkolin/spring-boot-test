package com.starfish.test.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starfish.test.entity.UserEntity;
import com.starfish.test.mapper.UserMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Component
public class UserCountTask {

    @Resource
    private UserMapper userMapper;

    @XxlJob("userCountTask")
    public void userCountTask(){
        log.info("UserCountStatisticsTask定时任务, 开始执行");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        LambdaQueryWrapper<UserEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        long count = userMapper.selectCount(lambdaQueryWrapper);
        log.info("UserCountStatisticsTask定时任务, count={}", count);

        stopWatch.stop();
        log.info("UserCountStatisticsTask定时任务, 执行完成,耗时={}", stopWatch.prettyPrint());
    }

}

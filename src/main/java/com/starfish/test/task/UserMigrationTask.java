package com.starfish.test.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starfish.test.entity.UserEntity;
import com.starfish.test.entity.UserHistoryEntity;
import com.starfish.test.mapper.UserHistoryMapper;
import com.starfish.test.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class UserMigrationTask implements ApplicationRunner {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserHistoryMapper userHistoryMapper;

    @Value("${user.migration.page-size:1000}")
    private int pageSize;

    @Override
    public void run(ApplicationArguments args) {
        log.info("UserMigrationTask 开始执行, pageSize={}", pageSize);
        StopWatch totalStopWatch = new StopWatch("用户数据迁移总耗时");
        totalStopWatch.start();

        long totalMigrated = 0;
        long totalDeleted = 0;
        Date lastMaxCreateTime = null;

        while (true) {
            StopWatch pageStopWatch = new StopWatch();

            pageStopWatch.start("查询数据");
            LambdaQueryWrapper<UserEntity> queryWrapper = new LambdaQueryWrapper<>();
            if (lastMaxCreateTime != null) {
                queryWrapper.ge(UserEntity::getCreateTime, lastMaxCreateTime);
            }
            queryWrapper.orderByAsc(UserEntity::getCreateTime);
            queryWrapper.last("LIMIT " + pageSize);
            List<UserEntity> users = userMapper.selectList(queryWrapper);
            pageStopWatch.stop();

            if (users == null || users.isEmpty()) {
                log.info("UserMigrationTask 无更多数据, 结束迁移");
                break;
            }

            pageStopWatch.start("插入历史表");
            List<UserHistoryEntity> historyList = new ArrayList<>(users.size());
            for (UserEntity user : users) {
                UserHistoryEntity history = new UserHistoryEntity();
                BeanUtils.copyProperties(user, history);
                historyList.add(history);
            }
            userHistoryMapper.insert(historyList);
            pageStopWatch.stop();

            pageStopWatch.start("删除原表数据");
            List<Long> userIds = new ArrayList<>(users.size());
            for (UserEntity user : users) {
                userIds.add(user.getId());
            }
            userMapper.deleteByIds(userIds);
            pageStopWatch.stop();

            lastMaxCreateTime = users.get(users.size() - 1).getCreateTime();
            totalMigrated = totalMigrated + users.size();
            totalDeleted = totalDeleted + users.size();

            log.info("UserMigrationTask 迁移进度, 本页迁移{}条, 累计迁移{}条, 当前最大createTime={}",users.size(), totalMigrated, lastMaxCreateTime);
            log.info("UserMigrationTask 本页耗时:\n{}", pageStopWatch.prettyPrint());
        }

        totalStopWatch.stop();
        log.info("UserMigrationTask 执行完成, 总迁移={}条, 总删除={}条", totalMigrated, totalDeleted);
        log.info("UserMigrationTask 总耗时:\n{}", totalStopWatch.prettyPrint());
    }

}
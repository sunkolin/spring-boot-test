package com.starfish.test.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        long pageNumber = 1;

        while (true) {
            StopWatch pageStopWatch = new StopWatch("第" + pageNumber + "页迁移");

            pageStopWatch.start("查询数据");
            LambdaQueryWrapper<UserEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByAsc(UserEntity::getId);
            Page<UserEntity> page = userMapper.selectPage(new Page<>(pageNumber, pageSize, false), queryWrapper);
            List<UserEntity> users = page.getRecords();
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
                history.setId(null);
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

            totalMigrated += users.size();
            totalDeleted += users.size();

            log.info("UserMigrationTask 第{}页完成, 本页迁移{}条, 累计迁移{}条",
                    pageNumber, users.size(), totalMigrated);
            log.info("UserMigrationTask 第{}页耗时详情:\n{}", pageNumber, pageStopWatch.prettyPrint());

            pageNumber++;
        }

        totalStopWatch.stop();
        log.info("UserMigrationTask 执行完成, 总迁移={}条, 总删除={}条", totalMigrated, totalDeleted);
        log.info("UserMigrationTask 总耗时:\n{}", totalStopWatch.prettyPrint());
    }

}
package com.starfish.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.starfish.experiment.jwt.JsonWebTokens;
import com.starfish.test.context.User;
import com.starfish.test.entity.UserEntity;
import com.starfish.test.enumeration.ResultEnum;
import com.starfish.core.exception.CustomException;
import com.starfish.test.mapper.UserMapper;
import com.starfish.test.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * UserService
 *
 * @author sunkolin
 * @version 1.0.0
 * @since 2022-07-19
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

//    @Autowired
//    private RedisTemplate<Object, Object> redisTemplate;
//
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Long register(UserEntity userEntity) {
        // 查询用户是否已经注册过
        UserEntity param = new UserEntity();
        param.setMobile(userEntity.getMobile());
        UserEntity result = userMapper.selectOne(new QueryWrapper<>(param));
        if (result != null) {
            log.error("UserServiceImpl register fail,mobile already register,mobile={},userId={}", userEntity.getMobile(), result.getId());
            throw new CustomException(ResultEnum.ALREADY_REGISTER);
        }

        userMapper.insert(userEntity);
        return userEntity.getId();
    }

    @Override
    public User login(UserEntity userEntity) {
        UserEntity param = new UserEntity();
        param.setMobile(userEntity.getMobile());
        param.setPassword(userEntity.getPassword());
        UserEntity result = userMapper.selectOne(new QueryWrapper<>(param));
        if (result == null) {
            log.error("UserServiceImpl login fail.mobile={},password={}", userEntity.getMobile(), userEntity.getPassword());
            throw new CustomException(ResultEnum.LOGIN_FAIL);
        }
        log.info("UserServiceImpl login success.mobile={},password={}", userEntity.getMobile(), userEntity.getPassword());

        // 生成 token
        User user = new User();
        Long userId = result.getId();
        user.setUserId(userId);
        user.setNickName(result.getNickName());
        user.setLastLoginTime(new Date());
        String token = JsonWebTokens.create(userId, user);
        user.setToken(token);
        return user;
    }

    @Override
    public void logout(UserEntity userEntity) {
        Long userId = userEntity.getId();
        // 清理登录数据
        log.info("UserServiceImpl logout success.userId={}", userId);
    }

    @Override
    public void delete(Long userId) {
        userMapper.deleteById(userId);
    }

    @Override
    public void update(UserEntity userEntity) {
        userMapper.updateById(userEntity);
    }

    @Override
    public UserEntity getUser(Long userId) {
        String key = "starfish:spring-boot4-test:" + userId;
//        if (redisTemplate.hasKey(key)) {
//            Object result = redisTemplate.opsForValue().get(key);
//            return result != null ? (UserEntity) result : null;
//        } else {
            UserEntity userEntity = userMapper.selectById(userId);
            // 缓存一个小时
//            Duration duration = Duration.ofSeconds(60 * 60);
//            redisTemplate.opsForValue().set(key, userEntity, duration);
            return userEntity;
//        }
    }

    @Override
    public Page<UserEntity> list(Long pageNumber, Long pageSize) {
        Page<UserEntity> page = new Page<>(pageNumber, pageSize, false);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(UserEntity::getId, 1L);
        wrapper.orderByAsc(UserEntity::getId);
        return userMapper.selectPage(page, wrapper);
    }

}

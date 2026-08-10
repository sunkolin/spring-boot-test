package com.starfish.test.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.starfish.core.context.User;
import com.starfish.test.context.User;
import com.starfish.test.entity.UserEntity;

/**
 * UserService
 *
 * @author sunkolin
 * @version 1.0.0
 * @since 2022-07-19
 */
public interface UserService {

    /**
     * 注册
     *
     * @param userEntity 用户
     */
//    Long register(UserEntity userEntity);

    /**
     * 登录
     *
     * @param userEntity 用户
     */
//    User login(UserEntity userEntity);

    /**
     * 登录
     *
     * @param userEntity 用户
     */
    void logout(UserEntity userEntity);

    /**
     * 查询用户
     *
     * @param userId 用户ID
     */
    void delete(Long userId);

    /**
     * 修改用户信息
     *
     * @param userEntity 用户信息
     */
    void update(UserEntity userEntity);

    /**
     * 查询用户信息
     *
     * @param userId 用户id
     * @return 结果
     */
    UserEntity getUser(Long userId);

    /**
     * 分页查询用户列表
     *
     * @param pageNumber 页数
     * @param pageSize   每页数量
     * @return 结果
     */
    Page<UserEntity> list(Long pageNumber, Long pageSize);

}

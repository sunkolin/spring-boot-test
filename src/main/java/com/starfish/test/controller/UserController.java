//package com.starfish.test.controller;
//
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.google.common.base.Strings;
//import com.starfish.core.annotation.RequireLogin;
//import com.starfish.core.context.User;
//import com.starfish.core.context.UserContext;
//import com.starfish.core.exception.CustomException;
//import com.starfish.core.model.Result;
//import com.starfish.test.entity.UserEntity;
//import com.starfish.test.enumeration.ResultEnum;
//import com.starfish.test.param.DeleteUserParam;
//import com.starfish.test.param.GetUserParam;
//import com.starfish.test.param.UpdateUserParam;
//import com.starfish.test.service.UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeanUtils;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * UserController
// *
// * @author sunkolin
// * @version 1.0.0
// * @since 2022-07-14
// */
//@Slf4j
//@Tag(name = "UserController", description = "用户接口")
//@RestController
//public class UserController {
//
//    @Resource
//    private UserService userService;
//
//    @RequireLogin(false)
//    @PostMapping("/api/user/register")
//    public Result<UserEntity> register(@RequestBody UserEntity userEntity) {
//        // 验证参数，手机号，密码必传
//        String mobile = userEntity.getMobile();
//        String password = userEntity.getPassword();
//        if (Strings.isNullOrEmpty(mobile)) {
//            throw new CustomException(ResultEnum.PARAM_ERROR);
//        }
//        if (Strings.isNullOrEmpty(userEntity.getPassword())) {
//            throw new CustomException(ResultEnum.PARAM_ERROR);
//        }
//
//        // 登录
//        log.info("UserController register start.mobile={},password={}", mobile, password);
//        Long userId = userService.register(userEntity);
//        userEntity.setId(userId);
//        log.info("UserController register success.mobile={},password={}", mobile, password);
//        return Result.success(userEntity);
//    }
//
//    @RequireLogin(false)
//    @PostMapping("/api/user/login")
//    public Result<User> login(@RequestBody UserEntity userEntity) {
//        // 验证参数，手机号，密码必传
//        String mobile = userEntity.getMobile();
//        String password = userEntity.getPassword();
//        if (Strings.isNullOrEmpty(mobile)) {
//            throw new CustomException(ResultEnum.PARAM_ERROR);
//        }
//        if (Strings.isNullOrEmpty(userEntity.getPassword())) {
//            throw new CustomException(ResultEnum.PARAM_ERROR);
//        }
//
//        // 登录
//        log.info("UserController login start.mobile={},password={}", mobile, password);
//        User user = userService.login(userEntity);
//        log.info("UserController login success.mobile={},password={}", mobile, password);
//        return Result.success(user);
//    }
//
//    @PostMapping("/api/user/logout")
//    public Result<String> logout() {
//        // 退出
//        UserEntity userEntity = new UserEntity();
//        Long userId = UserContext.getUserId();
//        userEntity.setId(userId);
//        log.info("UserController logout start.userId={}", userId);
//        userService.logout(userEntity);
//        log.info("UserController logout success.userId={}", userId);
//        return Result.success();
//    }
//
//    /**
//     * 注销接口
//     * @param param 参数
//     * @return 结果
//     */
//    @PostMapping("/api/user/delete")
//    public Result<Page<UserEntity>> delete(@RequestBody DeleteUserParam param) {
//        Long userId = param.getUserId();
//        userService.delete(userId);
//        return Result.success();
//    }
//
//    /**
//     * 修改个人信息
//     * @param param 参数
//     * @return 结果
//     */
//    @PostMapping("/api/user/update")
//    public Result<Page<UserEntity>> update(@RequestBody UpdateUserParam param) {
//        UserEntity userEntity = new UserEntity();
//        BeanUtils.copyProperties(param, userEntity);
//        userService.update(userEntity);
//        return Result.success();
//    }
//
//    /**
//     * 查询用户个人信息
//     * @param param 参数
//     * @return 结果
//     */
//    @Operation(summary = "查询用户接口", description = "查询用户接口")
//    @PostMapping("/api/user/get")
//    public Result<UserEntity> get(@RequestBody GetUserParam param) {
//        Long userId = param.getUserId();
//        log.info("UserController getUser.userId={}", userId);
//        UserEntity userEntity = userService.getUser(userId);
//        return Result.success(userEntity);
//    }
//
//}

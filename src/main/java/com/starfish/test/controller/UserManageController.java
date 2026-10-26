package com.starfish.test.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.starfish.core.annotation.RequireLogin;
import com.starfish.core.model.Result;
import com.starfish.test.entity.UserEntity;
import com.starfish.test.param.DeleteUserParam;
import com.starfish.test.param.ListUserParam;
import com.starfish.test.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * UserAdminController
 *
 * @author sunkolin
 * @version 1.0.0
 * @since 2026-03-17
 */
@Slf4j
@Tag(name = "UserAdminController", description = "用户管理接口")
@RestController
public class UserManageController {

    @Resource
    private UserService userService;

    /**
     * 获取用户列表
     *
     * @param param 参数
     * @return 用户列表
     */
    @RequireLogin(false)
    @PostMapping("/api/user/list")
    public Result<Page<UserEntity>> list(@RequestBody ListUserParam param) {
        Long pageNumber = param.getPageNumber();
        Long pageSize = param.getPageSize();
        Page<UserEntity> list = userService.list(pageNumber, pageSize);
        return Result.success(list);
    }

    /**
     * 注销接口
     * @param param 参数
     * @return 结果
     */
    @PostMapping("/api/user/delete")
    public Result<Page<UserEntity>> delete(@RequestBody DeleteUserParam param) {
        Long userId = param.getId();
        userService.delete(userId);
        return Result.success();
    }

}

package com.starfish.test.interceptor;

import com.starfish.core.annotation.RequireLogin;
import com.starfish.core.context.UserContext;
import com.starfish.core.exception.CustomException;
import com.starfish.experiment.jwt.JsonWebTokens;
import com.starfish.test.context.User;
import com.starfish.test.enumeration.ResultEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * LoginInterceptor
 *
 * @author sunkolin
 * @version 1.0.0
 * @since 2024-05-10
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * Token有效期，单位秒
     */
    public static final Long TOKEN_EXPIRE = 30 * 60L;

    /**
     * Token有效期，单位毫秒
     */
    public static final Long TOKEN_EXPIRE_MILLISECOND = TOKEN_EXPIRE * 1000L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前链接是否需要验证token，如果不需要验证token直接返回
        //  只要没有Login注解就不需要验证token
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RequireLogin requireLogin = method.getAnnotation(RequireLogin.class);
        if (requireLogin != null && !requireLogin.value()) {
            return true;
        }

        // 验证Token有效性
        String token = request.getParameter("token");
        String userIdString = request.getParameter("userId");
        Long userId = Long.valueOf(userIdString);
        User user = JsonWebTokens.verify(userId, token, User.class);
        if (user == null) {
            throw new CustomException(ResultEnum.INVALID_TOKEN);
        }

        // 验证登录有效期
        Date lastLoginTime = user.getLastLoginTime();
        Date now = new Date();
        if (now.getTime() - lastLoginTime.getTime() > TOKEN_EXPIRE_MILLISECOND) {
            throw new CustomException(ResultEnum.LOGIN_EXPIRE);
        }

        // 设置用户数据到上下文
        UserContext.setUser(user);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

}

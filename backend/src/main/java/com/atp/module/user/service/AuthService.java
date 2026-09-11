package com.atp.module.user.service;

import com.atp.module.user.dto.LoginRequest;
import com.atp.module.user.dto.RegisterRequest;
import com.atp.module.user.vo.CaptchaVO;
import com.atp.module.user.vo.LoginVO;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录，返回令牌
     */
    LoginVO login(LoginRequest request, String ip);

    /**
     * 登出，令牌加入黑名单
     */
    void logout(String token);

    /**
     * 生成图形验证码
     */
    CaptchaVO generateCaptcha();
}

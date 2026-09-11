package com.atp.module.user.controller;

import com.atp.common.result.Result;
import com.atp.common.util.IpUtils;
import com.atp.module.user.dto.LoginRequest;
import com.atp.module.user.dto.RegisterRequest;
import com.atp.module.user.service.AuthService;
import com.atp.module.user.vo.CaptchaVO;
import com.atp.module.user.vo.LoginVO;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、登出、验证码
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok("注册成功，请登录");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, IpUtils.getClientIp(httpRequest)));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        String token = resolveToken(httpRequest);
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token);
        }
        return Result.ok("已退出登录");
    }

    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(authService.generateCaptcha());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}

package com.atp.module.user.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.user.dto.LoginRequest;
import com.atp.module.user.dto.RegisterRequest;
import com.atp.module.user.entity.User;
import com.atp.module.user.service.AuthService;
import com.atp.module.user.service.UserService;
import com.atp.module.user.vo.CaptchaVO;
import com.atp.module.user.vo.LoginVO;
import com.atp.module.user.vo.UserInfoVO;
import com.atp.security.JwtAuthenticationFilter;
import com.atp.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Font;
import java.time.Duration;
import java.util.Objects;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    @Value("${atp.captcha.enabled:true}")
    private boolean captchaEnabled;
    private static final String CAPTCHA_KEY_PREFIX = "atp:captcha:";
    private static final long CAPTCHA_EXPIRE_SECONDS = 300L;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new BizException(ResultCode.PASSWORD_NOT_MATCH);
        }
        if (userService.getByUsername(request.getUsername()) != null) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
        long emailCount = userService.lambdaQuery().eq(User::getEmail, request.getEmail()).count();
        if (emailCount > 0) {
            throw new BizException(ResultCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(StrUtil.blankToDefault(request.getNickname(), request.getUsername()));
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setRole("TESTER");
        userService.save(user);

        consumeCaptcha(request.getCaptchaKey());
        log.info("用户注册成功 username={}", request.getUsername());
    }

    @Override
    public LoginVO login(LoginRequest request, String ip) {
        verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        User user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        userService.updateLoginInfo(user.getId(), ip);
        consumeCaptcha(request.getCaptchaKey());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpiration())
                .userInfo(toUserInfoVO(user))
                .build();
    }

    @Override
    public void logout(String token) {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            jwtAuthenticationFilter.blacklist(token, jwtTokenProvider.getRemainingMillis(claims));
        } catch (Exception e) {
            log.debug("登出时令牌解析失败，忽略：{}", e.getMessage());
        }
    }

    @Override
    public CaptchaVO generateCaptcha() {
        if (!captchaEnabled) {
            return CaptchaVO.builder().captchaKey("").imageBase64("").expireSeconds(0L).build();
        }
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 25);
        captcha.setFont(new Font("Arial", Font.PLAIN, 26));

        String key = IdUtil.fastSimpleUUID();
        try {
            stringRedisTemplate.opsForValue().set(
                    CAPTCHA_KEY_PREFIX + key, captcha.getCode(), Duration.ofSeconds(CAPTCHA_EXPIRE_SECONDS));
        } catch (Exception e) {
            log.warn("Redis 不可用，验证码无法持久化：{}", e.getMessage());
        }

        return CaptchaVO.builder()
                .captchaKey(key)
                .imageBase64("data:image/png;base64," + captcha.getImageBase64())
                .expireSeconds(CAPTCHA_EXPIRE_SECONDS)
                .build();
    }

    /**
     * 校验验证码。开关关闭时跳过校验；开启时未传 key/code 直接报错；Redis 不可用时降级放行。
     */
    private void verifyCaptcha(String key, String code) {
        if (!captchaEnabled) {
            return;
        }
        if (!StrUtil.hasLetter(key) || !StrUtil.hasLetter(code)) {
            throw new BizException(ResultCode.CAPTCHA_ERROR, "请输入验证码");
        }
        String cached;
        try {
            cached = stringRedisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过验证码校验：{}", e.getMessage());
            return;
        }
        if (cached == null) {
            throw new BizException(ResultCode.CAPTCHA_ERROR, "验证码已过期，请点击图片刷新");
        }
        if (!cached.equalsIgnoreCase(code.trim())) {
            throw new BizException(ResultCode.CAPTCHA_ERROR, "验证码错误");
        }
    }

    private void consumeCaptcha(String key) {
        if (!StrUtil.hasLetter(key)) {
            return;
        }
        try {
            stringRedisTemplate.delete(CAPTCHA_KEY_PREFIX + key);
        } catch (Exception e) {
            log.debug("删除验证码失败，忽略：{}", e.getMessage());
        }
    }

    /**
     * 实体转 VO，杜绝密码等敏感字段外泄
     */
    public static UserInfoVO toUserInfoVO(User user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .createTime(user.getCreateTime())
                .build();
    }
}

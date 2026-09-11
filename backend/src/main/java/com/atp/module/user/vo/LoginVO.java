package com.atp.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /**
     * JWT 令牌
     */
    private String token;

    /**
     * 令牌类型，固定 Bearer
     */
    private String tokenType;

    /**
     * 有效期（毫秒）
     */
    private Long expiresIn;

    private UserInfoVO userInfo;
}

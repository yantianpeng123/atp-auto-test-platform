package com.atp.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    /**
     * 验证码标识，提交时回传
     */
    private String captchaKey;

    /**
     * 图片 Base64（含 data URI 前缀，可直接用于 img src）
     */
    private String imageBase64;

    /**
     * 有效期（秒）
     */
    private Long expireSeconds;
}

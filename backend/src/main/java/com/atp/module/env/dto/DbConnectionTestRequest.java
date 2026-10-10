package com.atp.module.env.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 数据库连通性测试入参
 */
@Data
public class DbConnectionTestRequest {

    @NotBlank(message = "请填写主机地址")
    private String host;

    @NotNull(message = "请填写端口")
    private Integer port;

    @NotBlank(message = "请填写库名")
    private String dbName;

    @NotBlank(message = "请填写用户名")
    private String username;

    @NotBlank(message = "请填写密码")
    private String password;
}

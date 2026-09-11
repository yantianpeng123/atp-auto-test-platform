package com.atp.module.env.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试环境出参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvVO {

    private Long id;

    private Long projectId;

    private String name;

    private String baseUrl;

    /** 全局请求头（JSON 字符串），前端按需解析展示 */
    private String headers;

    /** 数据库配置（JSON 字符串） */
    private String dbConfig;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

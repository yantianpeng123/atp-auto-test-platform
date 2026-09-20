package com.atp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 自动化测试平台 - 后端启动类
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.atp.module.**.mapper")
@SpringBootApplication
public class AtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtpApplication.class, args);
    }
}

package com.atp.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 配置：统一 LocalDateTime 序列化/反序列化格式为 yyyy-MM-dd HH:mm:ss
 *
 * <p>说明：application.yml 里的 spring.jackson.date-format 只对 java.util.Date 生效，
 * 对 LocalDateTime 无效（默认按 ISO-8601 输出 2026-09-04T15:21:25），故在此统一处理。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(DATETIME_FORMATTER));
            builder.deserializers(new LocalDateTimeDeserializer(DATETIME_FORMATTER));
        };
    }
}

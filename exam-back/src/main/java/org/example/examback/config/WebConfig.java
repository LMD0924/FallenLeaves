package org.example.examback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    // 配置全局的 ObjectMapper
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        // 或者手动配置
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 配置 ISO 格式的 LocalDateTime 序列化和反序列化
        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(isoFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(isoFormatter));

        objectMapper.registerModule(javaTimeModule);

        return objectMapper;
    }

    // 可选：添加全局的转换器
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 添加 String 到 LocalDateTime 的转换器
        registry.addConverter(String.class, LocalDateTime.class, source -> {
            if (source == null || source.isEmpty()) {
                return null;
            }
            try {
                // 尝试解析 ISO 格式
                return LocalDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format: " + source);
            }
        });
    }
}
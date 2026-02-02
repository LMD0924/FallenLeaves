package org.example.examback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
 * @Author:总会落叶
 * @Date:2026/2/2
 * @Description:
 */
@Configuration
public class RedisConfig {

    @Bean //将RedisTemplate注入到Spring容器中
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        //创建RedisTemplate对象
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        //设置连接工厂
        template.setConnectionFactory(factory);

        //使用String序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // 1. 设置 Key 的序列化器（String 类型）
        // 作用：确保 Redis 中存储的键是纯文本，没有乱码
        template.setKeySerializer(stringSerializer);
        // 2. 设置 Hash 数据结构的 Key 序列化器
        // 作用：确保 Hash 类型的键也是纯文本
        template.setHashKeySerializer(stringSerializer);
        // 3. 设置 Value 的序列化器（String 类型）
        // 作用：确保 Redis 中存储的值是纯文本
        // ⚠️ 注意：这要求所有存储的值都必须是字符串或可转换为字符串的类型
        template.setValueSerializer(stringSerializer);
        // 4. 设置 Hash 数据结构的 Value 序列化器
        // 作用：确保 Hash 类型的值也是纯文本
        template.setHashValueSerializer(stringSerializer);
        // 初始化模板属性（必需步骤）
        template.afterPropertiesSet();
        return template;
    }
}

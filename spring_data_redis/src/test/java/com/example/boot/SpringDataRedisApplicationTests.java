package com.example.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class SpringDataRedisApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void m1() {

        redisTemplate.opsForValue().set("name","HanMeiMei");

        Object name = redisTemplate.opsForValue().get("name");

        System.out.println("name = " + name);

    }

}

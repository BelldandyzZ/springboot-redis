package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


@Component
public class RedisIdWorker {

    //开始时间
    private static final Long BEGIN_TIME = 1672531200L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 全局唯一id：时间戳 + 计数     在时间戳相同的情况下根据不同的计数来区分不同的id
     * @param businessName：根据不同的业务作为key的一部分生成id中计数的那个部分
     */
    public long nextId(String businessName){
        //获取当前时间
        LocalDateTime now = LocalDateTime.now();
        long time = now.toEpochSecond(ZoneOffset.UTC);
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //获取开始时间到当前时间的时间差
        long timestamp = time - BEGIN_TIME;
        //利用redis的自增获取id中计数的那一部分
        long count = stringRedisTemplate.opsForValue().increment("increment:" + businessName + date);
        //把时间戳与计数的把一个部分拼接然后返回
        return timestamp << 32 | count;
    }

}

package com.hmdp.utils.lock;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String prefix;
    private String key;

    public SimpleRedisLock() {
    }

    public SimpleRedisLock(String prefix, String key,StringRedisTemplate stringRedisTemplate) {
        this.prefix = prefix;
        this.key = key;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSes) {
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(prefix + key, Thread.currentThread().getName(), timeoutSes, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(lock);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.delete(prefix + key);

    }
}

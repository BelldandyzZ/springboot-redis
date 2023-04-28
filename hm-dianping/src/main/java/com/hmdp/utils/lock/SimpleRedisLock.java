package com.hmdp.utils.lock;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String prefix;
    private String key;

    //UUID主要是用于区分在分布式模式下不同的机器，相当于服务器ID
    private static final String ID_PREFIX = UUID.randomUUID(true).toString() + "-";

    public SimpleRedisLock() {
    }

    public SimpleRedisLock(String prefix, String key,StringRedisTemplate stringRedisTemplate) {
        this.prefix = prefix;
        this.key = key;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSes) {
        String threadId = ID_PREFIX + Thread.currentThread().getId() + "";
        //不同的jvm中有可能出现相同的threadId，使用UUID区分jvm
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(prefix + key, threadId, timeoutSes, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(lock);
    }

    @Override
    public void unlock() {
        String curThreadId = ID_PREFIX + Thread.currentThread().getId() + "";
        String cacheThreadId = stringRedisTemplate.opsForValue().get(prefix + key);
        //当jvm相同，也就是uuid相同时，并且是同一个线程，说明锁属于自己才有权力释放
        if(curThreadId.equals(cacheThreadId)){
            stringRedisTemplate.delete(prefix + key);
        }
    }
}

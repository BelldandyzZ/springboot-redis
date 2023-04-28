package com.hmdp.utils.lock;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
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

        //当jvm相同，也就是uuid相同时，并且是同一个线程，说明锁属于自己才有权力释放
        //判断操作与删除操作应该保持原子性，使用Lua脚本可以使redis命令具有原子性
//        String cacheThreadId = stringRedisTemplate.opsForValue().get(prefix + key);
//        if(curThreadId.equals(cacheThreadId)){
//            stringRedisTemplate.delete(prefix + key);
//        }
        //使用Lua脚本来保证判断和释放锁的原子性
        stringRedisTemplate.execute(
            UNLOCK_SCRIPT,
            Collections.singletonList(prefix + key),
            curThreadId
        );

    }
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }




}

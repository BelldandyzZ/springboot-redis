package com.example.boot.jedisTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import static com.example.boot.jedisTest.JedisConnectFactory.jedisPool;

public class JedisTest {

    private Jedis jedis;


    @BeforeEach
    public void initJedis(){
         jedis = jedisPool.getResource();
    }

    @AfterEach
    public void destroy(){
        if (jedis != null) {
            jedis.close();
        }
    }

    @Test
    public void execution(){
        String name = jedis.get("name");
        System.out.println("name = " + name);
    }



}

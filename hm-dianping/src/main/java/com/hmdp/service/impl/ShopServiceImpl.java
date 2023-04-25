package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryById(Long id) {
        /*
        * 使用逻辑过期完成缓存击穿问题，逻辑过期就是在redis中永不过期，自己在缓存的数据中设置一个过期时间
        * 这个缓存一般都会做预热处理，就是在程序运行之前把缓存先导入进去
        */

        //1.从redis查询缓存
        String cache = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //2.缓存不存在直接返回null,因为是逻辑过期，这里永不成立
        if(StrUtil.isBlank(cache)){
            return  null;
        }
        //3.缓存存则在判断是否过期
        RedisData redisData = JSONUtil.toBean(cache, RedisData.class);
        //4.未过期则直接返回数据
        LocalDateTime expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return Result.ok(redisData.getData());
        }
        //6.过期了则重建缓存,获取重建缓存的锁
        boolean flag = tryLock(LOCK_SHOP_KEY);
        if(flag){
            System.out.println(Thread.currentThread().getName() + "拿到了锁");
            //7.获取锁成功直接返回过期的数据，然后开一个线程重建缓存
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    buildCache(10L,id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    //9.重建完成释放锁
                    System.out.println(Thread.currentThread().getName() +"线程释放了锁");
                    unLock(LOCK_SHOP_KEY);
                }
            });
        }
        //10.获取失败则说明已经有其他线程正在重建缓存，直接返回过期的旧数据
        return Result.ok(redisData.getData());
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        System.out.println(Thread.currentThread().getName() +"线程的锁为:" + flag);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    public void buildCache(Long expireSecond,Long id){
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSecond));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));
    }


    @Override
    @Transactional
    public Result update(Shop shop) {
        if(shop.getId() == null){
            Result.fail("店铺ID不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //1.判断缓存是否存在，存在则直接返回，不存在则开始建立缓存
        Result checkCache = checkCache(CACHE_SHOP_KEY,id);
        if (checkCache != null) return checkCache;

        Shop shop = null;
        try {
            //2.使用互斥锁建立缓存防止缓存击穿
            boolean flag = tryLock(LOCK_SHOP_KEY);
            if(!flag){
                //3.获取互斥锁失败说明缓存正在重建中，则等待之后重新查询
                Thread.sleep(50);
                return queryById(id);
            }
            //4.获取锁成功再次验证缓存是否存在，不存在则开始建立缓存
            Result doubleCheck = checkCache(CACHE_SHOP_KEY,id);
            if (doubleCheck != null) return doubleCheck;
            //5.从数据库查询数据
            shop = getById(id);
            if(shop == null){
                //6.数据为空说明该查询key和查询的数据在缓存和数据库中都不存在，此时缓存""防止缓存穿透,然后返回错误信息
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return Result.fail("店铺不存在");
            }
            //7.数据从数据库中存在就写入缓存
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),
                    CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            //8.释放互斥锁
            unLock(LOCK_SHOP_KEY);
        }
        //8.返回查询信息
        return Result.ok(shop);
    }

    private Result checkCache(String key,Long id) {
        //1.从redis查询缓存
        String cacheShop = stringRedisTemplate.opsForValue().get(key + id);
        //2.判断缓存是否存在
        if(StrUtil.isNotBlank(cacheShop)){
            //3.缓存存在直接返回
            Shop res = JSONUtil.toBean(cacheShop, Shop.class);
            return  Result.ok(res);
        }
        //4. 判断命中的是否是空值
        if("".equals(cacheShop)){
            return  Result.fail("店铺不存在");
        }
        //5.不存在也不是空值则返回空，返回主方法开始建立缓存
        return null;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 2, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
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

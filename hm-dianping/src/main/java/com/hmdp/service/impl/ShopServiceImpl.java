package com.hmdp.service.impl;

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
        //1.从redis查询缓存
        String cacheShop = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if(StrUtil.isNotBlank(cacheShop)){
            //3.存在直接返回
            Shop shop = JSONUtil.toBean(cacheShop, Shop.class);
            return  Result.ok(shop);
        }

        //判断命中的是否是空值
        if("".equals(cacheShop)){
            return  Result.fail("店铺不存在");
        }

        //4.不存在根据id查询数据库
        Shop shop = getById(id);

        if(shop == null){
            //5.在数据库中不存在则缓存""防止缓存穿透,然年返回错误信息
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        //6.在数据库中存在则写入数据库
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),
                                                CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7.返回
        return Result.ok(shop);
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

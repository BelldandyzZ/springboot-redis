package com.hmdp.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {

        //1. 查询redis缓存数据
        List list = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE, 0, -1);

        if(list != null && list.size() > 0){
            //2. 存在直接返回
            List<ShopType> shopTypes = JSONUtil.toList(JSONUtil.parseArray(list.get(0)), ShopType.class);
            return Result.ok(shopTypes);
        }
        //3. 不存在就去数据库查询
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //4. 数据库中不存在则报错
        if(typeList == null || typeList.size() <= 0){
            return Result.fail("没有店铺分类");
        }
        //5. 数据库中存在则放入redis
        stringRedisTemplate.opsForList().leftPushAll(CACHE_SHOP_TYPE, JSONUtil.toJsonStr(typeList));
        //6. 返回list数据
        return Result.ok(typeList);
    }
}

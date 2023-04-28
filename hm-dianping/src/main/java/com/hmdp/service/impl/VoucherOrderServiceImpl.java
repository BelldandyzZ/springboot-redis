package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.lock.SimpleRedisLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private IVoucherOrderService iVoucherOrderService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠卷信息
        SeckillVoucher seckill = iSeckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始或过期
        //2.1判断是否开始
        if(seckill.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀未开始");
        }
        //2.2判断是否结束
        if(seckill.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已结束");
        }
        //3.未过期判断系统库存是否充足
        if (seckill.getStock() < 1) {
            return Result.fail("秒杀券库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        RLock lock = redissonClient.getLock(VOUCHER_ORDER_LOCK_PREFIX_KEY + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            return Result.fail("不允许并发下单！该优惠券每人限购一张");
        }
        try {
            return iVoucherOrderService.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }


    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count > 0){
           return  Result.fail("该优惠券每人限购一张");
        }
        //4.未购买过并且库存充足则扣减库存，并创建订单
        //4.1 扣减库存
        boolean success = iSeckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock",0)//cas法加上乐观锁
                .update();
        if (!success) {
            return Result.fail("库存扣减失败");
        }
        //4.2 创建订单
        long orderId = redisIdWorker.nextId(BUSINESS_NAME);
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        save(order);
        //7.返回订单Id
        return Result.ok(orderId);
    }
}

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        //1. 验证手机格式是否正确
        if(RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机格式不正确");
        //2. 从redis取出验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        //3. 对验证码进行验证
        if(code == null || !code.equals(cacheCode)) return  Result.fail("验证码错误");
        //4. 根据手机号查询用户
        User user = query().eq("phone", phone).one();
        //5. 如果该手机号没有用户则注册一个
        if(user == null) user = createUserByPhone(phone);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        //6. 将user以Hash结构存到redis
        String token = UUID.randomUUID(true).toString();
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),CopyOptions.create()
                                        .setIgnoreNullValue(true)
                                        .setFieldValueEditor((fileName,fileValue) -> fileValue.toString()));
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);

        //7. 设置token有效期为30分钟，（使用一次该token就属性有效期在拦截器中有设置）
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);

        //把token返回前端，前端每次请求都需要带着token
        return Result.ok(token);
    }

    private User createUserByPhone(String phone) {
        User user  = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        save(user);
        return user;
    }

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 验证手机格式是否正确
        if(RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机格式不正确");

        //2. 随机生成验证码
        String code = RandomUtil.randomNumbers(6);

        //把验证码存到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("发送验证码成功：" + code);
        return Result.ok();
    }
}

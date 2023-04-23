package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    /**
     * 该拦截器只负责属性token，不负责拦截，拦截由LoginInterceptor负责
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 从请求头获取token
        String token = request.getHeader("authorization");

        //2. token为空代表没有登录
        if (StringUtils.isEmpty(token)) return true;

        //3. 根据token从redis获取用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        //4. 把user放入ThreadLocal中，因为controller可能会用到用户相关的信息
        UserHolder.saveUser(userDTO);

        //5. 刷新token的有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //1. 当前请求结束把user从线程中移除，因为下次请求是全新的线程，不移除会造成内存泄露
        UserHolder.removeUser();
    }


    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public RefreshTokenInterceptor() {
    }
}

package com.tanhua.sso.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tanhua.sso.mapper.UserMapper;
import com.tanhua.sso.pojo.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import sun.rmi.runtime.Log;


import javax.management.QueryEval;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private UserMapper userMapper;
    @Value("${jwt.secret}")
    private String secret;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 用户登录
     *
     * @param phone
     * @param code
     * @return
     */
    public String login(String phone, String code) {
        String redisKey = "CHECK_CODE_" + phone;

        boolean isNew = false;

        //校验验证码
        String redisData = this.redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.equals(code, redisData)) {
            return null;
        }
        //验证码在校验完成后得删除
        this.redisTemplate.delete(redisKey);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mobile", phone);
        User user = this.userMapper.selectOne(queryWrapper);


        if (null == user) {
            user = new User();
            user.setMobile(phone);
            user.setPassword(DigestUtils.md5Hex("123456"));

            //注册新用户
            this.userMapper.insert(user);
            isNew = true;
        }

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("id", user.getId());
        // 生成token
        String token = Jwts.builder()
                .setClaims(claims) //payload，存放数据的位置，不能放置敏感数据，如：密码等
                .signWith(SignatureAlgorithm.HS256, secret) //设置加密方法和加密盐
                .setExpiration(new DateTime().plusHours(12).toDate()) //设置过期时间，12小时后过期
                .compact();

        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("id", user.getId());
            msg.put("date", System.currentTimeMillis());
            this.rocketMQTemplate.convertAndSend("tanhua-sso-login", msg);
        } catch (MessagingException e) {
            log.error("发送消息失败!", e);
        }

        return token + "|" + isNew;
    }

    public User queryUserByToken(String token) {
        try {
            // 通过token解析数据
            Map<String, Object> body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            User user = new User();
            user.setId(Long.valueOf(body.get("id").toString()));
            String redisKey = "TANHUA_USER_MOBILE_" + user.getId();
            if (this.redisTemplate.hasKey(redisKey)) {
                String mobile = this.redisTemplate.opsForValue().get(redisKey);
                user.setMobile(mobile);
            } else {
                User u = this.userMapper.selectById(user.getId());
                user.setMobile(u.getMobile());
                Long timeout = Long.valueOf(body.get("exp").toString()) * 1000 - System.currentTimeMillis();
                this.redisTemplate.opsForValue().set(redisKey, u.getMobile(), timeout, TimeUnit.MILLISECONDS);
            }
            return user;
        } catch (ExpiredJwtException e) {
            log.info("token已过期!token=" + token, e);
        } catch (Exception e) {
            log.error("token不合法！token=" + token, e);
        }
        return null;
    }
}

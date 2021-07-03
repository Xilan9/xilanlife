package com.tanhua.dubbo.server.service;


import com.tanhua.dubbo.server.enums.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 借助redis的自增长特性，实现全局的id唯一自增长
     * @return
     */
    public Long createId(IdType idType){
        String key="TANHUA_ID_"+idType.toString();
        return this.redisTemplate.opsForValue().increment(key);
    }


}

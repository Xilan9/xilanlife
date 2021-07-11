package com.tanhua.dubbo.server.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tanhua.dubbo.server.config.HuanXinConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TokenService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_KEY = "HX_TOKEN";

    @Autowired
    private HuanXinConfig huanXinConfig;

    /**
     * 获取token，先从redis中获取，如果没有，再去环信接口获取
     *
     * @return
     */
    public String getToken() {
        String token = this.redisTemplate.opsForValue().get(REDIS_KEY);
        if (StrUtil.isNotEmpty(token)) {
            return token;
        }

        //访问环信接口获取token
        return this.refreshToken();
    }

    /**
     * 刷新token，请求环信接口，将token存储到redis中
     *
     * @return
     */
    public String refreshToken() {
        String targetUrl = this.huanXinConfig.getUrl() +
                this.huanXinConfig.getOrgName() + "/" +
                this.huanXinConfig.getAppName() + "/token";

        Map<String, Object> param = new HashMap<>();
        param.put("grant_type", "client_credentials");
        param.put("client_id", this.huanXinConfig.getClientId());
        param.put("client_secret", this.huanXinConfig.getClientSecret());

        HttpResponse response = HttpRequest.post(targetUrl)
                .body(JSONUtil.toJsonStr(param))
                .timeout(20000) //请求超时时间
                .execute();

        if (!response.isOk()) {
            log.error("刷新token失败");
            return null;
        }

        String jsonBody = response.body();
        JSONObject jsonObject = JSONUtil.parseObj(jsonBody);
        String token = jsonObject.getStr("access_token");
        if (StrUtil.isNotEmpty(token)) {
            //将token数据缓存到redis中，缓存时间由expires_in决定
            //提前1小时失效
            long timeout = jsonObject.getLong("expires_in") - 3600;
            this.redisTemplate.opsForValue().set(REDIS_KEY, token, timeout, TimeUnit.SECONDS);

            return token;
        }

        return null;
    }
}


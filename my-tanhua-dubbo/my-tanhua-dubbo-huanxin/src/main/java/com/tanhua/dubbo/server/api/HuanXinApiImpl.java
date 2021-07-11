package com.tanhua.dubbo.server.api;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.service.RequestService;
import com.tanhua.dubbo.server.service.TokenService;
import com.tanhua.dubbo.server.config.HuanXinConfig;
import com.tanhua.dubbo.server.mapper.HuanXinUserMapper;
import com.tanhua.dubbo.server.pojo.HuanXinUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;

@Service(version = "1.0.0")
@Slf4j
public class HuanXinApiImpl implements HuanXinApi {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private HuanXinConfig huanXinConfig;

    @Autowired
    private RequestService requestService;

    @Autowired
    private HuanXinUserMapper huanXinUserMapper;


    @Override
    public String getToken() {
        return this.tokenService.getToken();
    }

    @Override
    public Boolean register(Long userId) {
        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/" +
                this.huanXinConfig.getAppName() + "/users";
        HuanXinUser huanXinUser = new HuanXinUser();
        huanXinUser.setUsername("HX_" + userId);  // 用户名
        huanXinUser.setPassword(IdUtil.simpleUUID()); //随机生成的密码
        HttpResponse response = this.requestService.execute(targetUrl, JSONUtil.toJsonStr(Arrays.asList(huanXinUser)), Method.POST);
        if (response.isOk()) {
            //将环信的账号信息保存到数据库
            huanXinUser.setUserId(userId);
            huanXinUser.setCreated(new Date());
            huanXinUser.setUpdated(huanXinUser.getCreated());
            this.huanXinUserMapper.insert(huanXinUser);
            return true;
        }
        return false;
    }

    @Override
    public HuanXinUser queryHuanXinUser(Long userId) {
        QueryWrapper<HuanXinUser> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return this.huanXinUserMapper.selectOne(wrapper);
    }

    @Override
    public HuanXinUser queryUserByUserName(String userName) {
        QueryWrapper<HuanXinUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", userName);
        return this.huanXinUserMapper.selectOne(wrapper);
    }



    @Override
    public Boolean addUserFriend(Long userId, Long friendId) {
        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users/HX_" +
                userId + "/contacts/users/HX_" + friendId;
        try {
            // 404 -> 对方未在环信注册
            return this.requestService.execute(targetUrl, null, Method.POST).isOk();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 添加失败
        return false;
    }

    @Override
    public Boolean removeUserFriend(Long userId, Long friendId) {
        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users/HX_" +
                userId + "/contacts/users/HX_" + friendId;
        try {
            // 404 -> 对方未在环信注册
            return this.requestService.execute(targetUrl, null, Method.DELETE).isOk();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 添加失败
        return false;
    }
}
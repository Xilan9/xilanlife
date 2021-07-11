package com.tanhua.server.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.common.pojo.User;
import com.tanhua.common.pojo.UserInfo;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.dubbo.server.api.HuanXinApi;
import com.tanhua.dubbo.server.api.UsersApi;
import com.tanhua.dubbo.server.pojo.HuanXinUser;
import com.tanhua.dubbo.server.pojo.Users;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.UserInfoVo;
import com.tanhua.server.vo.UsersVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IMService {

    @Reference(version = "1.0.0")
    private HuanXinApi huanXinApi;

    @Reference(version = "1.0.0")
    private UsersApi usersApi;

    @Autowired
    private UserInfoService userInfoService;

    public UserInfoVo queryUserInfoByUserName(String userName) {
        //查询环信账户
        HuanXinUser huanXinUser = this.huanXinApi.queryUserByUserName(userName);
        if (ObjectUtil.isEmpty(huanXinUser)) {
            return null;
        }

        //查询用户信息
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(huanXinUser.getUserId());
        if (ObjectUtil.isEmpty(userInfo)) {
            return null;
        }

        UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class, "marriage");
        userInfoVo.setGender(userInfo.getSex().toString().toLowerCase());
        userInfoVo.setMarriage(StrUtil.equals("已婚", userInfo.getMarriage()) ? 1 : 0);

        return userInfoVo;
    }

    /**
     * 添加好友
     *
     * @param friendId 好友id
     */
    public boolean contactUser(Long friendId) {
        User user = UserThreadLocal.get();

        String id = this.usersApi.saveUsers(user.getId(), friendId);

        if (StrUtil.isNotEmpty(id)) {
            //注册好友关系到环信
            return this.huanXinApi.addUserFriend(user.getId(), friendId);
        }

        return false;
    }

    public PageResult queryContactsList(Integer page, Integer pageSize, String keyword) {
        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);

        User user = UserThreadLocal.get();

        List<Users> usersList;
        if (StringUtils.isNotEmpty(keyword)) {
            //关键不为空，查询所有的好友，在后面进行关键字过滤
            usersList = this.usersApi.queryAllUsersList(user.getId());
        } else {
            //关键字为空，进行分页查询
            PageInfo<Users> usersPageInfo = this.usersApi.queryUsersList(user.getId(), page, pageSize);
            usersList = usersPageInfo.getRecords();
        }

        if (CollUtil.isEmpty(usersList)) {
            return pageResult;
        }


        List<Object> userIds = CollUtil.getFieldValues(usersList, "friendId");

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        if (StringUtils.isNotEmpty(keyword)) {
            queryWrapper.like("nick_name", keyword);
        }

        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<UsersVo> contactsList = new ArrayList<>();
        //填充用户信息
        for (UserInfo userInfo : userInfoList) {
            UsersVo usersVo = new UsersVo();
            usersVo.setId(userInfo.getUserId());
            usersVo.setAge(userInfo.getAge());
            usersVo.setAvatar(userInfo.getLogo());
            usersVo.setGender(userInfo.getSex().name().toLowerCase());
            usersVo.setNickname(userInfo.getNickName());
            //环信用户账号
            usersVo.setUserId("HX_" + String.valueOf(userInfo.getUserId()));
            usersVo.setCity(StringUtils.substringBefore(userInfo.getCity(), "-"));
            contactsList.add(usersVo);
        }

        pageResult.setItems(contactsList);
        return pageResult;
    }
}

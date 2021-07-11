package com.tanhua.server.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.common.pojo.User;
import com.tanhua.common.utils.UserThreadLocal;
import com.tanhua.dubbo.server.api.HuanXinApi;
import com.tanhua.dubbo.server.pojo.HuanXinUser;
import com.tanhua.server.vo.HuanXinUserVo;
import org.springframework.stereotype.Service;

@Service
public class HuanXinService {

    @Reference(version = "1.0.0")
    private HuanXinApi huanXinApi;

    public HuanXinUserVo queryHuanXinUser() {
        User user = UserThreadLocal.get();
        //通过dubbo服务查询环信用户
        HuanXinUser huanXinUser = this.huanXinApi.queryHuanXinUser(user.getId());
        if (ObjectUtil.isNotEmpty(huanXinUser)) {
            return new HuanXinUserVo(huanXinUser.getUsername(), huanXinUser.getPassword());
        }
        return null;
    }
}
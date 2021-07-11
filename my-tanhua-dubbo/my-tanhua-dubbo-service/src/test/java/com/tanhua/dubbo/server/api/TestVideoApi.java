package com.tanhua.dubbo.server.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestVideoApi {

    @Autowired
    private VideoApi videoApi;

    @Test
    public void testQueryVideoList() {
        //返回的推荐结果数据
        System.out.println(this.videoApi.queryVideoList(1L, 1, 8));
        //返回少于pageSize数据，因为推荐数据不够了
        System.out.println(this.videoApi.queryVideoList(1L, 3, 8));

        //返回系统数据
        System.out.println(this.videoApi.queryVideoList(1L, 4, 8));

    }

}
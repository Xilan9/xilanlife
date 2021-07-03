package com.tanhua.dubbo.server.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class testQuanZiApi {

    @Autowired
    private QuanZiApi quanZiApi;

    @Test
    public void testQueryPublishList(){
        this.quanZiApi.queryPublishList(1L, 1, 2).
                getRecords().forEach(publish -> System.out.println(publish));
    }

    @Test
    public void testLike(){
        Long userId = 1L;
        String publishId = "5fae53947e52992e78a3afb1";
        Boolean data = this.quanZiApi.queryUserIsLike(userId, publishId);
        System.out.println(data);

        System.out.println(this.quanZiApi.likeComment(userId, publishId));

        System.out.println(this.quanZiApi.queryLikeCount(publishId));

        System.out.println(this.quanZiApi.disLikeComment(userId, publishId));

        System.out.println(this.quanZiApi.queryLikeCount(publishId));
    }
}

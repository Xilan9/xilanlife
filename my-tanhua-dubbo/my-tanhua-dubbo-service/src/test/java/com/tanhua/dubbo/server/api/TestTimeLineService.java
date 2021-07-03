package com.tanhua.dubbo.server.api;

import com.tanhua.dubbo.server.service.TimeLineService;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestTimeLineService {

    @Autowired
    private TimeLineService timeLineService;

    @Test
    public void testSaveTimeLine() {
        ObjectId objectId = ObjectId.get();
        System.out.println("生成的id为：" + objectId.toHexString());
        CompletableFuture<String> future = this.timeLineService.saveTimeLine(1L, objectId);
        future.whenComplete((s, throwable) -> {
            System.out.println("执行完成：" + s);
        });

        System.out.println("异步方法执行完成");


        try {
            future.get(); //阻塞当前的主线程，等待异步执行的结束
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
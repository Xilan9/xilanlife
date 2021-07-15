package com.tanhua.dubbo.server.service;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.api.VisitorsApi;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.pojo.Visitors;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sound.midi.VoiceStatus;
import java.util.List;

@Service(version = "1.0.0")
public class VisitorsApiImpl implements VisitorsApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String VISITOR_REDIS_KEY = "VISITOR_USER";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public String saveVisitor(Long userId, Long visitorUserId, String from) {
        //校验一下
        if (!ObjectUtil.isAllNotEmpty(userId, visitorUserId, from)) {
            return null;
        }
        //查询访客在今日内是否记录过，如果记录过就不再记录了

        String today = DateUtil.today();
        Long minDate = DateUtil.parseDateTime(today + " 00:00:00").getTime();
        Long maxDate = DateUtil.parseDateTime(today + " 23:59:59").getTime();

        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("visitorUserId").is(visitorUserId)
                .andOperator(Criteria.where("date").gte(minDate),
                        Criteria.where("date").lte(maxDate)
                ));
        long count = this.mongoTemplate.count(null, Visitors.class);
        if (count > 0) {
            //已经记录过的
            return null;
        }
        Visitors visitors = new Visitors();
        visitors.setFrom(from);
        visitors.setVisitorUserId(visitorUserId);
        visitors.setUserId(userId);
        visitors.setDate(System.currentTimeMillis());
        visitors.setId(ObjectId.get());

        //存储数据
        this.mongoTemplate.save(visitors);

        return visitors.getId().toHexString();

    }

    @Override
    public List<Visitors> queryMyVisitor(Long userId) {
        //查询前五个访问的数据，按照访问时间倒叙排序
        //如果查询过了，纪录查询时间，后续查询需要按照时间往后去查询
        //上一次去查询列表的时间
        Long date = Convert.toLong(this.redisTemplate.opsForHash().get(VISITOR_REDIS_KEY, String.valueOf(userId)));
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Order.desc("date")));
        Query query = Query.query(Criteria.where("userId").is(userId))
                .with(pageRequest);
        if (ObjectUtil.isNotEmpty(date)) {
            query.addCriteria(Criteria.where("date").gte(date));
        }
        List<Visitors> visitorsList =this.mongoTemplate.find(query,Visitors.class);
        for (Visitors visitors : visitorsList) {
            Query queryScore = Query.query(Criteria.where("toUserId")
                    .is(userId).and("userId").is(visitors.getVisitorUserId()));
            RecommendUser recommendUser = this.mongoTemplate.findOne(queryScore, RecommendUser.class);
            if(ObjectUtil.isNotEmpty(recommendUser)){
                visitors.setScore(recommendUser.getScore());
            }else {
                //默认得分
                visitors.setScore(90d);
            }
        }
        return visitorsList;
    }
}

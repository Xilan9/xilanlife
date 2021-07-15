package com.tanhua.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import javafx.scene.control.TableColumn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;


@Service(version = "1.0.0")//dubbo服务
public class RecommendUserApiImpl implements RecommendUserApi {

    @Autowired
    private MongoTemplate mongoTemplate;  //

    @Override
    public RecommendUser queryWithMaxScore(Long userId) {
        //查询得分最高的用户，倒叙排序
        Query query= Query.query(Criteria.where("toUserId").is(userId))
                .with(Sort.by(Sort.Order.desc("score"))).limit(1);
        return this.mongoTemplate.findOne(query,RecommendUser.class);
    }

    @Override
    public PageInfo<RecommendUser> queryPageInfo(Long userId, Integer pageNum, Integer pageSize) {
        //分页和排序参数
        PageRequest pageRequest=PageRequest.of(pageNum -1,pageSize,Sort.by(Sort.Order.desc("score")));
        Query query=Query.query(Criteria.where("toUserId").is(userId))
                .with(pageRequest);
        List<RecommendUser> recommendUsers = this.mongoTemplate.find(query, RecommendUser.class);
        //暂时不提供数据总数
        return new PageInfo<>(0,pageNum,pageSize,recommendUsers);

    }

    @Override
    public Double queryScore(Long userId, Long toUserId) {
        Query query = Query.query(Criteria.where("toUserId").is(toUserId)
                .and("userId").is(userId));
        RecommendUser recommendUser = this.mongoTemplate.findOne(query, RecommendUser.class);
        if (null != recommendUser) {
            return recommendUser.getScore();
        }
        return null;
    }


}

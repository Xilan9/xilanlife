package com.tanhua.server.service;


import com.alibaba.dubbo.config.annotation.Reference;
import com.tanhua.dubbo.server.api.RecommendUserApi;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.vo.TodayBest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 负责与dubbo的服务进行交互
 */
@Service
public class RecommendUserService {

    @Reference(version = "1.0.0")
    private RecommendUserApi recommendUserApi;

    /**
     * 查询推荐用户的实现
     *
     * @param userId
     * @return
     */
    public TodayBest queryTodayBast(Long userId) {
        RecommendUser recommendUser = this.recommendUserApi.queryWithMaxScore(userId);
    if (recommendUser                                                                                                                                                 == null) {
            return null;
        }
        TodayBest todayBest = new TodayBest();
        todayBest.setId(recommendUser.getUserId());
        //缘分值
        double score = Math.floor(recommendUser.getScore());//取整，舍弃小数点后的数字
        todayBest.setFateValue(Double.valueOf(score).longValue());
        return todayBest;
    }

    public PageInfo<RecommendUser> queryRecommendUserList(Long userId, Integer page, Integer pageSize) {
        return this.recommendUserApi.queryPageInfo(userId,page,pageSize);
    }
}

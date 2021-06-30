package com.tanhua.server.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.dubbo.server.pojo.RecommendUser;
import com.tanhua.dubbo.server.vo.PageInfo;
import com.tanhua.server.pojo.User;
import com.tanhua.server.pojo.UserInfo;
import com.tanhua.server.vo.PageResult;
import com.tanhua.server.vo.RecommendUserQueryParam;
import com.tanhua.server.vo.TodayBest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 查询今日佳人的service实现
 */
@Service
public class TodayBestService {
    @Autowired
    private UserService userService;

    @Autowired
    private RecommendUserService recommendUserService;

    @Value("${tanhua.sso.default.user}")
    private Long defaultuser;

    @Autowired
    private UserInfoService userInfoService;

    public TodayBest queryTodayBast(String token) {
        //通过sso接口检验token是否有效
        User user = this.userService.queryUserByToken(token);
        if (user == null) {
            //token非法或过期
            return null;
        }
        //查询推荐用户
        TodayBest todayBest = this.recommendUserService.queryTodayBast(user.getId());
        if (todayBest == null) {
            //需要默认的推荐用户
            todayBest = new TodayBest();
            todayBest.setId(defaultuser);
            todayBest.setFateValue(80L); //固定缘分值
        }
        //补全个人信息
        UserInfo userInfo = this.userInfoService.queryUserInfoByUserId(todayBest.getId());
        if (userInfo == null) {
            return null;
        }
        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));
        todayBest.setGender(userInfo.getSex().getValue() == 1 ? "man" : "woman");
        todayBest.setAge(userInfo.getAge());
        return todayBest;
    }


    public PageResult queryRecommendation(String token, RecommendUserQueryParam queryParam) {
        //通过sso接口检验token是否有效
        User user = this.userService.queryUserByToken(token);
        if (user == null) {
            //token非法或过期
            return null;
        }
        PageResult pageResult = new PageResult();
        pageResult.setPage(queryParam.getPage());
        pageResult.setPagesize(queryParam.getPagesize());
        PageInfo<RecommendUser> pageInfo = this.recommendUserService.queryRecommendUserList(user.getId(), queryParam.getPage(), queryParam.getPagesize());
        List<RecommendUser> records = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            //没有查询到推荐的用户列表
            return pageResult;
        }
        //填充个人信息
        //收集推荐用户的ID
        Set<Long> userIds = new HashSet<>();
        for (RecommendUser record : records) {
            userIds.add(record.getUserId());
        }
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        //用户id参数
        queryWrapper.in("user_id", userIds);
        /*if (StringUtils.isNoneEmpty(queryParam.getGender())) {
            //需要设置性别参数
            queryWrapper.eq("sex", StringUtils.equals(queryParam.getGender(), "man") ? 1 : 2);
        }
        if (StringUtils.isNoneEmpty(queryParam.getCity())) {
            //添加城市的参数查询
            queryWrapper.like("city", queryParam.getCity());
        }
        if (queryParam.getAge() != null) {
            //设置年龄参数,年龄条件：小于等于
            queryWrapper.le("age",queryParam.getAge());
        }*/
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);
        if (CollectionUtils.isEmpty(userInfoList)){
            //没有查询到推荐的用户的基本信息
            return pageResult;
        }
        List<TodayBest>todayBests=new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            TodayBest todayBest=new TodayBest();
            todayBest.setId(userInfo.getUserId());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));
            todayBest.setGender(userInfo.getSex().getValue() == 1 ? "man" : "woman");
            todayBest.setAge(userInfo.getAge());

            //缘分值
            for (RecommendUser record : records) {
               if (record.getUserId().longValue()==userInfo.getUserId().longValue()){
                   double score = Math.floor(record.getScore());//取整，舍弃小数点后的数字
                   todayBest.setFateValue(Double.valueOf(score).longValue());
                   break;
               }
            }

            todayBests.add(todayBest);
        }
        //按照缘分值进行倒序排序
        Collections.sort(todayBests,(o1, o2) -> new Long(o2.getFateValue()-o1.getFateValue()).intValue());

        pageResult.setItems(todayBests);
        return pageResult;
    }
}

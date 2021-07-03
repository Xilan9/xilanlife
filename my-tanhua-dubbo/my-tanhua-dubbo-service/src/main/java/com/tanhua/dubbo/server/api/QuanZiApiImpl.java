package com.tanhua.dubbo.server.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.dubbo.config.annotation.Service;
import com.tanhua.dubbo.server.enums.CommentType;
import com.tanhua.dubbo.server.enums.IdType;
import com.tanhua.dubbo.server.pojo.Album;
import com.tanhua.dubbo.server.pojo.Comment;
import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.pojo.TimeLine;
import com.tanhua.dubbo.server.service.IdService;
import com.tanhua.dubbo.server.service.TimeLineService;
import com.tanhua.dubbo.server.vo.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service(version = "1.0.0")
public class QuanZiApiImpl implements QuanZiApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IdService idService;

    @Autowired
    private TimeLineService timeLineService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    //评论数据存储在Redis中key的前缀
    private static final String COMMENT_REDIS_KEY_PREFIX = "QUANZI_COMMENT_";

    //用户是否点赞的前缀
    private static final String COMMENT_USER_LIKE_REDIS_KEY_PREFIX = "USER_LIKE_";


    //用户是否喜欢的前缀
    private static final String COMMENT_USER_LOVE_REDIS_KEY_PREFIX = "USER_LOVE_";


    @Override
    public PageInfo<Publish> queryPublishList(Long userId, Integer page, Integer pageSize) {
        //查询好友的动态，实际查询时间线表
        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        Pageable pageable = PageRequest.of(page - 1, pageSize,
                Sort.by(Sort.Order.desc("date")));
        Query query = new Query().with(pageable);
        List<TimeLine> timeLinesList = this.mongoTemplate.find(query, TimeLine.class, "quanzi_time_line_" + userId);
        if (CollUtil.isEmpty(timeLinesList)) {
            //没有查询到数据
            return pageInfo;
        }

        //获取到时间线列表中的发布ID列表
        List<Object> ids = CollUtil.getFieldValues(timeLinesList, "publishId");
        //根据动态id查询动态列表
        Query queryPublish = Query.query(Criteria.where("id").in(ids)).with(Sort.by(Sort.Order.desc("created")));
        List<Publish> publishes = this.mongoTemplate.find(queryPublish, Publish.class);
        pageInfo.setRecords(publishes);
        return pageInfo;
    }

    /**
     * 发布动态
     *
     * @param publish
     * @return 发布成功返回动态id
     */
    public String savePublish(Publish publish) {
        //对publish对象做一个校验
        if (!ObjectUtil.isAllNotEmpty(publish.getText(), publish.getUserId())) {
            //发布失败
            return null;
        }
        //设置主键ID
        publish.setId(ObjectId.get());
        try {
            //设置自增长的ID
            publish.setPid(this.idService.createId(IdType.PUBLISH));
            publish.setCreated(System.currentTimeMillis());

            //写入到publish表中
            this.mongoTemplate.save(publish);


            //写入相册表
            Album album = new Album();
            album.setId(ObjectId.get());
            album.setCreated(System.currentTimeMillis());
            album.setPublishId(publish.getId());
            this.mongoTemplate.save(album, "quanzi_album_" + publish.getUserId());
            // 写入好友的时间线表，异步写入
            this.timeLineService.saveTimeLine(publish.getUserId(), publish.getId());
        } catch (Exception e) {
            //TODO 出现异常需要做一个事物的回滚。mongodb的单节点服务不支持回滚事物，不做回滚了
            log.error("发布动态失败   publish=" + publish, e);
        }


        return publish.getId().toHexString();
    }

    public PageInfo<Publish> queryRecommendPublishList(Long userId, Integer page, Integer pageSize) {

        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        //查询推荐数据
        String key = "QUANZI_PUBLISH_RECOMMEND_" + userId;
        String data = this.redisTemplate.opsForValue().get(key);
        if (StrUtil.isEmpty(data)) {
            return pageInfo;
        }
        //查询到的pid进行分页处理
        List<String> pids = StrUtil.split(data, ',');
        //计算分页
        //[0, 10]
        int[] startEnd = PageUtil.transToStartEnd(page - 1, pageSize);
        int startIndex = startEnd[0]; //开始
        int endIndex = Math.min(startEnd[1], pids.size()); //结束
        List<Long> pidLongList = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            pidLongList.add(Long.valueOf(pids.get(i)));
        }

        if (CollUtil.isEmpty(pidLongList)) {
            //没有查询到数据
            return pageInfo;
        }
        //根据pid查询publish
        Query query = Query.query(Criteria.where("pid").in(pidLongList))
                .with(Sort.by(Sort.Order.desc("created")));
        List<Publish> publishList = this.mongoTemplate.find(query, Publish.class);
        if (CollUtil.isEmpty(publishList)) {
            //没有查询到数据
            return pageInfo;
        }

        pageInfo.setRecords(publishList);
        return pageInfo;

    }

    @Override
    public Publish queryPublishById(String id) {
        return this.mongoTemplate.findById(new ObjectId(id), Publish.class);
    }

    /**
     * 点赞
     *
     * @param userId
     * @param publishId
     * @return
     */
    @Override
    public Boolean likeComment(Long userId, String publishId) {
        //判断该用户是否已经点赞，已经点赞了就返回
        if (this.queryUserIsLike(userId, publishId)) {
            return false;
        }
        //保存comment数据
        Boolean result = this.saveComment(userId, publishId, CommentType.LIKE, null);

        if (!result) {
            return false;
        }
        //修改redis中的点赞数与是否点赞

        //点赞数
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String hashKey = CommentType.LIKE.toString();

        this.redisTemplate.opsForHash().increment(redisKey, hashKey, 1);


        //用户是否点赞
        String userHashKey = this.getCommentUserLikeRedisKeyPrefix(userId);
        this.redisTemplate.opsForHash().put(redisKey, userHashKey, "1");

        return true;
    }

    /**
     * 去掉点赞
     *
     * @param userId
     * @param publishId
     * @return
     */

    @Override
    public Boolean disLikeComment(Long userId, String publishId) {

        //判断用户是否已经点赞，如果没有点赞就返回
        if (!this.queryUserIsLike(userId, publishId)) {
            return false;
        }
        //删除评论数据
        Boolean result = this.removeComment(userId, publishId, CommentType.LIKE);
        if (!result) {
            return false;
        }
        //修改Redis中的数据
        //修改点赞数
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String hashKey = CommentType.LIKE.toString();
        this.redisTemplate.opsForHash().increment(redisKey, hashKey, -1);
        //用户是否点赞
        String userHashKey = this.getCommentUserLikeRedisKeyPrefix(userId);
        this.redisTemplate.opsForHash().delete(redisKey, userHashKey);
        return true;
    }

    private String getCommentRedisKeyPrefix(String publishId) {
        return COMMENT_REDIS_KEY_PREFIX + publishId;
    }

    private String getCommentUserLikeRedisKeyPrefix(Long userId) {
        return COMMENT_USER_LIKE_REDIS_KEY_PREFIX + userId;
    }

    /**
     * 查询点赞的数量
     *
     * @param publishId
     * @return
     */
    @Override
    public Long queryLikeCount(String publishId) {
        //从redis中命中查询，命中就返回
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String hashKey = CommentType.LIKE.toString();
        Object data = this.redisTemplate.opsForHash().get(redisKey, hashKey);
        if (ObjectUtil.isNotEmpty(data)) {
            return Convert.toLong(data);
        }
        //查询MongoDB

        Long count = this.queryCommentCount(publishId, CommentType.LIKE);
        //写入redis中
        this.redisTemplate.opsForHash().put(redisKey, hashKey, String.valueOf(count));

        return count;
    }

    /**
     * 用户是否点赞
     *
     * @param userId
     * @param publishId
     * @return
     */
    @Override
    public Boolean queryUserIsLike(Long userId, String publishId) {
        //从redis中查询
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String userHashKey = this.getCommentUserLikeRedisKeyPrefix(userId);
        Object data = this.redisTemplate.opsForHash().get(redisKey, userHashKey);
        if (ObjectUtil.isNotEmpty(data)) {
            return StrUtil.equals(Convert.toStr(data), "1");
        }
        //查询MongoDB来确定是否点赞
        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(CommentType.LIKE)
        );
        long count = this.mongoTemplate.count(query, Comment.class);
        if (count == 0) {
            return false;
        }
        //写入redis中
        this.redisTemplate.opsForHash().put(redisKey, userHashKey, "1");
        return true;
    }

    /**
     * 保存comment方法
     *
     * @return
     */
    private Boolean saveComment(Long userId, String publishId,
                                CommentType commentType, String content) {
        try {
            Comment comment = new Comment();
            comment.setId(ObjectId.get());
            comment.setUserId(userId);
            comment.setPublishId(new ObjectId(publishId));
            // 评论类型
            comment.setCommentType(commentType.getType());
            //内容
            comment.setContent(content);
            comment.setCreated(System.currentTimeMillis());

            Publish publish = this.queryPublishById(publishId);
            if (ObjectUtil.isNotEmpty(publish)){
                comment.setPublishUserId(publish.getUserId());
            }else {
                //查询评论
                Comment myComment =this.queryCommentById(publishId);
                if(ObjectUtil.isNotEmpty(myComment)){
                    comment.setPublishUserId(myComment.getUserId());
                }else {
                    //TODO 其他情况，比如小视频啥的
                }
            }
            this.mongoTemplate.save(comment);
            return true;
        } catch (Exception e) {
            log.error("保存comment出错 userId" + userId + "publishId=" + publishId + ",commentType =" + commentType, e);
        }
        return false;
    }

    /**
     * 根据ID查询comment对象
     * @param id
     * @return
     */
    private Comment queryCommentById(String id){
        return this.mongoTemplate.findById(new ObjectId(id),Comment.class);
    }


    /**
     * 删除评论数据
     *
     * @param userId
     * @param publishId
     * @param commentType
     * @return
     */
    private Boolean removeComment(Long userId, String publishId, CommentType commentType) {
        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("publishId").is(new ObjectId(publishId))
                .and("commentType").is(commentType.getType())
        );
        return this.mongoTemplate.remove(query, Comment.class).getDeletedCount() > 0;
    }

    /**
     * 查询数量
     *
     * @param publishId
     * @param commentType
     * @return
     */
    private Long queryCommentCount(String publishId, CommentType commentType) {
        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(publishId))
                .and("commentType").is(commentType.getType())
        );
        return this.mongoTemplate.count(query, Comment.class);
    }


    @Override
    public Boolean loveComment(Long userId, String publishId) {
        //查询该用户是否已经喜欢
        if (this.queryUserIsLove(userId, publishId)) {
            return false;
        }

        //喜欢
        boolean result = this.saveComment(userId, publishId, CommentType.LOVE, null);
        if (!result) {
            return false;
        }

        //喜欢成功后，修改Redis中的总的喜欢数
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String hashKey = CommentType.LOVE.toString();
        this.redisTemplate.opsForHash().increment(redisKey, hashKey, 1);

        //标记用户已经喜欢
        hashKey = this.getCommentUserLoveRedisKey(userId);
        this.redisTemplate.opsForHash().put(redisKey, hashKey, "1");

        return true;
    }

    private String getCommentUserLoveRedisKey(Long userId) {
        return COMMENT_USER_LOVE_REDIS_KEY_PREFIX + userId;
    }

    @Override
    public Boolean disLoveComment(Long userId, String publishId) {
        if (!this.queryUserIsLove(userId, publishId)) {
            //如果用户没有喜欢，就直接返回
            return false;
        }

        boolean result = this.removeComment(userId, publishId, CommentType.LOVE);
        if (!result) {
            //删除失败
            return false;
        }

        //删除redis中的记录
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String hashKey = this.getCommentUserLoveRedisKey(userId);
        this.redisTemplate.opsForHash().delete(redisKey, hashKey);
        this.redisTemplate.opsForHash().increment(redisKey, CommentType.LOVE.toString(), -1);

        return true;
    }

    @Override
    public Long queryLoveCount(String publishId) {
        //首先从redis中命中，如果命中的话就返回，没有命中就查询Mongodb
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String hashKey = CommentType.LOVE.toString();
        Object value = this.redisTemplate.opsForHash().get(redisKey, hashKey);
        if (ObjectUtil.isNotEmpty(value)) {
            return Convert.toLong(value);
        }

        //查询count
        Long count = this.queryCommentCount(publishId, CommentType.LOVE);

        //存储到redis中
        this.redisTemplate.opsForHash().put(redisKey, hashKey, String.valueOf(count));

        return count;
    }

    @Override
    public Boolean queryUserIsLove(Long userId, String publishId) {
        String redisKey = this.getCommentRedisKeyPrefix(publishId);
        String hashKey = this.getCommentUserLoveRedisKey(userId);
        Object value = this.redisTemplate.opsForHash().get(redisKey, hashKey);
        if (ObjectUtil.isNotEmpty(value)) {
            return StrUtil.equals(Convert.toStr(value), "1");
        }

        //查询mongodb
        Query query = Query.query(Criteria.where("publishId")
                .is(new ObjectId(publishId))
                .and("userId").is(userId)
                .and("commentType").is(CommentType.LOVE.getType()));
        long count = this.mongoTemplate.count(query, Comment.class);
        if (count == 0) {
            return false;
        }

        //标记用户已经喜欢
        this.redisTemplate.opsForHash().put(redisKey, hashKey, "1");

        return true;
    }

    /**
     * 查询评论列表
     *
     * @param publishId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.asc("created")));

        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(publishId))
                .and("commentType").is(CommentType.COMMENT.getType())).with(pageRequest);
        List<Comment> commentList = this.mongoTemplate.find(query, Comment.class);
        PageInfo<Comment> pageInfo=new PageInfo<>();
        pageInfo.setPageSize(pageSize);
        pageInfo.setPageNum(page);
        pageInfo.setRecords(commentList);
        return pageInfo;
    }

    /**
     * 发表评论
     *
     * @param userId
     * @param publishId
     * @param content
     * @return
     */
    @Override
    public Boolean saveComment(Long userId, String publishId, String content) {
        return this.saveComment(userId, publishId, CommentType.COMMENT, content);
    }

}

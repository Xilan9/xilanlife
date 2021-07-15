package com.tanhua.dubbo.server.api;


import com.tanhua.dubbo.server.pojo.Comment;
import com.tanhua.dubbo.server.pojo.Publish;
import com.tanhua.dubbo.server.vo.PageInfo;

public interface QuanZiApi {
    /**
     * 查询好友动态
     *
     * @param userId 用户id
     * @param page 当前页数
     * @param pageSize 每一页查询的数据条数
     * @return
     */
    PageInfo<Publish> queryPublishList(Long userId, Integer page, Integer pageSize);


    /**
     * 发布动态
     *
     * @param publish
     * @return 发布成功返回动态id
     */
    String savePublish(Publish publish);

    /**
     * 查询推荐动态
     *
     * @param userId 用户id
     * @param page 当前页数
     * @param pageSize 每一页查询的数据条数
     * @return
     */
    PageInfo<Publish> queryRecommendPublishList(Long userId, Integer page, Integer pageSize);

    /**
     * 根据id查询动态
     *
     * @param id 动态id
     * @return
     */
    Publish queryPublishById(String id);

    /**
     * 点赞
     *
     * @param userId
     * @param publishId
     * @return
     */
    Boolean likeComment(Long userId, String publishId);

    /**
     * 取消点赞
     *
     * @param userId
     * @param publishId
     * @return
     */
    Boolean disLikeComment(Long userId, String publishId);

    /**
     * 查询点赞数
     *
     * @param publishId
     * @return
     */
    Long queryLikeCount(String publishId);

    /**
     * 查询用户是否点赞该动态
     *
     * @param userId
     * @param publishId
     * @return
     */
    Boolean queryUserIsLike(Long userId, String publishId);

    /**
     * 喜欢
     *
     * @param userId
     * @param publishId
     * @return
     */
    Boolean loveComment(Long userId, String publishId);

    /**
     * 取消喜欢
     *
     * @param userId
     * @param publishId
     * @return
     */
    Boolean disLoveComment(Long userId, String publishId);

    /**
     * 查询喜欢数
     *
     * @param publishId
     * @return
     */
    Long queryLoveCount(String publishId);

    /**
     * 查询用户是否喜欢该动态
     *
     * @param userId
     * @param publishId
     * @return
     */
    Boolean queryUserIsLove(Long userId, String publishId);

    /**
     * 查询评论
     *
     * @return
     */
    PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize);

    /**
     * 发表评论
     *
     * @param userId
     * @param publishId
     * @param content
     * @return
     */
    Boolean saveComment(Long userId, String publishId, String content);

    /**
     * 查询评论数
     * @param publishId
     * @return
     */
    Long queryCommentCount(String publishId);

    /**
     * 查询对我的点赞消息列表
     *
     * @return
     */
    PageInfo<Comment> queryLikeCommentListByUser(Long userId, Integer page, Integer pageSize);

    /**
     * 查询对我的喜欢消息列表
     *
     * @return
     */
    PageInfo<Comment> queryLoveCommentListByUser(Long userId, Integer page, Integer pageSize);

    /**
     * 查询对我的评论消息列表
     *
     * @return
     */
    PageInfo<Comment> queryCommentListByUser(Long userId, Integer page, Integer pageSize);

    /**
     * 查询相册表
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Publish> queryAlbumList(Long userId, Integer page, Integer pageSize);






}




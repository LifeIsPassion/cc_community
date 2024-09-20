package com.cyt.community.dao;


import com.cyt.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Mapper
public interface DiscussPostMapper {
    //查询帖子
    List<DiscussPost> selectDiscussPost(int userId,int offset,int limit,int orderMode);
    //查询帖子总数
    int selectDiscussPostRows(@Param("userId") int userId);
    //插入一条帖子
    int insertDiscussPost(DiscussPost discussPost);
    //根据id查询帖子
    DiscussPost selectDiscussPostById(int id);
    //


    int updateComment(int id,int commentCount);
    //更新帖子类型
    int updateDiscussPostType(int id,int type);
    //更新帖子状态,即删除帖子
    int updateDiscussPostStatus(int id,double status);
    //更新帖子分数
    int updateDiscussPostScore(int id,int score);
}

package com.cyt.community.dao;


import com.cyt.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentByEntityId(int entityType ,int entityId,int offset,int limit);

    int selectCountByEntityId(int entityType,int entityId);

    int insertComment(Comment comment);

    Comment selectCommentById(int id);
}

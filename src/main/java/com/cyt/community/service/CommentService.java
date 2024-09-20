package com.cyt.community.service;

import com.cyt.community.dao.CommentMapper;
import com.cyt.community.entity.Comment;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {
    @Autowired
    CommentMapper commentMapper;

    @Autowired
    SensitiveFilter sensitiveFilter;

    @Autowired
    DiscussPostService discussPostService;
    public List<Comment> findSelectCommentByEntityId(int entityType,int entityId,int offset,int limit){
        return commentMapper.selectCommentByEntityId(entityType,entityId,offset,limit);
    }
    public int findSelectCountByEntityId(int entityType,int entityId){
        return commentMapper.selectCountByEntityId(entityType,entityId);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
       if(comment == null){
           throw new IllegalArgumentException("参数无法为空");
       }
       comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
       comment.setContent(sensitiveFilter.filter(comment.getContent()));
       int row = commentMapper.insertComment(comment);
       //
        if(comment.getEntityType() == ENTITY_TYPE_POST){
            int count = commentMapper.selectCountByEntityId(comment.getEntityType(),comment.getEntityId());
            discussPostService.updateComment(comment.getEntityId(),count);
        }
        return row;
    }
    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }
}

package com.cyt.community.controller;

import com.cyt.community.entity.Comment;
import com.cyt.community.entity.DiscussPost;
import com.cyt.community.entity.Event;
import com.cyt.community.event.EventProducer;
import com.cyt.community.service.CommentService;
import com.cyt.community.service.DiscussPostService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.HostLocal;
import com.cyt.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    CommentService commentService;

    @Autowired
    HostLocal hostLocal;

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    EventProducer eventProducer;

    @Autowired
    RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{postId}",method = RequestMethod.POST)
    public String addComment(Comment comment, @PathVariable("postId") int postId){
        comment.setCreateTime(new Date());
        comment.setUserId(hostLocal.getUser().getId());
        comment.setStatus(0);
        commentService.addComment(comment);

        //发送消息
        Event event = new Event().setTopic(TOPIC_COMMENT)
                .setUserId(hostLocal.getUser().getId())
                .setEntityId(comment.getEntityId())
                .setEntityType(comment.getEntityType())
                .setData("postId",postId);
        if(comment.getEntityType() == ENTITY_TYPE_POST){
            DiscussPost discussPost = discussPostService.findDiscussPostById(postId);
            event.setEntityUserId(discussPost.getUserId());
        }else if(comment.getEntityType() == ENTITY_TYPE_COMMENT){
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // 触发发帖事件
            //这里的目的是为了把被评论后的帖子添加到el中，即更新其中的数据，方便查询
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(postId);
            eventProducer.fireEvent(event);

            //重新计算分数
            String scoreKey = RedisKeyUtil.getPostScore();
            redisTemplate.opsForSet().add(scoreKey,postId);
        }
        return "redirect:/discuss/detail/" + postId;
    }
}

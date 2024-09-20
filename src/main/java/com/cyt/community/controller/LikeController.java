package com.cyt.community.controller;

import com.cyt.community.entity.Event;
import com.cyt.community.entity.User;
import com.cyt.community.event.EventProducer;
import com.cyt.community.service.CommentService;
import com.cyt.community.service.DiscussPostService;
import com.cyt.community.service.LikeService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.cyt.community.util.HostLocal;
import com.cyt.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {
    @Autowired
    LikeService likeService;

    @Autowired
    HostLocal hostLocal;

    @Autowired
    EventProducer eventProducer;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    CommentService commentService;

    @RequestMapping(path = "/like",method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType,int entityId,int postId,int postOrCom){
        User user = hostLocal.getUser();
        int entityUserId;
        if(entityType == 1 && postOrCom == 1) {
            //postOrCom = 1说明是帖子
            entityUserId = discussPostService.findDiscussPostById(entityId).getUserId();
        }else{
            entityUserId = commentService.findCommentById(entityId).getUserId();
        }
        //点赞
        likeService.like(user.getId(),entityType,entityId,entityUserId);

        //点赞状态与数量
        Map<String,Object> map = new HashMap<>();

        long likeCount = likeService.findEntityLikeCount(entityType,entityId);

        int likeStatus = likeService.findEntityLikeStatus(user.getId(),entityType,entityId);

        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 触发点赞事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostLocal.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);

            //
            //重新计算分数,只算帖子的
            if(entityType == ENTITY_TYPE_POST){
                String scoreKey = RedisKeyUtil.getPostScore();
                redisTemplate.opsForSet().add(scoreKey,postId);
            }
        }

        return CommunityUtil.getJson(0,null,map);

    }

}

package com.cyt.community.controller;


import com.cyt.community.dao.CommentMapper;
import com.cyt.community.entity.*;
import com.cyt.community.event.EventProducer;
import com.cyt.community.service.CommentService;
import com.cyt.community.service.DiscussPostService;
import com.cyt.community.service.LikeService;
import com.cyt.community.service.UserService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.cyt.community.util.HostLocal;
import com.cyt.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.management.ObjectName;
import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    HostLocal hostLocal;

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    UserService userService;

    @Autowired
    LikeService likeService;

    @Autowired
    CommentService commentService;

    @Autowired
    EventProducer eventProducer;

    @Autowired
    RedisTemplate redisTemplate;

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content){
        User user = hostLocal.getUser();
        if(user == null){
            return CommunityUtil.getJson(403,"还未登录");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setUserId(user.getId());
        discussPost.setCreateTime(new Date());
        //
        discussPostService.addDiscussPost(discussPost);

        //发布通知事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);

        //重新计算分数
        String scoreKey = RedisKeyUtil.getPostScore();
        redisTemplate.opsForSet().add(scoreKey,scoreKey);

        return CommunityUtil.getJson(0,"发布成功");
    }

    @RequestMapping(path = "/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        //
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);

        //获取帖子赞的信息
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId());
        //当用户未登录时应该是看不到自己的具体点赞信息的
        int likeStatus = hostLocal.getUser() == null ? 0:
                likeService.findEntityLikeStatus(hostLocal.getUser().getId(),ENTITY_TYPE_POST,post.getId());
        model.addAttribute("likeCount",likeCount);
        model.addAttribute("likeStatus",likeStatus);
        //
        page.setPath("/discuss/detail/" + discussPostId);
        page.setLimit(5);
        page.setRows(post.getCommentCount());
        //评论：给帖子的评论
        //回复：给评论的评论
        //获取评论列表
        List<Comment> commentList = commentService.findSelectCommentByEntityId(ENTITY_TYPE_POST,post.getId(),page.getOffset(),page.getLimit());

        List<Map<String,Object>> commentVoList = new ArrayList<>();

        if(commentList != null){
            for(Comment comment : commentList){
                Map<String, Object> commentVo = new HashMap<>();
                //帖子的评论
                commentVo.put("comment",comment);
                //发布者
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                //点赞
                long likeCountOfComment = likeService.findEntityLikeCount(ENTITY_TYPE_POST,comment.getId());
                int likeStatusOfComment = hostLocal.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostLocal.getUser().getId(),ENTITY_TYPE_POST,comment.getId());
                commentVo.put("likeCount",likeCountOfComment);
                commentVo.put("likeStatus",likeStatusOfComment);
                //回复
                List<Comment> replayList = commentService.findSelectCommentByEntityId(ENTITY_TYPE_COMMENT,comment.getId(),0,Integer.MAX_VALUE);
                List<Map<String,Object>> replayVoList = new ArrayList<>();
                if(replayList != null){
                    for(Comment replay:replayList){
                        Map<String,Object> replayMap = new HashMap<>();
                        //
                        replayMap.put("reply",replay);
                        //
                        replayMap.put("user",userService.findUserById(replay.getUserId()));
                        //获取回复点赞信息
                        long likeCountOfReplay = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT,replay.getId());
                        int likeStatusOfReplay = hostLocal.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostLocal.getUser().getId(),ENTITY_TYPE_COMMENT,replay.getId());
                        replayMap.put("likeCount",likeCountOfReplay);
                        replayMap.put("likeStatus",likeStatusOfReplay);
                        User target = replay.getTargetId() == 0 ? null: userService.findUserById(replay.getTargetId());
                        replayMap.put("target",target);
                        //
                        replayVoList.add(replayMap);
                    }
                }
                commentVo.put("replys",replayVoList);
                int count = commentService.findSelectCountByEntityId(ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("replyCount",count);
                //
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }


    // 置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        discussPostService.updateDiscussPostType(id, 1);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostLocal.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJson(0);
    }

    // 加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        discussPostService.updateDiscussPostStatus(id, 1);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostLocal.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJson(0);
    }

    // 删除
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateDiscussPostStatus(id, 2);

        // 触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostLocal.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJson(0);
    }
}

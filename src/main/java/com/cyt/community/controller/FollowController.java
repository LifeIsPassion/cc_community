package com.cyt.community.controller;


import com.cyt.community.entity.Event;
import com.cyt.community.entity.Page;
import com.cyt.community.entity.User;
import com.cyt.community.event.EventProducer;
import com.cyt.community.service.FollowService;
import com.cyt.community.service.UserService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.cyt.community.util.HostLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {
    @Autowired
    private FollowService followService;

    @Autowired
    private HostLocal hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityUserId) {
        User user = hostHolder.getUser();

        followService.follow(user.getId(), entityType, entityUserId);

        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityUserId)
                .setEntityUserId(entityUserId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJson(0, "已关注!");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.unFollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJson(0, "已取消关注!");
    }
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/" + userId);
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        int rows = (int) Math.min(Integer.MAX_VALUE, followeeCount); // 进行范围检查，防止溢出
        page.setRows(rows);

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);

        return "/site/followee";
    }

    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/" + userId);
        long followeeCount = followService.findFollowerCount(userId, ENTITY_TYPE_USER);
        int rows = (int) Math.min(Integer.MAX_VALUE, followeeCount); // 进行范围检查，防止溢出
        page.setRows(rows);

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);

        return "/site/follower";
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }

        return followService.findFolloweeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }
}

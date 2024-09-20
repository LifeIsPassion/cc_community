package com.cyt.community.controller;

import com.cyt.community.entity.DiscussPost;
import com.cyt.community.entity.Page;
import com.cyt.community.entity.User;
import com.cyt.community.service.CommentService;
import com.cyt.community.service.DiscussPostService;
import com.cyt.community.service.LikeService;
import com.cyt.community.service.UserService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.HostLocal;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    UserService userService;

    @Autowired
    LikeService likeService;

    @Autowired
    HostLocal hostLocal;

    @Autowired
    CommentService commentService;

    @RequestMapping(path = "/" ,method = RequestMethod.GET)
    public String root(){
        return "forward:/index";
    }


    @RequestMapping(path = "/index",method = RequestMethod.GET)
    public String getHomePage(Model model,Page page,@RequestParam(value = "orderMode" ,defaultValue = "0")int orderMode){
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode=" + orderMode);
        List<DiscussPost> list = discussPostService.findDiscussPosts(0,page.getOffset(), page.getLimit(),orderMode);
        List<Map<String,Object>> discussPost = new ArrayList<>();
        if(list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId());
                map.put("likeCount",likeCount);
                int commentCount = commentService.findSelectCountByEntityId(ENTITY_TYPE_POST,post.getId());
                map.put("commentCount",commentCount);
                discussPost.add(map);
            }
        }
        model.addAttribute("discussPost", discussPost);
        return "/index";
    }
    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }
}

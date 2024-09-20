package com.cyt.community.controller;

import com.cyt.community.entity.DiscussPost;
import com.cyt.community.service.ElasticsearchService;
import com.cyt.community.service.LikeService;
import com.cyt.community.service.UserService;
import com.cyt.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    ElasticsearchService elasticsearchService;

    @Autowired
    UserService userService;

    @Autowired
    LikeService likeService;

    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, com.cyt.community.entity.Page page, Model model){
        Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyword,page.getCurrent() - 1,page.getLimit());
        List<Map<String,Object>> discussPostMap = new ArrayList<>();
        if(searchResult != null) {
            for (DiscussPost discussPost : searchResult) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", discussPost);
                // 作者
                map.put("user", userService.findUserById(discussPost.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId()));

                discussPostMap.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPostMap);
        model.addAttribute("keyword", keyword);

        // 分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());

        return "/site/search";
    }
}

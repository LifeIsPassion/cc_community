package com.cyt.community;

import com.cyt.community.dao.DiscussPostMapper;
import com.cyt.community.dao.elasticsearch.DiscussPostRepository;
import com.cyt.community.service.DiscussPostService;
import com.cyt.community.util.MailClient;
import com.cyt.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.cyt.community.entity.DiscussPost;

import java.util.List;

@SpringBootTest
class CommunityApplicationTests {

    @Autowired
    DiscussPostMapper discussPostMapper;

    @Autowired
    MailClient mailClient;

    @Autowired
    SensitiveFilter sensitiveFilter;

    @Autowired
    DiscussPostRepository discussPostRepository;

    @Autowired
    DiscussPostService discussPostService;
    @Test
    public void test0(){
        System.out.println(0);
    }

    @Test
    public void test2(){
        mailClient.sendMail("3138190369@qq.com","TestEmail","hello springemail");
    }

    @Test
    public void test3(){
        DiscussPost discussPost = discussPostService.findDiscussPostById(109);
        discussPostRepository.save(discussPost);
    }



}

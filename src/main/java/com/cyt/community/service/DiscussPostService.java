package com.cyt.community.service;

import com.cyt.community.util.SensitiveFilter;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.cyt.community.dao.DiscussPostMapper;
import com.cyt.community.entity.DiscussPost;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    private LoadingCache<String,List<DiscussPost>> discussPostCache;

    private LoadingCache<Integer,Integer> postRowsCache;

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @PostConstruct
    private void init(){
        discussPostCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(@NonNull String s) throws Exception {
                        if(s == null || s.length() == 0){
                            throw new IllegalArgumentException("参数不得为空");
                        }

                        String [] param = s.split(":");
                        if(param == null || param.length < 2){
                            throw new IllegalArgumentException("参数格式错误");
                        }

                        //二级Redis缓存

                        //
                        int offset = Integer.parseInt(param[0]);
                        int limit = Integer.parseInt(param[1]);
                        //
                        logger.info("load post list from DB");
                        return discussPostMapper.selectDiscussPost(0,offset,limit,1);
                    }
                });
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {
                        logger.info("load post rows from DB");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });

    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit,int orderMode) {
        if(userId == 0 && orderMode == 1){
            return discussPostCache.get(offset + ":" + limit);
        }
        logger.info("load post list from DB");
        return discussPostMapper.selectDiscussPost(userId, offset, limit,orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if(userId == 0){
            return postRowsCache.get(userId);
        }
        logger.info("load post rows from DB");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost discussPost){
        if(discussPost == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        //转义
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        //过滤敏感词
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));
        String t = sensitiveFilter.filter(discussPost.getTitle());
        discussPost.setTitle(t);

        return discussPostMapper.insertDiscussPost(discussPost);
    }
    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateComment(int id,int comment){
        return discussPostMapper.updateComment(id,comment);
    }

    public int updateDiscussPostType(int id,int type){
        return discussPostMapper.updateDiscussPostType(id,type);
    }

    public int updateDiscussPostStatus(int id,int status){
        return discussPostMapper.updateDiscussPostStatus(id,status);
    }
    public int updateDiscussPostScore(int id,double score){
        return discussPostMapper.updateDiscussPostStatus(id,score);
    }
}

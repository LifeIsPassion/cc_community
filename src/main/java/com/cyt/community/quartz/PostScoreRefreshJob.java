package com.cyt.community.quartz;

import com.cyt.community.entity.DiscussPost;
import com.cyt.community.service.DataService;
import com.cyt.community.service.DiscussPostService;
import com.cyt.community.service.ElasticsearchService;
import com.cyt.community.service.LikeService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.cyt.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {

    Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    LikeService likeService;

    @Autowired
    ElasticsearchService elasticsearchService;

    @Autowired
    RedisTemplate redisTemplate;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2003-11-15 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化失败");
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postScoreKey = RedisKeyUtil.getPostScore();
        BoundSetOperations operations = redisTemplate.boundSetOps(postScoreKey);

        if(operations.size() == 0){
            logger.info("任务取消没有需要更新帖子");
        }
        logger.info("开始进行任务" + operations.size());
        while (operations.size() > 0){
            this.refresh((Integer) operations.pop());
        }
        logger.info("任务结束" + operations.size());
    }

    private void refresh(int postId){
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);

        if(discussPost == null){
            logger.error("帖子不存在 id = " + discussPost.getId());
        }

        //获取帖子数据 状态 点赞 评论数量
        boolean postStatus = discussPost.getStatus() == 1 ;

        int postCommentCount = discussPost.getCommentCount();

        long postLike = likeService.findEntityLikeCount(ENTITY_TYPE_POST,discussPost.getId());

        // 计算权重
        double w = (postStatus ? 75 : 0) + postCommentCount * 10 + postLike * 2;
        // 分数 = 帖子权重 + 距离天数
        double score = Math.log10(Math.max(w, 1))
                + (discussPost.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);

        //更新帖子分数
        discussPostService.updateDiscussPostScore(postId,score);
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);

    }
}

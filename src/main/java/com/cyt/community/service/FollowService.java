package com.cyt.community.service;

import com.cyt.community.entity.User;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    UserService userService;


    public void follow(int userId,int entityType,int entityUserId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId,entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityUserId);
                operations.multi();
                operations.opsForZSet().add(followeeKey,entityUserId,System.currentTimeMillis());
                operations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                return operations.exec();
            }
        });
    }


    public void unFollow(int userId,int entityType,int entityUserId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId,entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityUserId);
                operations.multi();
                operations.opsForZSet().remove(followeeKey,entityUserId);
                operations.opsForZSet().remove(followerKey,userId);

                return operations.exec();
            }
        });
    }



    //查询用户的关注数量
    public Long findFolloweeCount(int userId, int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId,entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //查询实体的关注数量
    public Long findFollowerCount(int entityType,int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityId);
        return  redisTemplate.opsForZSet().zCard(followerKey);
    }

    //查询某一个实体是否在用户的的关注列表当中
    public boolean findFolloweeStatus(int userId, int entityType, int entityId){
        String followeeStatus = RedisKeyUtil.getFolloweeKey(userId,entityType);
        return redisTemplate.opsForZSet().score(followeeStatus,entityId) != null;
    }

    //查询用户关注列表
    public List<Map<String,Object>> findFollowees(int userId,int offset,int limit){
        //
        List<Map<String,Object>> list = new ArrayList<>();
        //这里只查询关注的用户
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId,ENTITY_TYPE_USER);
        //
        Set<Integer> followeeId = redisTemplate.opsForZSet().reverseRange(followeeKey,offset,offset + limit + 1);
        //
        for(Integer id:followeeId){
            Map<String,Object> map = new HashMap<>();
            User user = userService.findUserById(id);
            map.put("user",user);
            Double time = redisTemplate.opsForZSet().score(followeeKey,id);
            map.put("followeeTime",time.longValue());
            list.add(map);
        }
        return list;
    }

    // 查询某用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }
}

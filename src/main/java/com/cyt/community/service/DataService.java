package com.cyt.community.service;

import com.cyt.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
    @Autowired
    RedisTemplate redisTemplate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    //将访问ip存入uv
    public void recordUv(String ip){
        String uvKey = RedisKeyUtil.getUvkey(sdf.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey,ip);
    }
    //查询某时间范围内的uv
    public long caculateUv(Date start,Date end){
        if(start == null || end == null){
            throw  new IllegalArgumentException("查询时间为空");
        }

        List<String> keyList = new ArrayList<>();
        //
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String uvKey = RedisKeyUtil.getUvkey(sdf.format(calendar.getTime()));
            keyList.add(uvKey);
            calendar.add(Calendar.DATE,1);
        }

        String uvAllTime = RedisKeyUtil.getUvKey(sdf.format(start.getTime()),sdf.format(end.getTime()));

        redisTemplate.opsForHyperLogLog().union(uvAllTime,keyList.toArray());

        return redisTemplate.opsForHyperLogLog().size(uvAllTime);
    }
    // 将指定用户计入DAU
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDauKey(sdf.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    //查询某时间范围内的dau
    public long caculateDau(Date start,Date end){
        if(start == null || end == null){
            throw  new IllegalArgumentException("查询时间为空");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        List<byte[]> dauKeyList = new ArrayList<>();
        while(!calendar.getTime().after(end)){
            String dauKey = RedisKeyUtil.getDauKey(sdf.format(calendar.getTime()));
            dauKeyList.add(dauKey.getBytes());
            calendar.add(Calendar.DATE,1);
        }
        String allDauKey = RedisKeyUtil.getDauKey(sdf.format(start),sdf.format(end));

        return (long)redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR,
                        allDauKey.getBytes(),dauKeyList.toArray(new byte[0][0]));
                return redisConnection.bitCount(allDauKey.getBytes());
            }
        });
    }
}

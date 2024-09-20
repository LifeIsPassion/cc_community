package com.cyt.community.util;

public class RedisKeyUtil {
    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";
    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_FOLLOWER = "follower";
    private static final String PREFIX_KAPTCHA = "kaptcha";
    private static final String PREFIX_LOGINTICKET = "loginTicket";
    private static final String PREFIX_USER = "user";
    private static final String PREFIX_UV = "uv";
    private static final String PREFIX_DAU = "dau";
    private static final String PREFIX_POST = "post";

    //获取点赞对象的key
    public static String getEntityLikeKey(int entityType,int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }
    //获取用户点赞数的key
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    //获取某用户的关注列表key
    public static String getFolloweeKey(int userId,int entityType){
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }
    //获取关注者列表的key
    public static String getFollowerKey(int entityType, int entityId){
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    //获取缓存验证码的key
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    //获取登录凭证的key
    public static String getLoginTicketKey(String loginTicket){
        return PREFIX_LOGINTICKET + SPLIT + loginTicket;
    }

    //获取用户的key
    public static String getUserKey(int  userId){
        return PREFIX_USER + SPLIT + userId;
    }

    //获取特定时间的uvkey
    public static String getUvkey(String date){
        return PREFIX_UV + SPLIT + date;
    }

    //获取范围时间的uvkey
    public static String getUvKey(String startDate,String endDate){
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    //获取特定时间的daukey
    public static String getDauKey(String date){
        return PREFIX_DAU + SPLIT + date;
    }

    //获取范围时间daukey
    public static String getDauKey(String startDate,String endDate){
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    //获取更新帖子的分数key
    public static String getPostScore(){
        return PREFIX_POST + SPLIT + "score";
    }
}

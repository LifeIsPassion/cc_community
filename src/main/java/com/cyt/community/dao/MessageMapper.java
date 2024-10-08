package com.cyt.community.dao;

import com.cyt.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {
    //查询当前用户会话列表，并返回每个会话的最近消息
    List<Message> selectConversations(int userId,int offset,int limit);

    //当前用户的会话数量
    int selectConversationsCount(int userId);

    //某个会话所包含的私信列表
    List<Message> selectLetters(String conversation,int offset,int limit);

    //会话所包含的私信数量
    int selectLettersCount(String conversation);

    //查询未读私信数量
    int selectLettersUnreadCount(int userId,String conversation);

    //新增私信
    int insertMessage(Message message);

    //修改消息状态
    int updateStatus(List<Integer> ids,int status);

    // 查询某个主题下最新的通知
    Message selectLatestNotice(int userId, String topic);

    // 查询某个主题所包含的通知数量
    int selectNoticeCount(int userId, String topic);

    // 查询未读的通知的数量
    int selectNoticeUnreadCount(int userId, String topic);

    // 查询某个主题所包含的通知列表
    List<Message> selectNotices(int userId, String topic, int offset, int limit);

}

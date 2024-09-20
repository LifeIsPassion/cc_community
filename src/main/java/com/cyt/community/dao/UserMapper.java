package com.cyt.community.dao;

import com.cyt.community.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {


    public User getNameById(@Param("id") int id);

    public User getUserByName(@Param("username") String username);

    public User getUserByEmail(@Param("email") String email);

    int insertUser(User user);

    int updateStatus(int id, int status);

    int updateHeader(int id, String headerUrl);

    int updatePassword(int id, String password);
}

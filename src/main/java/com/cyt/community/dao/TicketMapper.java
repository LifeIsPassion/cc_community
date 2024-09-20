package com.cyt.community.dao;

import com.cyt.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated
public interface TicketMapper {

    @Insert({"insert into login_ticket(user_id,ticket,status,expired) values (#{userId},#{ticket},#{status},#{expired})"})
    @Options(useGeneratedKeys = true,keyProperty = "id")
    public void insertTicket(LoginTicket loginTicket);

    @Select({"select id,user_id,ticket,status,expired from login_ticket where ticket = #{ticket}"})
    public LoginTicket selectByTicket(String ticket);

    @Update({"update login_ticket set status = #{status} where ticket = #{ticket}"})
    int updateTicketStatus(String ticket,int status);

}

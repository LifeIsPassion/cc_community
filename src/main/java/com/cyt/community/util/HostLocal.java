package com.cyt.community.util;


import com.cyt.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，代替session对象
 *
 */
@Component
public class HostLocal {
    private ThreadLocal<User> user = new ThreadLocal<>();

    public void setUser(User user){
        this.user.set(user);
    }

    public User getUser(){
        return this.user.get();
    }

    public void removeUser(){
        this.user.remove();
    }
}

package com.cyt.community.controller.interceptor;


import com.cyt.community.entity.LoginTicket;
import com.cyt.community.entity.User;
import com.cyt.community.service.UserService;
import com.cyt.community.util.CookieUtil;
import com.cyt.community.util.HostLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;

    @Autowired
    HostLocal hostLocal;

    public static final Logger logger =  LoggerFactory.getLogger(LoginTicketInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("prehandle" + handler.toString());
        String ticket = CookieUtil.getValue(request,"loginticket");
        if(ticket != null){
            LoginTicket loginTicket = userService.getLoginTicket(ticket);
            if(loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())){
                User user = userService.findUserById(loginTicket.getUserId());
                hostLocal.setUser(user);
                //构造用户认证结果
                //userdetail password grantedauthority
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user,user.getPassword(), userService.getUserGranted(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostLocal.getUser();
        if(user != null && modelAndView != null){
            modelAndView.addObject("loginuser",user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostLocal.removeUser();
        SecurityContextHolder.clearContext();
    }
}

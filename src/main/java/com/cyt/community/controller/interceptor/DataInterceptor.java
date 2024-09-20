package com.cyt.community.controller.interceptor;

import com.cyt.community.entity.User;
import com.cyt.community.service.DataService;
import com.cyt.community.util.HostLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {
    @Autowired
    HostLocal hostLocal;

    @Autowired
    DataService dataService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //
        String ip = request.getRemoteHost();
        dataService.recordUv(ip);
        //如果用户已登录
        User user = hostLocal.getUser();
        if(user != null)
        dataService.recordDAU(user.getId());

        return true;
    }
}

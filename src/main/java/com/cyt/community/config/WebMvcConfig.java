package com.cyt.community.config;

import com.cyt.community.controller.interceptor.DataInterceptor;
import com.cyt.community.controller.interceptor.LoginRequirdInterceptor;
import com.cyt.community.controller.interceptor.LoginTicketInterceptor;
import com.cyt.community.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    LoginTicketInterceptor loginTicketInterceptor;

    @Autowired
    MessageInterceptor messageInterceptor;

    @Autowired
    DataInterceptor dataInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.jpg","/**/*.html","/**/*.img","/**/*.js");
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.jpg","/**/*.html","/**/*.img","/**/*.js");
        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.jpg","/**/*.html","/**/*.img","/**/*.js");
    }
}

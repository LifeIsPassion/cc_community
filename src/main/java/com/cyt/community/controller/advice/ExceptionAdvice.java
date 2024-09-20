package com.cyt.community.controller.advice;


import com.cyt.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);
    @ExceptionHandler({Exception.class})
    public void exceptionHandler(Exception e, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        logger.error("服务器发生错误"+e.getMessage());
        for(var msg:e.getStackTrace()){
            logger.error(msg.toString());
        }

        String xRequestWith = httpServletRequest.getHeader("x-requested-with");
        if("XMLHttpRequest".equals(xRequestWith)){
            httpServletResponse.setContentType("application/plain;charset=utf-8");
            PrintWriter printWriter = httpServletResponse.getWriter();
            printWriter.write(CommunityUtil.getJson(1,"服务器异常"));
        } else{
            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/error");
        }

    }

}

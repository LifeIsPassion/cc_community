package com.cyt.community.controller;


import com.cyt.community.entity.Test;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TestController {

    @RequestMapping("/test")
    public String testController(Test test){
        test.setName("CYT");
        return "/test";
    }
}

package com.cyt.community.controller;


import com.cyt.community.config.KaptConfig;
import com.cyt.community.entity.User;
import com.cyt.community.service.UserService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.cyt.community.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    UserService userService;

    @Autowired
    Producer kaptProduer;

    @Autowired
    RedisTemplate redisTemplate;

    @Value("server.servlet.context-path=/community")
    String path;



    @RequestMapping(path = "/register",method = RequestMethod.GET)
    public String getRegisterPage(){
        return "site/register";
    }

    @RequestMapping(path = "/login",method = RequestMethod.GET)
    public String getLoginPage(){
        return "site/login";
    }

    @RequestMapping(path = "/register",method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String,Object> map = userService.userRegister(user);
        if(map == null || map.isEmpty()){
            //注册成功
            model.addAttribute("target","/index");
            return "site/operate-result";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "site/register";
        }
    }
    //http://localhost/community/activation/userud/useractivationcode
    @RequestMapping(path = "/activation/{userid}/{useractivationcod}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userid") int userid,@PathVariable("useractivationcod") String code){
        int result = userService.activation(userid,code);
        if(result == ACTIVATION_SUCCESS){
            model.addAttribute("Msg","激活成功");
            model.addAttribute("target","/login");
        }else if(result == ACTIVATION_REPEAT){
            model.addAttribute("Msg","重复激活");
            model.addAttribute("target","/index");
        }else {
            model.addAttribute("Msg","激活失败");
            model.addAttribute("target","/index");
        }
        return "site/operate-result";
    }

    //
    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse httpServlet) throws IOException {
        String text = kaptProduer.createText();
        //把生成的uuid与验证码文本一起存放到redis中，键值即为uuid。生成cookie，把uuid放入，当我们登录的时候，获取cookie中的uuid，在把redis-value取出进行比较
        String owner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",owner);
        cookie.setMaxAge(60);
        cookie.setPath(path);
        httpServlet.addCookie(cookie);
        //将验证码保存入redis
        String ownerKey = RedisKeyUtil.getKaptchaKey(owner);
        redisTemplate.opsForValue().set(ownerKey, text,60, TimeUnit.SECONDS);
        //
        BufferedImage image = kaptProduer.createImage(text);
        //设置浏览器的接收值
        httpServlet.setContentType("image/png");
        //获取响应输出流
        OutputStream os = httpServlet.getOutputStream();
        //
        ImageIO.write(image,"png",os);
    }

    //
    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String login(String username,String password,String code,boolean rememberMe,Model model,HttpServletResponse httpServletResponse,@CookieValue("kaptchaOwner") String kaptchaOwner){
        //
        //String kaptcha = (String) httpSession.getAttribute("kaptcha");
        String kaptcha = null;
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
        }
        //判断输入信息是否为空
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","验证码错误");
            return "/site/login";
        }
        //
        if(StringUtils.isBlank(username)){
            model.addAttribute("usernameMsg","用户名为空");
            return "/site/login";
        }
        //
        if(StringUtils.isBlank(password)){
            model.addAttribute("passwordMsg","密码为空");
            return "site/login";
        }

        //
        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String,Object> map = userService.login(username,password,expiredSeconds);
        if(map.containsKey("loginticket")){
            Cookie cookie = new Cookie("loginticket",map.get("loginticket").toString());
            cookie.setPath(path);
            cookie.setMaxAge(expiredSeconds);
            httpServletResponse.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("password",map.get("passwordMsg"));
            model.addAttribute("status",map.get("statusMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout",method = RequestMethod.GET)
    public String logout(@CookieValue("loginticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }



}

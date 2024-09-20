package com.cyt.community.service;

import com.cyt.community.dao.TicketMapper;
import com.cyt.community.dao.UserMapper;
import com.cyt.community.entity.LoginTicket;
import com.cyt.community.entity.User;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.cyt.community.util.MailClient;
import com.cyt.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    UserMapper userMapper;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    MailClient mailClient;

    @Autowired
    RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    String domain;

    @Value("${server.servlet.context-path}")
    String path;

    @Autowired
    TicketMapper ticketMapper;

    public User findUserById(int id){
//        return userMapper.getNameById(id);
        User user = getUserByCache(id);
        if(user == null){
            user = initUserToCache(id);
        }
        return user;
    }
    public User findUserByName(String name){
        return userMapper.getUserByName(name);
    }

    /**
     * 返回一个带有出错情况的map
     *
     * @return
     */
    public Map<String,Object> userRegister(User user){
        Map<String,Object> map = new HashMap<>();

        //空值处理
        if(user == null) throw new IllegalArgumentException("参数为空");

        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","用户名不能为空");
            return map;
        }

        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","用户密码不能为空");
            return map;
        }

        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        //进行验证
        User userRegister = userMapper.getUserByName(user.getUsername());
        if(userRegister != null){
            map.put("usernameMsg","用户名已存在");
            return map;
        }
        userRegister = userMapper.getUserByEmail(user.getEmail());
        if(userRegister != null){
            map.put("emailMsg","该邮箱已被注册");
            return  map;
        }

        //注册
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.MD5(user.getPassword() + user.getSalt()));
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setType(ID_USER);
        user.setStatus(ACTIVATION_NO);
        user.setCreateTime(new Date());
        //
        userMapper.insertUser(user);
        //更新user信息，获取id字段值
        user = userMapper.getUserByName(user.getUsername());

        //发送激活邮箱

        //生成一个thymeleaf的上下文对象
        Context context = new Context();
        //设置值，注意变量的名字不能更改
        context.setVariable("email",user.getEmail());
        //http://localhost/community/activation/userud/useractivationcode
        String url = domain + path + "/activation/" + user.getId()+ "/" + user.getActivationCode();
        context.setVariable("url",url);
        //调用thymeleaf模板“mail/activation”处理context，将context中的值替换掉模板当中的值
        String content = templateEngine.process("mail/activation",context);
        //
        mailClient.sendMail(user.getEmail(),"激活账号",content);

        return map;
    }

    public Map<String,Object> login(String username,String password,int expiredSeconds){
        Map<String,Object> map = new HashMap<>();
        //判断空值
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg","用户名为空");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码为空");
        }
        //验证账号合法性

        //检查是否有该用户
        User user = userMapper.getUserByName(username);
        if(user == null) {
            map.put("usernameMsg", "没有该用户");
            return map;
        }
        //判断是否已被激活
        if(user.getStatus() == 0){
            map.put("statusMsg","账号未被激活");
            return map;
        }
        //验证密码
        password = CommunityUtil.MD5(password + user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg","密码错误");
            return map;
        }

        //
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        loginTicket.setStatus(0);
        //
        String longinTicketKey = RedisKeyUtil.getLoginTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(longinTicketKey,loginTicket);
        //
        //ticketMapper.insertTicket(loginTicket);
        map.put("loginticket",loginTicket.getTicket());
        return map;

    }

    public int activation(int id,String code){
        User user = userMapper.getNameById(id);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(id,1);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILUE;
        }
    }

    public void logout(String ticket){
        //ticketMapper.updateTicketStatus(ticket,1);
        String ticketKey = RedisKeyUtil.getLoginTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket)  redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
    }

    public LoginTicket getLoginTicket(String ticket){
        //return ticketMapper.selectByTicket(ticket);
        String ticketKey = RedisKeyUtil.getLoginTicketKey(ticket);
        return  (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    public void updateHeadUrl(int userId,String headUrl){
        userMapper.updateHeader(userId,headUrl);
        clearCache(userId);
    }

    //优先缓存取用户信息
    private User getUserByCache(int userId){
        String userIdKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userIdKey);
    }
    //初始化用户信息
    private User initUserToCache(int userId){
        User user = userMapper.getNameById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey,user,3600, TimeUnit.SECONDS);
        return user;
    }
    //数据变更时清理缓存
    private void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

    //获取用户权限
    public Collection<? extends GrantedAuthority> getUserGranted(int userId){
        User user = this.findUserById(userId);
        List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        grantedAuthorityList.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return grantedAuthorityList;
    }

}

package com.cyt.community.controller;


import com.cyt.community.annotation.LoginRequird;
import com.cyt.community.entity.User;
import com.cyt.community.service.FollowService;
import com.cyt.community.service.LikeService;
import com.cyt.community.service.UserService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.cyt.community.util.HostLocal;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller
@RequestMapping(path = "/user")
public class UserController implements CommunityConstant {

    @Value("${community.path.upload}")
    String headPath;

    @Value("${server.servlet.context-path}")
    String contextPath;

    @Value("${community.path.domain}")
    String domain;

    @Autowired
    UserService userService;

    @Autowired
    HostLocal hostLocal;

    @Autowired
    LikeService likeService;

    @Autowired
    FollowService followService;

    @Value("${qiniu.bucket.header.name}")
    String headerBucketName;

    @Value("${quniu.bucket.header.url}")
    String headerBucketUrl;

    @Value("${qiniu.key.access}")
    String accessKey;

    @Value("${qiniu.key.secret}")
    String secretKey;

    @LoginRequird
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSetting(Model model){
        String file = CommunityUtil.generateUUID();
        //
        StringMap stringMap = new StringMap();
        stringMap.put("returnBody",CommunityUtil.getJson(0));
        //
        Auth auth = Auth.create(accessKey,secretKey);
        String token = auth.uploadToken(headerBucketName,file,3600,stringMap);
        //
        model.addAttribute("uploadToken", token);
        model.addAttribute("fileName", file);

        return "/site/setting";
    }

    @RequestMapping(path = "/header/url",method = RequestMethod.POST)
    @ResponseBody
    public String updateHeadUrl(String fileName){
        if (StringUtils.isBlank(fileName)){
            return CommunityUtil.getJson(1,"文件名无法为空");
        }
        String headUrl = headerBucketUrl + "/" + fileName;
        userService.updateHeadUrl(hostLocal.getUser().getId(),headUrl);
        return CommunityUtil.getJson(0);
    }

    //上传本地
    //@LoginRequird
    //@RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile multipartFile, Model model) throws IOException {
        if (multipartFile == null){
            model.addAttribute("erro","还没有上传文件");
            return "site/setting";
        }

        String fileName = multipartFile.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        fileName = CommunityUtil.generateUUID() + suffix;
        if(StringUtils.isBlank(fileName)){
            model.addAttribute("erro","文件格式错误");
        }

        File file = new File(headPath + "/" + fileName);
        multipartFile.transferTo(file);
        //
        String headUrl = domain + contextPath + "/user/header/" + fileName;
        User user = hostLocal.getUser();
        userService.updateHeadUrl(user.getId(),headUrl);
        return "redirect:/index";
    }

    //@RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse httpServletResponse){
        //
        filename = headPath +"/"+filename;

        //获取文件后缀名
        String  suffix = filename.substring(filename.lastIndexOf("."));

        //设置浏览器接收格式
        httpServletResponse.setContentType("image/" + suffix);

        try (
            FileInputStream fileInputStream = new FileInputStream(filename);
            OutputStream outputStream = httpServletResponse.getOutputStream();
        ){
            byte[] b = new byte[1024];
            int a = 0;
            while((a = fileInputStream.read(b)) != -1) {
                outputStream.write(b, 0, a);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId,Model model){
        User user = userService.findUserById(userId);
        if(user == null){
             throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user",user);
        //查询获得了多少个赞
        int countLike = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",countLike);
        //查询followee数量,关注我的
        Long countFollowee = followService.findFolloweeCount(userId,ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",countFollowee);
        //查询follower数量，我关注的
        Long countFollower = followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        model.addAttribute("followerCount",countFollower);

        //查询关注状态
        boolean hasFollow;
        if(hostLocal.getUser() != null){
            hasFollow = followService.findFolloweeStatus(hostLocal.getUser().getId(),ENTITY_TYPE_USER,userId);
            model.addAttribute("hasFollowed",hasFollow);
        }


        return "/site/profile";
    }

}

package com.cyt.community.controller;

import com.cyt.community.entity.Event;
import com.cyt.community.event.EventProducer;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Controller
public class ShareController implements CommunityConstant {
    public static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Value("${community.path.domain}")
    String domain;

    @Value("${server.servlet.context-path}")
    String contextPath;

    @Value("${wk.image.storage}")
    String imageStorage;

    @Autowired
    EventProducer eventProducer;

    @Value("${qiniu.bucket.share.url}")
    String shareBucketUrl;




    @RequestMapping(path = "/share" ,method = RequestMethod.GET)
    @ResponseBody
    public String makeImages(String htmlUrl){
        //文件名
        String fileName = CommunityUtil.generateUUID();
        //异步获取长图
        Event event = new Event();
        event.setTopic(TOPIC_SHARE);
        event.setData("htmlUrl",htmlUrl);
        event.setData("fileName",fileName);
        event.setData("prefix",".png");
        eventProducer.fireEvent(event);
        //获取文件名
        Map<String,Object> map = new HashMap<>();
        //http://localhost:8080/community/share/fileName
        //map.put("shareUrl",domain + contextPath + "/share/image/" + fileName);
        map.put("shareUrl",shareBucketUrl + "/" + fileName);

        return CommunityUtil.getJson(0,"图片生成完毕",map);
    }

    //获取保存在本地的图片
    @RequestMapping(path = "/share/image/{filename}",method = RequestMethod.GET)
    public void getImage(@PathVariable(name = "filename") String filename, HttpServletResponse httpServletResponse){
        if(filename == null){
            throw new IllegalArgumentException("文件名不能为空");
        }
        //
        httpServletResponse.setContentType("image/png");
        File file = new File(imageStorage + "/" + filename + ".png" );
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            OutputStream outputStream = httpServletResponse.getOutputStream();
            byte[] bytes = new byte[1024];
            int b = 0;
            while((b = fileInputStream.read(bytes)) != -1){
                outputStream.write(bytes,0,b);
            }
        }catch (IOException ioException){
            logger.error("图片获取失败");
        }
    }

}

package com.cyt.community.event;

import com.alibaba.fastjson.JSONObject;
import com.cyt.community.entity.DiscussPost;
import com.cyt.community.entity.Event;
import com.cyt.community.entity.Message;
import com.cyt.community.service.DiscussPostService;
import com.cyt.community.service.ElasticsearchService;
import com.cyt.community.service.MessageService;
import com.cyt.community.util.CommunityConstant;
import com.cyt.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.servlet.http.PushBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Value("${wk.image.command}")
    String wkImageCommand;

    @Value("${wk.image.storage}")
    String wkImageStorage;

    @Value("${qiniu.bucket.share.url}")
    String shareBucketUrl;

    @Value("${qiniu.bucket.share.name}")
    String shareBucketName;

    @Value("${qiniu.key.access}")
    String accessKey;

    @Value("${qiniu.key.secret}")
    String secretKey;

    @Autowired
    MessageService messageService;

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    ElasticsearchService elasticsearchService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handleMessage(ConsumerRecord consumerRecord) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息为空");
            return;
        }
        //
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setCreateTime(new Date());
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setStatus(0);
        //
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        //
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord consumerRecord){
        if(consumerRecord == null && consumerRecord.value() == null){
            logger.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }

        DiscussPost discussPost = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }

    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord consumerRecord){
        if(consumerRecord == null && consumerRecord.value() == null){
            logger.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式错误");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    //处理分享
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShare(ConsumerRecord consumerRecord){
        if(consumerRecord == null && consumerRecord.value() == null){
            logger.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(),Event.class);
        if (event == null){
            logger.error("消息格式错误");
            return;
        }
        //获取data数据
        Map<String,Object> map = event.getData();
        String htmlUrl = (String) map.get("htmlUrl");
        String fileName = (String) map.get("fileName");
        String suffix = (String) map.get("prefix");

        //得到cmd
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("创建图片成功");
        } catch (IOException ioException) {
            logger.info("创建图片失败");
        }
        UploadTask uploadTask = new UploadTask(fileName,suffix);
        Future future = taskScheduler.scheduleAtFixedRate(uploadTask,500);
        uploadTask.setFuture(future);
    }

    class UploadTask implements Runnable{
        //文件名
        String fileName;
        //文件后缀
        String suffix;
        //任务开始时间
        long startime;
        //任务执行次数
        int times = 0;
        //当前任务的返回状态
        Future future;

        public UploadTask(String fileName,String suffix){
            this.fileName = fileName;
            this.suffix = suffix;
            startime = System.currentTimeMillis();
        }

        public void setFuture(Future future){
            this.future = future;
        }
        @Override
        public void run() {
            if(System.currentTimeMillis() - startime > 30000){
                future.cancel(true);
                logger.error("执行时间过长，终止任务 ："+fileName);
            }
            if(times > 3){
                future.cancel(true);
                logger.error("执行次数过多，终止任务 : "+fileName);
            }

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()){
                logger.info(String.format("开始第[%d]次上传,文件名[%s]",times,fileName));
                //响应形式
                StringMap stringMap = new StringMap();
                stringMap.put("returnBody", CommunityUtil.getJson(0));
                //生成上传凭证
                Auth auth = Auth.create(accessKey,secretKey);
                String token = auth.uploadToken(shareBucketName,fileName,3600,stringMap);
                //指定响应服务器
                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));

                try{
                    Response response = manager.put(
                            path, fileName, token, null, "image/" + suffix, false);
                    JSONObject responseJson = JSONObject.parseObject(response.bodyString());
                    if(responseJson == null || responseJson.get("code") == null || responseJson.get("code").equals("0")){
                        logger.info(String.format("第[%d]次上传[%s]失败",times,fileName));
                    }else{
                        logger.info(String.format("第[%d]次上传[%s]成功",times,fileName));
                        future.cancel(true);
                    }
                }
                catch (QiniuException e) {
                    e.printStackTrace();
                }
            }else{
                logger.info(String.format("请等待文件生成[%s]",fileName));
            }
        }
    }


}

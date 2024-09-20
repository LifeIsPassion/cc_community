package com.cyt.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class WkConfig {
    public static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    @Value("${wk.image.storage}")
    String imageStorage;

    @PostConstruct
    public void init(){
        File file = new File(imageStorage);
        if(!file.exists()){
            file.mkdir();
            logger.info("创建mk目录成功 ：" + imageStorage);
        }
    }

}

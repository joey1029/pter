package com.joey.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SpiderPterApplication extends WebMvcConfigurerAdapter {

    public static void main(String[] args) {
        SpringApplication.run(SpiderPterApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ   启动成功   ლ(´ڡ`ლ)ﾞ");
    }


}

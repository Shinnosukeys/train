package com.chovysun.train.pay.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@ComponentScan("com.chovysun")
@MapperScan("com.chovysun.train.*.mapper")
//@EnableFeignClients(basePackages = "com.chovysun.train.business.feign")
public class PayApplication {

    private static final Logger LOG =  LoggerFactory.getLogger(PayApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PayApplication.class);
        Environment env = app.run(args).getEnvironment();
        LOG.info("Member服务启动成功！！！");
        LOG.info("测试地址: \thttp://127.0.0.1:{}{}/hello", env.getProperty("server.port"), env.getProperty("server.servlet.context-path"));

    }
}

package com.chovysun.train.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class TestController {

    @Autowired
    Environment environment;

    @GetMapping("/hello")
    public String hello() {
        String port = environment.getProperty("local.server.port");
        return String.format("Hello! 端口：%s", port);
    }
}

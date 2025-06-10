package com.chovysun.train.business.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.chovysun.train.business.constant.constant.CONFIRM_ORDER_QUEUE;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue voucherOrderQueue() {
        return new Queue(CONFIRM_ORDER_QUEUE);
    }
}
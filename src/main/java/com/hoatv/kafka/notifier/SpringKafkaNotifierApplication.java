package com.hoatv.kafka.notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableAutoConfiguration
@ComponentScan({"com.hoatv.kafka.notifier", "com.hoatv.springboot.common"})
public class SpringKafkaNotifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaNotifierApplication.class, args);
    }
}

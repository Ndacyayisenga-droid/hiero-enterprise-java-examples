package com.hedera.tutorial;

import org.hiero.spring.EnableHiero;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableHiero
public class CreateTopicApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreateTopicApplication.class, args);
    }
}

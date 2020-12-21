package com.kalsym.flowcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class FlowcoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowcoreApplication.class, args);
    }

}

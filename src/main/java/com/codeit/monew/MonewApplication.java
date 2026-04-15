package com.codeit.monew;

import com.codeit.monew.global.config.AwsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AwsProperties.class)
public class MonewApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonewApplication.class, args);
    }

}

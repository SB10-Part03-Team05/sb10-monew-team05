package com.codeit.monew;

import com.codeit.monew.global.config.AwsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AwsProperties.class)
@EnableScheduling
public class MonewApplication {

  public static void main(String[] args) {
    SpringApplication.run(MonewApplication.class, args);
  }

}

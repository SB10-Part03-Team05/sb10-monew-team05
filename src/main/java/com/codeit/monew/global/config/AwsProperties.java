package com.codeit.monew.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "monew.aws")
@Getter
@Setter
public class AwsProperties {

    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private long presignedUrlExpiration;

}

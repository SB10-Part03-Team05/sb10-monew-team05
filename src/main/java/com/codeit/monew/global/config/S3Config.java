package com.codeit.monew.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

  private final AwsProperties awsProperties;

  public S3Config(AwsProperties awsProperties) {
    this.awsProperties = awsProperties;
  }

  @Bean
  public S3Client s3Client() {
    if (awsProperties.getAccessKey() != null
        && !awsProperties.getAccessKey().isBlank()) {
      return S3Client.builder()
          .region(Region.of(awsProperties.getRegion()))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(
                      awsProperties.getAccessKey(),
                      awsProperties.getSecretKey()
                  )
              )
          ).build();
    }

    return S3Client.builder()
        .region(Region.of(awsProperties.getRegion()))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    if (awsProperties.getAccessKey() != null
        && !awsProperties.getAccessKey().isBlank()) {
      return S3Presigner.builder()
          .region(Region.of(awsProperties.getRegion()))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(
                      awsProperties.getAccessKey(),
                      awsProperties.getSecretKey()
                  )
              )
          ).build();
    }

    return S3Presigner.builder()
        .region(Region.of(awsProperties.getRegion()))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }
}

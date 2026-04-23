package com.codeit.monew.infra.logging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "monew.logging.upload")
@Getter
@Setter
public class LogUploadProperties {

  private String cron;
  private String zone;

  private String logDir;
  private String filePrefix;
  private String s3Prefix;

  private int maxRetries;
  private long retryDelayMs;
}

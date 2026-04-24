package com.codeit.monew.global.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4); // 기본 스레드 수
    executor.setMaxPoolSize(16); // 최대 스레드 수
    executor.setQueueCapacity(500); // 대기열 큐 사이즈
    executor.setThreadNamePrefix("async-"); // 로그에 찍힐 스레드 이름
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    // 비동기 스레드에서 에러 시 로그
    return (ex, method, params) ->
        log.error("[ASYNC_ERROR] method={}, error={}", method.getName(), ex.getMessage(), ex);
  }
}

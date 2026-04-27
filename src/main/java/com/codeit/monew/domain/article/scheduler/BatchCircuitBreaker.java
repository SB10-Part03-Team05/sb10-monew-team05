package com.codeit.monew.domain.article.scheduler;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class BatchCircuitBreaker {

  private int consecutiveFailures = 0;
  private static final int MAX_ALLOWED = 10; // 10회 연속 실패 시 중단

  public void recordSuccess() {
    this.consecutiveFailures = 0;
  }

  public void recordFailure() {
    this.consecutiveFailures++;
  }

  public boolean isBroken() {
    return consecutiveFailures >= MAX_ALLOWED;
  }

}
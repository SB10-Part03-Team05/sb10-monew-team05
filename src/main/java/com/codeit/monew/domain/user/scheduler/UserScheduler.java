package com.codeit.monew.domain.user.scheduler;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Slf4j
public class UserScheduler {

  private final UserRepository userRepository;
  private final MeterRegistry meterRegistry;

  //크론 표현식으로 매일 0시 0분에 실행되도록 처리
  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  @Transactional
  public void cleanUpUser() {
    Timer.Sample sample = Timer.start(meterRegistry);
    String status = "success";
    int deletedCount = 0;

    try {
      Instant threshold = Instant.now().minus(Duration.ofDays(1));
      log.info("[USER_CLEAN_UP] 논리 삭제 후 1일 지난 사용자 삭제 스케줄러 동작: 기준 시간={}", threshold);
      List<User> targets = userRepository.findByDeletedAtBefore(threshold);
      deletedCount = targets.size();
      log.info("[USER_CLEAN_UP] 삭제 대상 사용자 수={}", targets.size());
      userRepository.deleteAllInBatch(targets);

      if (deletedCount > 0) {
        // 삭제된 사용자의 총 누적 합계를 기록
        meterRegistry.counter("scheduler.user.deleted.total").increment(deletedCount);
      }
    } catch (Exception e) {
      status = "fail";
      throw e;
    } finally {
      // 작업 횟수 및 결과 기록
      meterRegistry.counter("scheduler.user.job", "status", status).increment();

      // 작업 소요 시간 기록
      sample.stop(meterRegistry.timer("scheduler.user.job.time", "status", status));
      log.info("[USER_CLEAN_UP] 스케줄러 종료: 상태={}, 삭제건수={}", status, deletedCount);
    }
  }

}

package com.codeit.monew.domain.user.scheduler;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
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

  //크론 표현식으로 매일 0시 0분에 실행되도록 처리
  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cleanUpUser() {
    Instant threshold = Instant.now().minus(Duration.ofDays(1));
    log.info("[USER_CLEAN_UP] 논리 삭제 후 1일 지난 사용자 삭제 스케줄러 동작: 기준 사간={}", threshold);
    List<User> targets = userRepository.findByDeletedAtBefore(threshold);
    log.info("[USER_CLEAN_UP] 삭제 대상 사용자 수={}", targets.size());
    userRepository.deleteAllInBatch(targets);
    log.info("[USER_CLEAN_UP] 물리 삭제 배치 완료");
  }

}

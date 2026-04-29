package com.codeit.monew.domain.user.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSchedulerTest {

  @Mock
  private UserRepository userRepository;

  @Spy
  private MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @InjectMocks
  private UserScheduler userScheduler;

  @Test
  @DisplayName("논리 삭제된 지 24시간이 지난 유저는 물리 삭제되어야 한다")
  void should_hard_delete_users_when_soft_deleted_over_24_hours_ago() {
    // given
    User targetUser = new User("test@email.com", "testNickname", "testPassword1");
    targetUser.softDelete();
    List<User> targets = List.of(targetUser);
    given(userRepository.findByDeletedAtBefore(any(Instant.class))).willReturn(targets);

    // when
    userScheduler.cleanUpUser();

    // then
    verify(userRepository, times(1)).deleteAllInBatch(targets);
    double deletedTotalCount = meterRegistry.counter("scheduler.user.deleted.total").count();
    assertThat(deletedTotalCount).isEqualTo(1.0);
    double jobSuccessCount = meterRegistry.counter("scheduler.user.job", "status", "success").count();
    assertThat(jobSuccessCount).isEqualTo(1.0);
    double jobSuccessTime = meterRegistry.timer("scheduler.user.job.time", "status", "success").count();
    assertThat(jobSuccessTime).isNotEqualTo(0.0);
  }

}
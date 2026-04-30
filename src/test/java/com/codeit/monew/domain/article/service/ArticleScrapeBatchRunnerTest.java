package com.codeit.monew.domain.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.dto.response.ArticleScrapeBatchRunResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleScrapeBatchRunnerTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job articleScrapeBatchJob;

  @InjectMocks
  private ArticleScrapeBatchRunner articleScrapeBatchRunner;

  @Nested
  @DisplayName("배치 실행 (runNow)")
  class RunNow {

    @Test
    @DisplayName("완료 상태면 JobParameters와 함께 실행하고 응답을 반환한다")
    void run_success_with_job_parameters_and_response_mapping() throws Exception {
      // given: 테스트용 실행 시간 및 JobExecution 결과 설정
      LocalDateTime startTime = LocalDateTime.of(2026, 4, 30, 9, 1, 0);
      LocalDateTime endTime = LocalDateTime.of(2026, 4, 30, 9, 1, 3);

      JobExecution execution = new JobExecution(101L);
      execution.setStatus(BatchStatus.COMPLETED);
      execution.setStartTime(startTime);
      execution.setEndTime(endTime);

      // Step 데이터 설정 (정렬 검증을 위해 zStep, aStep 순서로 추가)
      StepExecution zStep = new StepExecution("zStep", execution);
      zStep.setStatus(BatchStatus.COMPLETED);
      zStep.setExitStatus(ExitStatus.COMPLETED);
      ReflectionTestUtils.setField(zStep, "commitCount", 5);
      ReflectionTestUtils.setField(zStep, "rollbackCount", 1);

      StepExecution aStep = new StepExecution("aStep", execution);
      aStep.setStatus(BatchStatus.COMPLETED);
      aStep.setExitStatus(ExitStatus.COMPLETED);
      ReflectionTestUtils.setField(aStep, "commitCount", 3);
      ReflectionTestUtils.setField(aStep, "rollbackCount", 2);

      execution.addStepExecutions(List.of(zStep, aStep));

      given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(execution);

      // when: 배치 실행 메서드 호출
      ArticleScrapeBatchRunResponse response = articleScrapeBatchRunner.runNow();

      // then: JobLauncher 호출 시 파라미터가 제대로 생성되었는지 검증
      ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
      verify(jobLauncher).run(eq(articleScrapeBatchJob), captor.capture());

      JobParameters params = captor.getValue();
      assertNotNull(params.getLong("manualTriggeredAt"));

      // then: 반환된 응답 DTO의 필드 매핑 검증
      assertEquals(101L, response.jobExecutionId());
      assertEquals("COMPLETED", response.status());
      assertEquals(startTime, response.startTime());
      assertEquals(endTime, response.endTime());

      // then: Step 결과 리스트가 이름 순(aStep -> zStep)으로 정렬 및 필드 매핑되었는지 검증
      assertEquals(2, response.steps().size());
      assertEquals("aStep", response.steps().get(0).stepName());
      assertEquals(3, response.steps().get(0).commitCount());

      assertEquals("zStep", response.steps().get(1).stepName());
      assertEquals(5, response.steps().get(1).commitCount());
    }

    @Test
    @DisplayName("JobExecution 상태가 FAILED이면 JobExecutionException을 던진다")
    void throw_when_job_status_failed() throws Exception {
      // given: FAILED 상태의 JobExecution 결과 설정
      JobExecution execution = new JobExecution(201L);
      execution.setStatus(BatchStatus.FAILED);
      given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(execution);

      // when & then: 예외 발생 여부 검증
      assertThrows(JobExecutionException.class, () -> articleScrapeBatchRunner.runNow());
    }

    @Test
    @DisplayName("JobExecution 상태가 STOPPED이면 JobExecutionException을 던진다")
    void throw_when_job_status_stopped() throws Exception {
      // given: STOPPED 상태의 JobExecution 결과 설정
      JobExecution execution = new JobExecution(202L);
      execution.setStatus(BatchStatus.STOPPED);
      given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(execution);

      // when & then: 예외 발생 여부 검증
      assertThrows(JobExecutionException.class, () -> articleScrapeBatchRunner.runNow());
    }

    @Test
    @DisplayName("JobLauncher 예외를 그대로 전파한다")
    void propagate_when_job_launcher_throws_exception() throws Exception {
      // given: JobLauncher에서 발생할 특정 예외 설정
      JobParametersInvalidException expected = new JobParametersInvalidException("launcher failed");
      given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willThrow(expected);

      // when: 예외 발생 시점 검증
      JobParametersInvalidException actual = assertThrows(
          JobParametersInvalidException.class,
          () -> articleScrapeBatchRunner.runNow()
      );

      // then: 메시지 일치 여부 확인
      assertEquals(expected.getMessage(), actual.getMessage());
    }
  }
}
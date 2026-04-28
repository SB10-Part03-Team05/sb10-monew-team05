package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 기사 스크래핑 배치 흐름(Job Flow) 통합 테스트 실제 Spring Batch 인프라를 활용하여 각 Step 간의 조건부 실행 로직을 검증합니다.
 */
@SpringJUnitConfig(classes = {
    ArticleScrapeBatchConfig.class, // 테스트 대상 배치 설정
    ArticleScrapeBatchConfigTest.TestInfraConfig.class // 테스트용 인프라 설정
})
class ArticleScrapeBatchConfigTest {

  @Configuration
  @EnableBatchProcessing
  static class TestInfraConfig {

    // 배치의 상태 정보(메타데이터)를 저장할 테스트용 인메모리 DB
    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()
          .setType(EmbeddedDatabaseType.H2)
          .addScript("classpath:org/springframework/batch/core/schema-h2.sql")
          .build();
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }

    // 실제 Batch 프레임워크가 Step의 성공/실패 여부를 기록할 리포지토리
    @Bean
    JobRepository jobRepository(DataSource dataSource,
        PlatformTransactionManager transactionManager) throws Exception {
      JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
      factory.setDataSource(dataSource);
      factory.setTransactionManager(transactionManager);
      factory.afterPropertiesSet();
      return factory.getObject();
    }

    // 배치를 실제로 구동시킬 런처
    @Bean
    JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
      TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
      launcher.setJobRepository(jobRepository);
      launcher.afterPropertiesSet();
      return launcher;
    }

    // 실제 API 연동을 하지 않도록 Tasklet들을 Mock 객체로 생성
    @Bean
    RssArticleScrapeTasklet rssArticleScrapeTasklet() {
      return mock(RssArticleScrapeTasklet.class);
    }

    @Bean
    NaverArticleScrapeTasklet naverArticleScrapeTasklet() {
      return mock(NaverArticleScrapeTasklet.class);
    }

    @Bean
    ArticleScrapeNotificationTasklet articleScrapeNotificationTasklet() {
      return mock(ArticleScrapeNotificationTasklet.class);
    }
  }

  @jakarta.annotation.Resource
  private JobLauncher jobLauncher;

  @jakarta.annotation.Resource(name = "articleScrapeBatchJob")
  private Job job;

  @jakarta.annotation.Resource
  private RssArticleScrapeTasklet rssArticleScrapeTasklet;

  @jakarta.annotation.Resource
  private NaverArticleScrapeTasklet naverArticleScrapeTasklet;

  @jakarta.annotation.Resource
  private ArticleScrapeNotificationTasklet articleScrapeNotificationTasklet;

  @BeforeEach
  void setUp() throws Exception {
    // 테스트마다 Mock 상태를 초기화하여 격리 보장
    reset(rssArticleScrapeTasklet, naverArticleScrapeTasklet, articleScrapeNotificationTasklet);

    // 기본값으로 모든 단계가 성공하도록 설정
    when(rssArticleScrapeTasklet.execute(any(), any())).thenReturn(RepeatStatus.FINISHED);
    when(naverArticleScrapeTasklet.execute(any(), any())).thenReturn(RepeatStatus.FINISHED);
    when(articleScrapeNotificationTasklet.execute(any(), any())).thenReturn(RepeatStatus.FINISHED);
  }

  @Test
  @DisplayName("RSS 수집 실패 시, 네이버 단계는 건너뛰고 알림 단계로 바로 진입한다")
  void notification_runs_when_rss_failed() throws Exception {
    // Given: 첫 번째 단계인 RSS 스크래핑에서 예외가 발생하는 상황
    when(rssArticleScrapeTasklet.execute(any(), any()))
        .thenThrow(new RuntimeException("rss fail"));

    // When: 배치를 실행하여 전체 흐름을 가동
    JobExecution execution = runJob();
    List<String> stepNames = stepNames(execution);

    // Then: 조건부 흐름(Flow) 제어 검증
    assertTrue(stepNames.contains("rssStep"), "RSS 단계는 실행되어야 함");
    assertFalse(stepNames.contains("naverStep"), "RSS 실패 시 네이버 단계는 건너뛰어야 함");
    assertTrue(stepNames.contains("notificationStep"), "장애가 발생해도 알림 단계는 반드시 실행되어야 함");

    // 알림 서비스가 실제로 호출되었는지 검증
    verify(articleScrapeNotificationTasklet, times(1)).execute(any(), any());
  }

  @Test
  @DisplayName("네이버 수집 실패 시에도, 최종 알림 단계는 정상적으로 실행된다")
  void notification_runs_when_naver_failed() throws Exception {
    // Given: RSS는 성공하지만 네이버 스크래핑에서 예외가 발생하는 상황
    when(naverArticleScrapeTasklet.execute(any(), any()))
        .thenThrow(new RuntimeException("naver fail"));

    // When: 배치를 실행
    JobExecution execution = runJob();
    List<String> stepNames = stepNames(execution);

    // Then: 장애 전파 및 후속 조치 검증
    assertTrue(stepNames.contains("rssStep"), "이전 단계인 RSS는 성공적으로 실행되어야 함");
    assertTrue(stepNames.contains("naverStep"), "네이버 단계는 실행되었으나 실패로 기록되어야 함");
    assertTrue(stepNames.contains("notificationStep"), "네이버 수집 실패와 무관하게 알림 단계는 실행되어야 함");

    // 최종 결과 요약 알림이 호출되었는지 검증
    verify(articleScrapeNotificationTasklet, times(1)).execute(any(), any());
  }

  /**
   * 중복 실행 방지를 위해 고유한 타임스탬프 파라미터와 함께 Job을 구동하는 헬퍼 메서드
   */
  private JobExecution runJob() throws Exception {
    return jobLauncher.run(
        job,
        new JobParametersBuilder()
            .addLong("ts", System.nanoTime())
            .toJobParameters()
    );
  }

  /**
   * 실행된 Job 기록으로부터 실제 수행된 Step 이름들을 리스트로 추출
   */
  private List<String> stepNames(JobExecution execution) {
    return execution.getStepExecutions().stream()
        .map(StepExecution::getStepName)
        .collect(Collectors.toList());
  }
}
package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.dto.response.ArticleScrapeBatchRunResponse;
import com.codeit.monew.domain.article.dto.response.ArticleScrapeBatchRunResponse.StepResult;
import com.codeit.monew.domain.article.scheduler.ArticleScrapeBatchConfig;
import java.util.Comparator;
import java.util.List;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ArticleScrapeBatchRunner {

  private final JobLauncher jobLauncher;
  private final Job articleScrapeBatchJob;

  public ArticleScrapeBatchRunner(
      JobLauncher jobLauncher,
      @Qualifier(ArticleScrapeBatchConfig.JOB_NAME) Job articleScrapeBatchJob
  ) {
    this.jobLauncher = jobLauncher;
    this.articleScrapeBatchJob = articleScrapeBatchJob;
  }

  public ArticleScrapeBatchRunResponse runNow() throws JobExecutionException {
    JobParameters params = new JobParametersBuilder()
        .addLong("manualTriggeredAt", System.currentTimeMillis())
        .toJobParameters();

    JobExecution execution = jobLauncher.run(articleScrapeBatchJob, params);
    if (execution.getStatus() == BatchStatus.FAILED || execution.getStatus() == BatchStatus.STOPPED) {
      throw new JobExecutionException(
          "Article scrape batch failed. jobExecutionId=" + execution.getId()
              + ", status=" + execution.getStatus()
      );
    }
    return toResponse(execution);
  }

  private ArticleScrapeBatchRunResponse toResponse(JobExecution execution) {
    List<StepResult> steps = execution.getStepExecutions().stream()
        .sorted(Comparator.comparing(StepExecution::getStepName))
        .map(step -> new StepResult(
            step.getStepName(),
            step.getStatus().name(),
            step.getExitStatus().getExitCode(),
            step.getCommitCount(),
            step.getRollbackCount()
        ))
        .toList();

    return new ArticleScrapeBatchRunResponse(
        execution.getId(),
        execution.getStatus().name(),
        execution.getStartTime(),
        execution.getEndTime(),
        steps
    );
  }
}

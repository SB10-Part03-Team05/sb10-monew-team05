package com.codeit.monew.global.exception.article;

import com.codeit.monew.global.exception.ErrorCode;
import java.time.LocalDate;

public class ArticleBackupBatchRunFailed extends ArticleException {

  public ArticleBackupBatchRunFailed(LocalDate backupDate, Throwable cause) {
    super(ErrorCode.ARTICLE_BACKUP_BATCH_RUN_FAILED, "backupDate", backupDate);
  }
}

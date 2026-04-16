package com.codeit.monew.domain.article.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode // 복합키 비교를 위해 필요
@NoArgsConstructor
@AllArgsConstructor
public class ArticleInterestId implements Serializable {

  private UUID article;  // ArticleInterest의 필드명과 일치해야 함
  private UUID interest; // ArticleInterest의 필드명과 일치해야 함
}
package com.codeit.monew.domain.article.entity;

import com.codeit.monew.domain.interest.entity.Interest;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@IdClass(ArticleInterestId.class) // 복합키 식별자 클래스 필요
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleInterest {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id")
  private Article article;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id")
  private Interest interest;

  public static ArticleInterest create(Article article, Interest interest) {
    ArticleInterest mapping = new ArticleInterest();
    mapping.article = article;
    mapping.interest = interest;
    return mapping;
  }
}

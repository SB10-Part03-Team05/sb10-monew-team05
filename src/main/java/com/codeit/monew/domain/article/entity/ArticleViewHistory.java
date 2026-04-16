package com.codeit.monew.domain.article.entity;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.common.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "article_view_histories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "article_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleViewHistory extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id")
  private Article article;
}

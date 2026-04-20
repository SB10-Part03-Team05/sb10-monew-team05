package com.codeit.monew.domain.interest.entity;

import com.codeit.monew.global.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "keywords",
    uniqueConstraints = @UniqueConstraint(columnNames = {"interest_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  @Column(nullable = false, length = 20)
  private String name;

  public static Keyword create(Interest interest, String name) {
    Keyword keyword = new Keyword();
    keyword.interest = interest;
    keyword.name = name;
    return keyword;
  }
}

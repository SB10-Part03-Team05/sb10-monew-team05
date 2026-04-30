package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수집된 기사 데이터를 영속화하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class ArticleScrapePersistenceService {

  private final ArticleRepository articleRepository;
  private final EntityManager entityManager;

  /**
   * 수집된 기사와 매핑된 관심사 목록을 일괄 저장합니다.
   *
   * @param articleInterestMap 기사 엔티티와 관심사 세트의 매핑 정보
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveAll(Map<Article, Set<Interest>> articleInterestMap) {
    if (articleInterestMap == null || articleInterestMap.isEmpty()) {
      return;
    }

    List<Article> toSave = new ArrayList<>();

    for (Map.Entry<Article, Set<Interest>> entry : articleInterestMap.entrySet()) {
      // 1. 유효성 검사 (Guard Clause)
      if (!isValidEntry(entry)) {
        continue;
      }

      Article article = entry.getKey();
      Set<Interest> interests = entry.getValue();

      // 2. 연관관계 설정 (Mutation)
      linkArticleWithInterests(article, interests);

      // 3. 이번 처리 과정에서 관심사가 정상적으로 연결된 기사만 저장 대상으로 선정
      if (!article.getArticleInterests().isEmpty()) {
        toSave.add(article);
      }
    }

    // 4. 일괄 저장
    if (!toSave.isEmpty()) {
      articleRepository.saveAll(toSave);
    }
  }

  /**
   * 기사와 관심사 목록 간의 연관관계를 설정합니다.
   */
  private void linkArticleWithInterests(Article article, Set<Interest> interests) {
    interests.stream()
        .filter(interest -> interest != null && interest.getId() != null)
        .map(Interest::getId)
        .distinct()
        .map(id -> entityManager.getReference(Interest.class, id))
        .forEach(article::addInterest);
  }

  private boolean isValidEntry(Map.Entry<Article, Set<Interest>> entry) {
    return entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty();
  }
}
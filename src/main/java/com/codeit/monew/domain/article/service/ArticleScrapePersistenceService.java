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

@Service
@RequiredArgsConstructor
public class ArticleScrapePersistenceService {

  private final ArticleRepository articleRepository;
  private final EntityManager entityManager;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveAll(Map<Article, Set<Interest>> articleInterestMap) {
    List<Article> toSave = new ArrayList<>(articleInterestMap.keySet());
    for (Map.Entry<Article, Set<Interest>> entry : articleInterestMap.entrySet()) {
      Article article = entry.getKey();
      for (Interest interest : entry.getValue()) {
        if (interest == null || interest.getId() == null) {
          continue;
        }
        Interest managedReference = entityManager.getReference(Interest.class, interest.getId());
        article.addInterest(managedReference);
      }
    }
    articleRepository.saveAll(toSave);
  }
}

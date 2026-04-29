package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleScrapePersistenceService {

  private final ArticleRepository articleRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveAll(List<Article> articles) {
    articleRepository.saveAll(articles);
  }
}

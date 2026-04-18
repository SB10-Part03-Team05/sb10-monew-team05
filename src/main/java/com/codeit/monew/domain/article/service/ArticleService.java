package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.mapper.ArticleMapper;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.article.repository.ArticleViewHistoryRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.article.ArticleNotFoundException;
import com.codeit.monew.global.exception.interest.InterestNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleService {

  private final ArticleRepository articleRepository;
  private final ArticleViewHistoryRepository articleViewHistoryRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final InterestRepository interestRepository;
  private final ArticleMapper articleMapper;

  // 뉴스 기사 단건 조회
  @Transactional(readOnly = true)
  public ArticleDto getArticle(UUID articleId, UUID requestUserId) {
    log.debug("[ARTICLE_FIND] 뉴스 기사 조회 시작: articleId={}", articleId);

    // 사용자 존재 검증
    userRepository.findByIdAndDeletedAtIsNull(requestUserId)
        .orElseThrow(() -> new UserNotFoundException(requestUserId));

    Article article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    // 댓글 수
    long commentCount = commentRepository.countByArticleIdAndDeletedAtIsNull(articleId);
    // 조회 수
    long viewCount = articleViewHistoryRepository.countByArticleId(articleId);
    // 요청자 조회 여부
    boolean viewedByMe = articleViewHistoryRepository.existsByArticleIdAndUserId(articleId,
        requestUserId);

    log.debug("[ARTICLE_FIND] 뉴스 기사 조회 성공: articleId={}, title={}, publishDate={}, createdAt={}",
        articleId, article.getTitle(), article.getPublishDate(), article.getCreatedAt());

    return articleMapper.toDto(article, commentCount, viewCount, viewedByMe);
  }

  // 뉴스 기사 출처 목록 조회
  @Transactional(readOnly = true)
  public List<ArticleSource> getSources() {
    return articleRepository.findDistinctSource();
  }

  // 뉴스 기사 목록 조회(커서 페이지네이션)
  @Transactional(readOnly = true)
  public CursorPageResponseArticleDto search(ArticleSearchRequest request, UUID requestUserId) {
    log.debug(
        "[ARTICLE_LIST_FIND] 뉴스 기사 목록 조회 시작: keyword={}, sourceIn={}, publishDateFrom={}, publishDateTo={}, orderBy={}, direction={}, cursor={}, after={}, limit={}",
        request.getKeyword(), request.getSourceIn(), request.getPublishDateFrom(),
        request.getPublishDateTo(), request.getOrderBy(), request.getDirection(),
        request.getCursor(), request.getAfter(), request.getLimit());

    // 사용자 존재 검증
    userRepository.findByIdAndDeletedAtIsNull(requestUserId)
        .orElseThrow(() -> new UserNotFoundException(requestUserId));

    // 관심사 존재 검증
    if (request.getInterestId() != null) {
      interestRepository.findById(request.getInterestId())
          .orElseThrow(() -> new InterestNotFoundException(request.getInterestId()));
    }

    return articleRepository.searchArticleList(request, requestUserId);
  }
}

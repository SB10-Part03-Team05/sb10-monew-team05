package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.dto.response.ArticleViewDto;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleViewHistory;
import com.codeit.monew.domain.article.mapper.ArticleMapper;
import com.codeit.monew.domain.article.mapper.ArticleViewMapper;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.article.repository.ArticleViewHistoryRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.domain.useractivity.event.ArticleViewedEvent;
import com.codeit.monew.global.exception.Interest.InterestNotFoundException;
import com.codeit.monew.global.exception.article.ArticleNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final ArticleViewMapper articleViewMapper;
  private final ApplicationEventPublisher eventPublisher;

  // 뉴스 기사 단건 조회
  @Transactional(readOnly = true)
  public ArticleDto getArticle(UUID articleId, UUID requestUserId) {
    log.debug("[ARTICLE_FIND] 뉴스 기사 조회 시작: articleId={}", articleId);

    // 사용자 존재 검증
    userRepository.findByIdAndDeletedAtIsNull(requestUserId)
        .orElseThrow(() -> new UserNotFoundException(requestUserId));

    // 뉴스 기사 존재 검증
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

    CursorPageResponseArticleDto responseArticleDto = articleRepository.searchArticleList(request,
        requestUserId);

    log.debug(
        "[ARTICLE_LIST_FIND] 뉴스 기사 목록 조회 성공: contentSize={}, size={}, totalElement={}, hasNext={}, nextCursor={}, nextAfter={}",
        responseArticleDto.content().size(), responseArticleDto.size(),
        responseArticleDto.totalElements(), responseArticleDto.hasNext(),
        responseArticleDto.nextCursor(), responseArticleDto.nextAfter());

    return responseArticleDto;
  }

  // 뉴스 기사 view 등록
  public ArticleViewDto view(UUID articleId, UUID requestUserId) {
    log.debug("[ARTICLE_VIEW_POST] 뉴스 기사 조회 처리 시작: articleId={}", articleId);

    // 사용자 존재 검증
    User user = userRepository.findByIdAndDeletedAtIsNull(requestUserId)
        .orElseThrow(() -> new UserNotFoundException(requestUserId));

    // 뉴스 기사 존재 검증
    Article article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    // 뉴스 기사 view 조회 후 없으면 null, 있으면 해당 뉴스 기사 view 정보 반환
    ArticleViewHistory articleViewHistory = articleViewHistoryRepository
        .findByArticleIdAndUserId(articleId, requestUserId).orElse(null);

    // view가 없다면 새로 생성
    if (articleViewHistory == null) {
      try {
        // `save` 만 사용하면 JPA가 바로 `INSERT` 하지 않고, `flush`/`commit` 시점에 SQL을 보낼 수 있음
        // 이 경우 `DataIntegrityViolationException` 가 `save` 에서 발생하지 않고, 트랜잭션 `commit` 시점에 발생 가능
        // 작성한 `try-catch` 가 제대로 동작 하지 않게됨
        // 그래서 `saveAndFlush`를 사용해 영속성 컨텍스트에 쌓여있는 SQL을 DB로 보냄 (단, `flush` != `commit` 이 아님. 트랜잭션 롤백 시 취소됨)
        articleViewHistory = articleViewHistoryRepository
            .saveAndFlush(new ArticleViewHistory(user, article));
      } catch (DataIntegrityViolationException e) {
        // 같은(또는 다른) 사용자가 짧은 시간 내에 2번 연속 클릭 등으로 동시성 문제가 발생할 경우
        // `DataIntegrityViolationException` 문제 발생
        // 지금의 로직에서는 해당 예외가 발생할 경우 다시 조회해서 기존 이력 반환
        articleViewHistory = articleViewHistoryRepository
            .findByArticleIdAndUserId(articleId, requestUserId)
            .orElseThrow(() -> e);
      }
    }

    // 댓글 수
    long commentCount = commentRepository.countByArticleIdAndDeletedAtIsNull(articleId);
    // 조회 수
    long viewCount = articleViewHistoryRepository.countByArticleId(articleId);

    log.info("[ARTICLE_VIEW_POST] 뉴스 기사 조회 처리 성공: id={}, viewedBy={}, createdAt={}, articleId={}",
        articleViewHistory.getId(), user.getId(), articleViewHistory.getCreatedAt(),
        article.getId());

    // 활동 내역 기사 조회 정보 갱신 로직
    eventPublisher.publishEvent(new ArticleViewedEvent(
        articleId,
        user.getId(),
        Instant.now(),
        article.getSource(),
        article.getSourceUrl(),
        article.getTitle(),
        article.getPublishDate(),
        article.getSummary(),
        commentCount,
        viewCount
    ));

    return articleViewMapper.toDto(articleViewHistory, article, user, commentCount, viewCount);
  }

  // 뉴스 기사 논리 삭제
  public void delete(UUID articleId) {
    log.debug("[ARTICLE_SOFT_DELETE] 뉴스 기사 논리 삭제 시작: articleId={}", articleId);

    // 뉴스 기사 존재 검증
    Article article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    // 논리 삭제 (`@SQLDelete`로 인해 `delete` 사용 시 `deletedAt` 이 업데이트 됨)
    articleRepository.delete(article);

    log.info("[ARTICLE_SOFT_DELETE] 뉴스 기사 논리 삭제 완료: articleId={}", articleId);
  }

  // 뉴스 기사 물리 삭제
  public void hardDelete(UUID articleId) {
    log.debug("[ARTICLE_HARD_DELETE] 뉴스 기사 물리 삭제 시작: articleId={}", articleId);

    // `@SQLDelete`로 인해 `delete` 사용 시 삭제가 아닌 `deletedAt` 이 업데이트 됨
    // `@SQLRestriction`로 조회 시 deletedAt이 null인 데이터만 가져오도록 필터링하기 때문에
    // `void` 가 아닌 `int` 로 값을 반환해 id가 존재하는 기사인지 검증도 같이함
    int deletedCount = articleRepository.hardDelete(articleId);
    if (deletedCount == 0) { // 뉴스 기사 존재 검증
      throw new ArticleNotFoundException(articleId);
    }

    log.info("[ARTICLE_HARD_DELETE] 뉴스 기사 물리 삭제 완료: articleId={}", articleId);
  }
}

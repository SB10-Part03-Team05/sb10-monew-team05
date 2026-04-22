package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentCursorRequest;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.mapper.CommentMapper;
import com.codeit.monew.domain.comment.repository.CommentLikeRepository;
import com.codeit.monew.domain.comment.repository.CommentQueryRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.event.CommentCreatedEvent;
import com.codeit.monew.global.event.CommentUpdatedEvent;
import com.codeit.monew.global.exception.article.ArticleNotFoundException;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final CommentQueryRepository commentQueryRepository;
  private final CommentMapper commentMapper;
  private final ApplicationEventPublisher eventPublisher;

  // 댓글 등록
  @Transactional
  public CommentDto registerComment(UUID articleId, UUID userId, String content) {
    log.debug("[COMMENT_CREATE] 댓글 등록 요청: articleId={}, userId={}", articleId, userId);

    // 1. 유저 조회 및 검증
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 2. 기사 조회 및 검증
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    // 3. 댓글 엔티티 생성 및 영속화
    Comment comment = new Comment(article, user, content);
    Comment savedComment = commentRepository.save(comment);
    log.info("[COMMENT_POST] 댓글 등록 완료: commentId= {}, articleId= {}", savedComment.getId(), articleId);

    // 4. DTO로 변환
    boolean likedByMe = false; // 댓글 등록 시점에는 좋아요가 없으므로 false로 초기화

    // 활동 내역 댓글 정보 갱신 로직
    eventPublisher.publishEvent(new CommentCreatedEvent(
        comment.getId(),
        article.getTitle(),
        userId,
        user.getNickname(),
        content,
        comment.getLikeCount(),
        comment.getCreatedAt() != null ? comment.getCreatedAt() : Instant.now()
    ));

    return commentMapper.toDto(savedComment, user.getNickname(), likedByMe);
  }

  // 댓글 수정
  @Transactional
  public CommentDto updateComment(UUID commentId, UUID requesterId, String content) {
    log.debug("[COMMENT_UPDATE] 댓글 수정 요청: commentId={}, requesterId={}", commentId, requesterId);

    // 1. 댓글 및 작성자 정보 한 번에 조회
    Comment comment = commentRepository.findByIdWithUser(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // 2. 엔티티 내부에서 권한 검증 및 수정 수행
    comment.updateContent(content, requesterId);
    log.info("[COMMENT_UPDATE] 댓글 수정 완료: commentId= {}, requesterId= {}", commentId, requesterId);

    // 3. DTO 변환
    boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, requesterId);

    // 활동 내역 댓글 수정 정보 갱신 로직
    eventPublisher.publishEvent(new CommentUpdatedEvent(
        requesterId,
        commentId,
        content
    ));

    return commentMapper.toDto(comment, comment.getUser().getNickname(), likedByMe);
  }

  // 댓글 논리 삭제 (삭제 기본값)
  @Transactional
  public void deleteComment(UUID commentId) {
    log.debug("[COMMENT_DELETE] 댓글 논리 삭제 요청: commentId={}", commentId);

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // 엔티티에 설정된 @SQLDelete 작동 -> deleted_at에 현재 시간 기록
    commentRepository.delete(comment);
    log.info("[COMMENT_DELETE] 댓글 논리 삭제 완료: commentId= {}", commentId);
  }

  // 댓글 물리 삭제
  @Transactional
  public void hardDeleteComment(UUID commentId) {
    log.debug("[COMMENT_DELETE] 댓글 물리 삭제 요청: commentId={}", commentId);

    // 1. 댓글 존재 여부 확인
    boolean exists = commentRepository.existsById(commentId);
    if (!exists) {
      throw new CommentNotFoundException(commentId);
    }

    // 2. 연관 데이터 물리 삭제
    commentLikeRepository.deleteByCommentId(commentId);

    // 3. 댓글 물리 삭제
    commentRepository.deleteByIdHard(commentId);
    log.info("[COMMENT_DELETE] 댓글 물리 삭제 완료: commentId= {}", commentId);
  }

  // 댓글 목록 조회
  @Transactional(readOnly = true)
  public CursorPageResponseCommentDto getCommentList(UUID articleId, UUID requesterId, CommentCursorRequest request) {

    log.debug("[COMMENT_READ] 댓글 목록 조회 요청: articleId={}, requesterId={}, orderBy={}, cursor={}",
        articleId, requesterId, request.orderBy(), request.cursor());

    // 1. 기사 존재 여부 검증
    if (!articleRepository.existsById(articleId)) {
      throw new ArticleNotFoundException(articleId);
    }

    // 2. 커서 기반으로 댓글 엔티티 목록 조회
    List<Comment> comments = commentQueryRepository.findCommentsByCursor(articleId, request);

    // 3. 다음 페이지 존재 여부(hasNext) 판별 및 초과 데이터 자르기
    boolean hasNext = comments.size() > request.limit();
    if (hasNext) {
      comments.remove(comments.size() - 1);
    }

    // 4. 조회된 댓글 ID 추출 및 좋아요 여부 한 번에 가져오기
    Set<UUID> likedCommentIds = new HashSet<>();
    if (!comments.isEmpty()) {
      List<UUID> commentIds = comments.stream().map(Comment::getId).toList();

      // 해당 유저가 좋아요를 누른 댓글 ID만 한 번에 가져와서 Set(O(1) 조회 속도)에 담기
      List<UUID> likedList = commentLikeRepository.findLikedCommentIdsByUserAndComments(requesterId, commentIds);
      likedCommentIds.addAll(likedList);
    }

    // 5. DTO 변환
    List<CommentDto> content = comments.stream()
        .map(comment -> {
          boolean likedByMe = likedCommentIds.contains(comment.getId()); // Set에서 조회하므로 빠르게 매핑 가능
          return commentMapper.toDto(comment, comment.getUser().getNickname(), likedByMe);
        }).toList();

    // 6. 다음 페이지를 위한 커서 계산
    String nextCursor = null;
    Instant nextAfter = null;

    if (hasNext && !comments.isEmpty()) {
      Comment lastComment = comments.get(comments.size() - 1);
      nextAfter = lastComment.getCreatedAt(); // 보조 커서는 작성일자

      if ("likeCount".equals(request.orderBy())) {
        nextCursor = lastComment.getLikeCount() + "_" + lastComment.getId(); // nextCursor 필드 기준값에 UUID를 추가하여 중복 방지
      } else {
        nextCursor = lastComment.getCreatedAt().toString() + "_" + lastComment.getId();
      }
    }

    // 7. 전체 요소 개수 조회
    long totalElements = commentRepository.countByArticleIdAndDeletedAtIsNull(articleId);

    log.info("[COMMENT_READ] 댓글 목록 조회 완료: articleId={}, 응답 데이터 수={}, hasNext={}",
        articleId, content.size(), hasNext);

    // 8. 최종 응답 DTO 생성
    return new CursorPageResponseCommentDto(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        totalElements,
        hasNext
    );
  }
}

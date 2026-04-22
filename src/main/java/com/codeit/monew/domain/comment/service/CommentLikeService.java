package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.comment.dto.CommentLikeDto;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.entity.CommentLike;
import com.codeit.monew.domain.comment.repository.CommentLikeRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.event.CommentLikedCancelEvent;
import com.codeit.monew.global.event.CommentLikedEvent;
import com.codeit.monew.global.exception.comment.CommentLikeAlreadyExistsException;
import com.codeit.monew.global.exception.comment.CommentLikeNotFoundException;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
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
@Transactional(readOnly = true)
public class CommentLikeService {
  private final CommentLikeRepository commentLikeRepository;
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  // 댓글 좋아요 등록
  @Transactional
  public CommentLikeDto addLike(UUID commentId, UUID userId) {
    // 1. 유저 검증
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 2. 댓글 및 작성자 정보 조회
    Comment comment = commentRepository.findByIdWithUser(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // 3. 중복 좋아요 검증
    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      throw new CommentLikeAlreadyExistsException(commentId, userId);
    }

    try {
      // 4. 좋아요 카운트 증가 및 저장
      comment.increaseLikeCount();
      CommentLike savedLike = commentLikeRepository.save(new CommentLike(comment, user));
      commentLikeRepository.flush(); // 즉시 반영하여 유니크 제약 검사 유도

      log.info("댓글 좋아요 등록: commentId={}, userId={}", commentId, userId);

      // 활동 내역 댓글 좋아요 정보 갱신 로직
      eventPublisher.publishEvent(new CommentLikedEvent(
          userId,
          savedLike.getId(),
          savedLike.getCreatedAt() != null ? savedLike.getCreatedAt() : Instant.now(),
          commentId,
          comment.getArticle().getId(),
          comment.getArticle().getTitle(),
          comment.getUser().getId(),
          comment.getUser().getNickname(),
          comment.getContent(),
          comment.getLikeCount(),
          comment.getCreatedAt()
      ));

      return new CommentLikeDto(
          savedLike.getId(),
          user.getId(),
          savedLike.getCreatedAt(),
          comment.getId(),
          comment.getArticle().getId(),
          comment.getUser().getId(),
          comment.getUser().getNickname(),
          comment.getContent(),
          comment.getLikeCount(),
          comment.getCreatedAt()
      );
    } catch (DataIntegrityViolationException e) {
      // 동시 요청으로 인해 DB 유니크 제약 조건 위반 시 예외 처리
      log.warn("좋아요 중복 등록 시도 감지: userId={}, commentId={}", userId, commentId);
      throw new CommentLikeAlreadyExistsException(commentId, userId);
    }
  }

  // 댓글 좋아요 취소
  @Transactional
  public void cancelLike(UUID commentId, UUID userId) {
    // 1. 댓글 검증
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // 2. 일단 삭제를 시도하고 삭제된 행 개수를 가져옴 (중복 좋아요가 없거나 이미 취소된 경우 0이 됨)
    int deletedCount = commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);

    if (deletedCount == 0) {
      // 삭제된 게 없다면 이미 누가 삭제했거나 존재하지 않는 것
      throw new CommentLikeNotFoundException(commentId, userId);
    }

    // 3. DB에서 실제로 삭제가 성공했을 때 좋아요 수 감소
    comment.decreaseLikeCount();

    // 활동 내역 댓글 좋아요 정보 갱신 로직
    eventPublisher.publishEvent(new CommentLikedCancelEvent(
        userId,
        commentId
    ));

    log.info("댓글 좋아요 취소: commentId={}, userId={}", commentId, userId);
  }
}

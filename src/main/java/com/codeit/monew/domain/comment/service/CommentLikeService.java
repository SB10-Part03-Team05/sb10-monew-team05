package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.comment.dto.CommentLikeDto;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.entity.CommentLike;
import com.codeit.monew.domain.comment.repository.CommentLikeRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.comment.CommentLikeAlreadyExistsException;
import com.codeit.monew.global.exception.comment.CommentLikeNotFoundException;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentLikeService {
  private final CommentLikeRepository commentLikeRepository;
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;

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

    // 4. 좋아요 수 증가 (엔티티의 @Version을 통해 낙관적 락 발동)
    comment.increaseLikeCount();

    // 5. 좋아요 교차 테이블 데이터 저장
    CommentLike savedLike = commentLikeRepository.save(new CommentLike(comment, user));
    log.info("댓글 좋아요 등록: commentId={}, userId={}", commentId, userId);

    // 6. DTO 변환
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
  }

  // 댓글 좋아요 취소
  @Transactional
  public void cancelLike(UUID commentId, UUID userId) {
    // 1. 댓글 검증
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // 2. 좋아요 존재 여부 확인
    if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      throw new CommentLikeNotFoundException(commentId, userId);
    }

    // 3. 좋아요 수 감소
    comment.decreaseLikeCount();

    // 4. 좋아요 데이터 삭제
    commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    log.info("댓글 좋아요 취소: commentId={}, userId={}", commentId, userId);
  }
}

package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.mapper.CommentMapper;
import com.codeit.monew.domain.comment.repository.CommentLikeRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.event.CommentCreatedEvent;
import com.codeit.monew.global.exception.article.ArticleNotFoundException;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
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
  private final CommentMapper commentMapper;
  private final ApplicationEventPublisher eventPublisher;

  // 댓글 등록
  @Transactional
  public CommentDto registerComment(UUID articleId, UUID userId, String content) {

    // 1. 유저 조회 및 검증
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 2. 기사 조회 및 검증
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    // 3. 댓글 엔티티 생성 및 영속화
    Comment comment = new Comment(article, user, content);
    Comment savedComment = commentRepository.save(comment);
    log.info("댓글 등록 완료: commentId= {}, articleId= {}", savedComment.getId(), articleId);

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
    // 1. 댓글 및 작성자 정보 한 번에 조회
    Comment comment = commentRepository.findByIdWithUser(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // 2. 엔티티 내부에서 권한 검증 및 수정 수행
    comment.updateContent(content, requesterId);
    log.info("댓글 수정 완료: commentId= {}, requesterId= {}", commentId, requesterId);

    // 3. DTO 변환
    boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, requesterId);
    return commentMapper.toDto(comment, comment.getUser().getNickname(), likedByMe);
  }

  // 댓글 논리 삭제 (삭제 기본값)
  @Transactional
  public void deleteComment(UUID commentId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CommentNotFoundException(commentId));

    // 엔티티에 설정된 @SQLDelete 작동 -> deleted_at에 현재 시간 기록
    commentRepository.delete(comment);
    log.info("댓글 논리 삭제 완료: commentId= {}", commentId);
  }

  // 댓글 물리 삭제
  @Transactional
  public void hardDeleteComment(UUID commentId) {
    // 1. 댓글 존재 여부 확인
    boolean exists = commentRepository.existsById(commentId);
    if (!exists) {
      throw new CommentNotFoundException(commentId);
    }

    // 2. 연관 데이터 물리 삭제
    commentLikeRepository.deleteByCommentId(commentId);

    // 3. 댓글 물리 삭제
    commentRepository.deleteByIdHard(commentId);
    log.info("댓글 물리 삭제 완료: commentId= {}", commentId);
  }
}

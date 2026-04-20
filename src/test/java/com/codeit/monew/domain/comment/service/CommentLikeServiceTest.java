package com.codeit.monew.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.entity.Article;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentLikeServiceTest {
  @InjectMocks
  private CommentLikeService commentLikeService;

  @Mock private CommentLikeRepository commentLikeRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private UserRepository userRepository;

  private UUID userId;
  private UUID commentId;
  private User user;
  private Comment comment;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    commentId = UUID.randomUUID();

    user = mock(User.class);
    given(user.getId()).willReturn(userId);
    given(user.getNickname()).willReturn("testUser");

    Article article = mock(Article.class);
    given(article.getId()).willReturn(UUID.randomUUID());

    comment = mock(Comment.class);
    given(comment.getId()).willReturn(commentId);
    given(comment.getUser()).willReturn(user);
    given(comment.getArticle()).willReturn(article);
    given(comment.getContent()).willReturn("테스트 댓글입니다.");
    given(comment.getLikeCount()).willReturn(0L);
  }

  @Nested
  @DisplayName("댓글 좋아요 등록 테스트")
  class AddLikeTest {

    @Test
    @DisplayName("정상적인 요청일 경우 좋아요 카운트가 증가하고 DTO를 반환한다.")
    void success_addLike() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
      given(commentRepository.findByIdWithUser(commentId)).willReturn(Optional.of(comment));
      given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(false);

      CommentLike savedLike = mock(CommentLike.class);
      given(savedLike.getId()).willReturn(UUID.randomUUID());
      given(commentLikeRepository.save(any(CommentLike.class))).willReturn(savedLike);

      // when
      CommentLikeDto result = commentLikeService.addLike(commentId, userId);

      // then
      verify(comment).increaseLikeCount(); // 낙관적 락 발동 메서드 호출 검증
      verify(commentLikeRepository).save(any(CommentLike.class)); // 저장 검증
      assertThat(result.commentId()).isEqualTo(commentId);
      assertThat(result.likedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("이미 좋아요를 누른 댓글에 또 좋아요를 누른다면, CommentLikeAlreadyExistsException 예외가 발생한다.")
    void fail_like_AlreadyExists() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
      given(commentRepository.findByIdWithUser(commentId)).willReturn(Optional.of(comment));
      given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(true); // 중복 발생

      // when & then
      assertThatThrownBy(() -> commentLikeService.addLike(commentId, userId))
          .isInstanceOf(CommentLikeAlreadyExistsException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글일 경우, CommentNotFoundException 예외가 발생한다/")
    void fail_CommentNotFound() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
      given(commentRepository.findByIdWithUser(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentLikeService.addLike(commentId, userId))
          .isInstanceOf(CommentNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 취소 테스트")
  class CancelLikeTest {

    @Test
    @DisplayName("정상적인 요청일 경우, 좋아요 카운트가 감소하고 데이터를 삭제한다.")
    void success_cancelLike() {
      // given
      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(true);

      // when
      commentLikeService.cancelLike(commentId, userId);

      // then
      verify(comment).decreaseLikeCount(); // 감소 메서드 호출 검증
      verify(commentLikeRepository).deleteByCommentIdAndUserId(commentId, userId); // 삭제 쿼리 검증
    }

    @Test
    @DisplayName("누른 적 없는 좋아요를 취소하려고 하면, CommentLikeNotFoundException 예외가 발생한다.")
    void fail_like_NotFound() {
      // given
      given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
      given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)).willReturn(false); // 좋아요 데이터 없음!

      // when & then
      assertThatThrownBy(() -> commentLikeService.cancelLike(commentId, userId))
          .isInstanceOf(CommentLikeNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글일 경우, CommentNotFoundException 예외가 발생한다.")
    void fail_CommentNotFound() {
      // given
      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentLikeService.cancelLike(commentId, userId))
          .isInstanceOf(CommentNotFoundException.class);
    }
  }
}
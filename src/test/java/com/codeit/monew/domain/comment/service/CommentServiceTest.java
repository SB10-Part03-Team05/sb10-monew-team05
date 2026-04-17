package com.codeit.monew.domain.comment.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.mapper.CommentMapper;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.article.ArticleNotFoundException; // 경로 확인 필요
import com.codeit.monew.global.exception.user.UserNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
  @InjectMocks
  private CommentService commentService; // 💡 테스트 대상 (가짜 객체들을 주입받을 진짜 서비스)

  @Mock private CommentRepository commentRepository;
  @Mock private ArticleRepository articleRepository;
  @Mock private UserRepository userRepository;
  @Mock private CommentMapper commentMapper;

  @Nested
  @DisplayName("댓글 등록 테스트")
  class RegisterComment {

    @Test
    @DisplayName("유저와 기사가 존재하면 댓글이 정상적으로 저장되고 DTO를 반환한다.")
    void success_register_comment() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      String content = "테스트 댓글 내용";
      String nickname = "테스트유저";

      // 가짜 객체 생성
      User mockUser = mock(User.class);
      Article mockArticle = mock(Article.class);
      Comment mockComment = mock(Comment.class);

      CommentDto expectedDto = new CommentDto(UUID.randomUUID(), articleId, userId, nickname, content, 0L, false, Instant.now());

      // DB 조회 및 매핑 Mock 설정
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));
      given(mockUser.getNickname()).willReturn(nickname);

      given(articleRepository.findById(articleId)).willReturn(Optional.of(mockArticle));

      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);
      given(commentMapper.toDto(any(), anyString(), anyBoolean())).willReturn(expectedDto);

      // when
      CommentDto result = commentService.registerComment(articleId, userId, content);

      // then
      assertThat(result).isNotNull();
      assertThat(result.content()).isEqualTo(content);

      // DB에 save 메서드가 1번 호출되었는지 검증
      verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID가 들어오면 UserNotFoundException이 발생한다.")
    void fail_comment_userNotFound() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      // 유저 없음
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.registerComment(articleId, userId, "내용"))
          .isInstanceOf(UserNotFoundException.class);

      // 유저가 없으면 기사 조회나 댓글 저장은 실행되면 안 됨을 검증
      verify(articleRepository, never()).findById(any());
      verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 기사 ID가 들어오면 ArticleNotFoundException이 발생한다.")
    void fail_comment_articleNotFound() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      User mockUser = mock(User.class);

      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser)); // 유저 존재
      given(articleRepository.findById(articleId)).willReturn(Optional.empty()); // 기사 없음

      // when & then
      assertThatThrownBy(() -> commentService.registerComment(articleId, userId, "내용"))
          .isInstanceOf(ArticleNotFoundException.class);

      // 기사가 없으면 댓글 저장은 실행되면 안 됨을 검증
      verify(commentRepository, never()).save(any());
    }
  }
}
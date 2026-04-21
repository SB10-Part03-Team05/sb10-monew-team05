package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.entity.CommentLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  // 중복 좋아요 방지
  boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

  // 좋아요 취소
  @Modifying
  @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId AND cl.user.id = :userId")
  int deleteByCommentIdAndUserId(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

  // 댓글 물리 삭제 시 좋아요 데이터도 함께 삭제
  @Modifying
  @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
  void deleteByCommentId(@Param("commentId") UUID commentId);
}

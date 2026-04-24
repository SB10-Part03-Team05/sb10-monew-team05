package com.codeit.monew.domain.notification.event;

import java.util.UUID;

public record CommentLikedEvent(
    UUID commentId,
    UUID articleId,
    UUID readerId, // 알림을 받을 사람 (댓글 작성자)
    String likerNickname // 좋아요를 누른 사람의 닉네임 (알림 문구용)
) {

}

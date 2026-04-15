-- 사용자 table
CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    nickname   VARCHAR(20)  NOT NULL,
    password   VARCHAR(20)  NOT NULL,
    deleted_at TIMESTAMPTZ NULL
);

-- 관심사 table
CREATE TABLE interests
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(50) NOT NULL,
    subscriber_count BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL
);

-- 키워드 table
CREATE TABLE keywords
(
    id          UUID PRIMARY KEY,
    interest_id UUID        NOT NULL,
    name        VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_keywords_interests
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,

    CONSTRAINT uq_keywords_interest_name
        UNIQUE (interest_id, name)
);

-- 구독 테이블
CREATE TABLE subscriptions
(
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL,
    interest_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_subscriptions_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,

    CONSTRAINT uq_subscriptions_user_interest
        UNIQUE (user_id, interest_id)
);

-- 뉴스 기사 table
CREATE TABLE articles
(
    id           UUID PRIMARY KEY,
    source_url   VARCHAR(2048) NOT NULL UNIQUE,
    source       VARCHAR(50)   NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    publish_date TIMESTAMPTZ   NOT NULL,
    summary      TEXT          NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    deleted_at   TIMESTAMPTZ NULL
);

-- 뉴스 기사-관심사 매핑
CREATE TABLE article_interests
(
    article_id  UUID NOT NULL,
    interest_id UUID NOT NULL,

    CONSTRAINT pk_article_interests PRIMARY KEY (article_id, interest_id),
    CONSTRAINT fk_article_interests_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_article_interests_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE
);

-- 뉴스 기사 조회 이력 table
CREATE TABLE article_view_histories
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    article_id UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_article_view_histories_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_article_view_histories_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT uq_article_view_histories_user_article UNIQUE (user_id, article_id)
);

-- 소프트 삭제된 기사를 제외한 기사 목록 조회
CREATE INDEX idx_articles_active_publish_date ON articles (publish_date DESC) WHERE deleted_at IS NULL;
-- 관심사별 기사 조회 시
CREATE INDEX idx_article_interests_interest_id ON article_interests (interest_id);
-- 기사별 조회 수 집계나 조회 이력 또는 기사 삭제 시
CREATE INDEX idx_article_view_histories_article_id ON article_view_histories (article_id);

-- 알림 table
CREATE TABLE notifications
(
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL,
    content       VARCHAR(255) NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    resource_id   UUID         NOT NULL,
    confirmed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 인덱스) 안 읽은 알림만 빠르게 조회 및 1주일 뒤 삭제 배치를 위한 부분 인덱스
CREATE INDEX idx_notifications_unread ON notifications (user_id, created_at DESC) WHERE confirmed_at IS NULL;

-- 댓글 table (댓글 + 통계 병합)
CREATE TABLE comments
(
    id         UUID PRIMARY KEY,
    article_id UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    content    VARCHAR(500) NOT NULL,
    like_count BIGINT       NOT NULL DEFAULT 0,
    version    BIGINT       NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_comments_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 인덱스) 특정 기사의 댓글 목록 커서 페이지네이션 (최신순)
CREATE INDEX idx_comments_pagination ON comments (article_id, created_at DESC, id DESC);


-- 댓글 좋아요 이력 table
CREATE TABLE comment_likes
(
    id         UUID PRIMARY KEY,
    comment_id UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_likes_comment_user UNIQUE (comment_id, user_id)
);
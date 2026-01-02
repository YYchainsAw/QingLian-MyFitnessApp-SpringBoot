-- 1. 启用 UUID 扩展 (PostgreSQL 特有)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. 创建自动更新 updated_at 的触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- ==========================================
-- Users 表 (ID 改为 UUID, 新增时间字段)
-- ==========================================
CREATE TABLE users (
    user_id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    nickname        VARCHAR(50),
    avatar_url      VARCHAR(255),
    gender          VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')), -- 优化: 枚举约束
    height_cm       INTEGER,
    weight_kg       NUMERIC(5, 2),
    last_login_time TIMESTAMP, -- 新增: 最后登录时间
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 新增: 更新时间
);

-- 绑定触发器
CREATE TRIGGER update_users_modtime BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Movements 表 (动作库，通常变动少，ID保持 Serial)
-- ==========================================
CREATE TABLE movements (
    movement_id      BIGSERIAL PRIMARY KEY,
    title            VARCHAR(100) NOT NULL,
    description      TEXT,
    video_url        VARCHAR(255),
    category         VARCHAR(50),
    difficulty_level INTEGER DEFAULT 1 CHECK (difficulty_level BETWEEN 1 AND 5),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_movements_modtime BEFORE UPDATE ON movements FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Plans 表 (外键改为 UUID)
-- ==========================================
CREATE TABLE plans (
    plan_id     BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(user_id) ON DELETE CASCADE, -- 优化: 用户删了计划也删
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    start_date  DATE,
    end_date    DATE,
    status      VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plans_user_id ON plans(user_id); -- 优化: 查询某人的计划
CREATE TRIGGER update_plans_modtime BEFORE UPDATE ON plans FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Workout Records 表
-- ==========================================
CREATE TABLE workout_records (
    record_id        BIGSERIAL PRIMARY KEY,
    user_id          UUID REFERENCES users(user_id) ON DELETE CASCADE,
    plan_id          BIGINT REFERENCES plans(plan_id) ON DELETE SET NULL,
    duration_seconds INTEGER,
    calories_burned  INTEGER,
    workout_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes            TEXT
);

CREATE INDEX idx_workout_records_user_date ON workout_records(user_id, workout_date DESC); -- 优化: 查询某人的历史记录

-- ==========================================
-- Posts 表 (社区动态)
-- ==========================================
CREATE TABLE posts (
    post_id     BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(user_id) ON DELETE CASCADE,
    content     TEXT,
    image_urls  TEXT[], -- PostgreSQL 数组类型
    likes_count INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC); -- 优化: 首页Feed流按时间倒序
CREATE TRIGGER update_posts_modtime BEFORE UPDATE ON posts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Friendships 表 (复合主键)
-- ==========================================
CREATE TABLE friendships (
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    friend_id  UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status     VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id)
);

CREATE INDEX idx_friendships_friend_id ON friendships(friend_id); -- 优化: 查询"谁关注了我"
CREATE TRIGGER update_friendships_modtime BEFORE UPDATE ON friendships FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- Messages 表 (私信)
-- ==========================================
CREATE TABLE messages (
    msg_id      BIGSERIAL PRIMARY KEY,
    sender_id   UUID REFERENCES users(user_id) ON DELETE SET NULL,
    receiver_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    content     TEXT,
    is_read     BOOLEAN DEFAULT FALSE,
    sent_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_sender_receiver ON messages(sender_id, receiver_id); -- 优化: 聊天记录查询
CREATE INDEX idx_messages_unread ON messages(receiver_id) WHERE is_read = FALSE; -- 优化: 快速统计未读消息

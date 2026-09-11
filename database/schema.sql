CREATE DATABASE IF NOT EXISTS news_management;
USE news_management;

-- Drop tables in reverse order of dependencies to avoid FK conflicts
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `news`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `user`;

-- 1. Table `user`
CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_date TIMESTAMP NULL,
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT uq_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Table `role`
CREATE TABLE `role` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_role_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Table `category`
CREATE TABLE `category` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_date TIMESTAMP NULL,
    CONSTRAINT uq_category_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Table `user_role`
CREATE TABLE `user_role` (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES `role`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Table `news`
CREATE TABLE `news` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    short_description VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    thumbnail VARCHAR(500) NULL,
    category_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_date TIMESTAMP NULL,
    CONSTRAINT fk_news_category FOREIGN KEY (category_id) REFERENCES `category`(id) ON DELETE RESTRICT,
    CONSTRAINT fk_news_created_by FOREIGN KEY (created_by) REFERENCES `user`(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Table `comment`
CREATE TABLE `comment` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    news_id BIGINT NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE RESTRICT,
    CONSTRAINT fk_comment_news FOREIGN KEY (news_id) REFERENCES `news`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indexes
CREATE INDEX idx_user_role_role_id ON `user_role`(role_id);
CREATE INDEX idx_news_category_id ON `news`(category_id);
CREATE INDEX idx_news_created_by ON `news`(created_by);
CREATE INDEX idx_comment_user_id ON `comment`(user_id);
CREATE INDEX idx_comment_news_created_date ON `comment`(news_id, created_date);

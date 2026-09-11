USE news_management;

-- 1. Insert Roles
INSERT INTO `role` (code, name) VALUES
    ('ADMIN', 'Administrator'),
    ('USER', 'User');

-- 2. Insert Users (Sample Demo Users with precomputed valid BCrypt hash, NO plaintext passwords)
-- Development sample only.
-- Password hashing/authentication logic is implemented in Sprint 4.
INSERT INTO `user` (username, password, full_name, email, status) VALUES
    ('admin_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 'admin@example.com', 1),
    ('demo_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Demo User', 'demo@example.com', 1);

-- 3. Insert User-Role mapping (resolve IDs dynamically via business keys)
INSERT INTO `user_role` (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.code = 'ADMIN'
WHERE u.username = 'admin_user';

INSERT INTO `user_role` (user_id, role_id)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON r.code = 'USER'
WHERE u.username = 'demo_user';

-- 4. Insert Categories
INSERT INTO `category` (name, code) VALUES
    ('Java', 'java'),
    ('Technology', 'technology'),
    ('Programming', 'programming'),
    ('Database', 'database');

-- 5. Insert News (resolve category_id and created_by dynamically via business keys)
INSERT INTO `news` (title, short_description, content, thumbnail, category_id, created_by)
SELECT 
    'Java 21 Released',
    'Java 21 is now available.',
    'Full content about Java 21 release...',
    'java21.png',
    c.id,
    u.id
FROM `category` c
JOIN `user` u ON u.username = 'admin_user'
WHERE c.code = 'java';

INSERT INTO `news` (title, short_description, content, thumbnail, category_id, created_by)
SELECT 
    'MySQL 8.0 Features',
    'Discover what is new in MySQL.',
    'Full content about MySQL 8.0...',
    'mysql.png',
    c.id,
    u.id
FROM `category` c
JOIN `user` u ON u.username = 'admin_user'
WHERE c.code = 'database';

INSERT INTO `news` (title, short_description, content, thumbnail, category_id, created_by)
SELECT 
    'Tech Trends 2024',
    'What to expect in technology.',
    'Full content about tech trends...',
    'tech.png',
    c.id,
    u.id
FROM `category` c
JOIN `user` u ON u.username = 'demo_user'
WHERE c.code = 'technology';

-- 6. Insert Comments (resolve user_id and news_id dynamically)
INSERT INTO `comment` (content, user_id, news_id)
SELECT 
    'Great article on Java!',
    u.id,
    n.id
FROM `user` u
JOIN `news` n ON n.title = 'Java 21 Released'
WHERE u.username = 'demo_user';

INSERT INTO `comment` (content, user_id, news_id)
SELECT 
    'Thanks for the MySQL tips.',
    u.id,
    n.id
FROM `user` u
JOIN `news` n ON n.title = 'MySQL 8.0 Features'
WHERE u.username = 'demo_user';

INSERT INTO `comment` (content, user_id, news_id)
SELECT 
    'Admin comment on tech trends.',
    u.id,
    n.id
FROM `user` u
JOIN `news` n ON n.title = 'Tech Trends 2024'
WHERE u.username = 'admin_user';

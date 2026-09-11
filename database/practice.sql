USE news_management;

-- Query 1 - SELECT Category
-- Select all categories available in the system
SELECT id, name, code, created_date 
FROM `category`;

-- Query 2 - WHERE Category Code
-- Select a specific category by its unique code
SELECT id, name, code 
FROM `category`
WHERE code = 'java';

-- Query 3 - JOIN News + Category
-- View news articles along with their category name and code
SELECT 
    n.id AS news_id,
    n.title,
    c.name AS category_name,
    c.code AS category_code
FROM `news` n
JOIN `category` c 
    ON n.category_id = c.id;

-- Query 4 - JOIN User + Role
-- Display users and their assigned roles (only returns data if sample users are present)
SELECT 
    u.username,
    u.full_name,
    r.code AS role_code,
    r.name AS role_name
FROM `user` u
JOIN `user_role` ur 
    ON u.id = ur.user_id
JOIN `role` r 
    ON ur.role_id = r.id;

-- Query 5 - JOIN Comment + User + News
-- Display comments along with the commenter's username and the news title they commented on
SELECT 
    cm.id AS comment_id,
    cm.content AS comment_content,
    u.username AS commenter,
    n.title AS news_title,
    cm.created_date
FROM `comment` cm
JOIN `user` u 
    ON cm.user_id = u.id
JOIN `news` n 
    ON cm.news_id = n.id;

-- Query 6 - COUNT
-- Count the total number of news articles in the system
SELECT COUNT(*) AS total_news
FROM `news`;

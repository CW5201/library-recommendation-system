-- ============================================================
-- 高校图书馆图书推荐系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 使用方式: mysql -uroot -p < sql/init.sql
-- 默认账号: admin / Admin@123    student / Student@123
-- 注意: 演示密码仅用于本地答辩演示，部署前请务必修改。
-- ============================================================
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS library_recommendation
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_recommendation;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS book_embedding;
DROP TABLE IF EXISTS recommendation_log;
DROP TABLE IF EXISTS ai_chat_message;
DROP TABLE IF EXISTS borrow_record;
DROP TABLE IF EXISTS book_rating;
DROP TABLE IF EXISTS user_favorite;
DROP TABLE IF EXISTS user_behavior;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS book_category;
DROP TABLE IF EXISTS sys_user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  nickname VARCHAR(50) NOT NULL COMMENT '昵称',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
  role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_role (role),
  KEY idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE book_category (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  name VARCHAR(50) NOT NULL COMMENT '分类名称',
  description VARCHAR(255) DEFAULT NULL COMMENT '分类介绍',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

CREATE TABLE book (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图书ID',
  title VARCHAR(200) NOT NULL COMMENT '书名',
  author VARCHAR(120) NOT NULL COMMENT '作者',
  publisher VARCHAR(120) DEFAULT NULL COMMENT '出版社',
  isbn VARCHAR(20) NOT NULL COMMENT 'ISBN',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  description TEXT COMMENT '简介',
  cover VARCHAR(255) DEFAULT NULL COMMENT '封面',
  publish_date DATE DEFAULT NULL COMMENT '出版日期',
  total_stock INT NOT NULL DEFAULT 1 COMMENT '馆藏总量',
  available_stock INT NOT NULL DEFAULT 1 COMMENT '可借数量',
  rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '平均评分',
  rating_count INT NOT NULL DEFAULT 0 COMMENT '评分人数',
  borrow_count INT NOT NULL DEFAULT 0 COMMENT '累计借阅次数',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_book_isbn (isbn),
  KEY idx_book_category (category_id),
  KEY idx_book_title (title),
  KEY idx_book_author (author),
  KEY idx_book_status (status),
  CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES book_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

CREATE TABLE user_behavior (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  behavior_type VARCHAR(20) NOT NULL COMMENT 'VIEW/FAVORITE/BORROW/RATE/RETURN',
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_behavior_user_time (user_id, create_time),
  KEY idx_behavior_book (book_id),
  KEY idx_behavior_type (behavior_type),
  CONSTRAINT fk_behavior_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_behavior_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

CREATE TABLE user_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorite_user_book (user_id, book_id),
  KEY idx_favorite_book (book_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_favorite_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

CREATE TABLE book_rating (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  score TINYINT NOT NULL COMMENT '1-5分',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rating_user_book (user_id, book_id),
  KEY idx_rating_book (book_id),
  CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_rating_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
  CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分表';

CREATE TABLE borrow_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  borrow_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_time DATETIME NOT NULL,
  return_time DATETIME DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'BORROWED' COMMENT 'BORROWED/RETURNED/OVERDUE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_borrow_user (user_id),
  KEY idx_borrow_book (book_id),
  KEY idx_borrow_status (status),
  CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES book (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

CREATE TABLE ai_chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(64) NOT NULL,
  role VARCHAR(20) NOT NULL COMMENT 'user/assistant',
  content TEXT NOT NULL,
  related_books TEXT COMMENT '关联图书JSON',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chat_user_session (user_id, session_id, create_time),
  CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

CREATE TABLE recommendation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  algorithm VARCHAR(30) NOT NULL COMMENT 'HOT/CONTENT/ITEM_CF/HYBRID',
  score DECIMAL(8,5) NOT NULL DEFAULT 0,
  reason VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_recommend_user_time (user_id, create_time),
  KEY idx_recommend_book (book_id),
  CONSTRAINT fk_recommend_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_recommend_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果日志表';

CREATE TABLE book_embedding (
  id BIGINT NOT NULL AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  model VARCHAR(100) NOT NULL,
  dimension INT NOT NULL,
  vector LONGTEXT NOT NULL COMMENT 'JSON数组',
  content_hash VARCHAR(64) NOT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_embedding_book (book_id),
  CONSTRAINT fk_embedding_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书向量索引表';

-- ------------------------------------------------------------
-- 初始化用户（密码均为 BCrypt 加密）
-- admin   / Admin@123
-- student / Student@123
-- reader01 ~ reader04 / Demo@123
-- ------------------------------------------------------------
INSERT INTO sys_user (id, username, password, nickname, email, phone, role, status) VALUES
(1, 'admin', '$2b$10$7fgLj6ISmQMKMmZnJy/Kf.8g4AEdcW5AgkykKiG.rCxDOblmInXVO', '系统管理员', 'admin@library.edu.cn', '13800000001', 'ADMIN', 1),
(2, 'student', '$2b$10$0YUtmi98uZSCrikVQDDJAOitdmswfjWHCsUSLJ26gM3Ul9HeOxVuG', '林晓', 'student@library.edu.cn', '13800000002', 'USER', 1),
(3, 'reader01', '$2b$10$pRDwWVkcEpUF7hLbaivo8eeSDFk3Bbj0yPGGHxNdctTAhSLvtNpfS', '陈默', 'reader01@library.edu.cn', '13800000003', 'USER', 1),
(4, 'reader02', '$2b$10$pRDwWVkcEpUF7hLbaivo8eeSDFk3Bbj0yPGGHxNdctTAhSLvtNpfS', '苏婉', 'reader02@library.edu.cn', '13800000004', 'USER', 1),
(5, 'reader03', '$2b$10$pRDwWVkcEpUF7hLbaivo8eeSDFk3Bbj0yPGGHxNdctTAhSLvtNpfS', '王浩', 'reader03@library.edu.cn', '13800000005', 'USER', 1),
(6, 'reader04', '$2b$10$pRDwWVkcEpUF7hLbaivo8eeSDFk3Bbj0yPGGHxNdctTAhSLvtNpfS', '李想', 'reader04@library.edu.cn', '13800000006', 'USER', 1);

INSERT INTO book_category (id, name, description, sort_order) VALUES
(1, '计算机科学', '编程开发、人工智能、数据库与计算机网络', 1),
(2, '文学小说', '中外经典文学、小说与散文', 2),
(3, '历史传记', '中国史、世界史与人物传记', 3),
(4, '哲学思想', '哲学经典、伦理与思想史', 4),
(5, '经济管理', '经济学、管理学与商业实践', 5),
(6, '自然科学', '数学、物理、生物与科学通识', 6),
(7, '艺术设计', '艺术史、设计理论与视觉文化', 7),
(8, '心理学', '心理学通识、行为科学与自我成长', 8),
(9, '外语学习', '英语学习、语法与语言能力', 9),
(10, '社会科学', '社会学、传播学与公共议题', 10);

INSERT INTO book (id, title, author, publisher, isbn, category_id, description, cover, publish_date, total_stock, available_stock, rating_avg, rating_count, borrow_count, view_count, status) VALUES
(1, '深入理解计算机系统（原书第3版）', 'Randal E. Bryant', '机械工业出版社', '9787111544937', 1, '从程序员视角讲解计算机系统底层原理，涵盖处理器、存储层次、链接、异常控制流、虚拟内存与网络编程，是理解程序如何真正运行的经典教材。', '/covers/computer.svg', '2016-11-01', 5, 5, 4.90, 128, 186, 980, 1),
(2, '算法导论（原书第3版）', 'Thomas H. Cormen', '机械工业出版社', '9787111407010', 1, '系统介绍算法设计与分析的基础理论与经典算法，包括排序、图算法、动态规划、贪心算法与NP完全性，适合深入学习算法。', '/covers/computer.svg', '2013-01-01', 4, 3, 4.80, 96, 152, 860, 1),
(3, 'Java核心技术 卷I（原书第11版）', 'Cay S. Horstmann', '机械工业出版社', '9787111636663', 1, '全面讲解Java语言基础、面向对象、集合、异常、并发与常用API，内容扎实、示例丰富，是Java学习者的常备参考书。', '/covers/computer.svg', '2019-12-01', 5, 5, 4.70, 85, 143, 790, 1),
(4, 'Spring Boot编程思想（核心篇）', '小马哥', '电子工业出版社', '9787121355189', 1, '围绕Spring Boot的自动配置、条件装配、启动流程与Web开发展开，帮助读者理解框架设计思想并掌握企业级开发方法。', '/covers/computer.svg', '2019-04-01', 4, 4, 4.60, 67, 118, 650, 1),
(5, '代码整洁之道', 'Robert C. Martin', '人民邮电出版社', '9787115216878', 1, '通过大量案例讲解命名、函数、注释、错误处理与单元测试等实践，帮助开发者写出可读、可维护的高质量代码。', '/covers/computer.svg', '2010-01-01', 6, 6, 4.80, 154, 201, 1100, 1),
(6, '数据库系统概念（第6版）', 'Abraham Silberschatz', '机械工业出版社', '9787111375296', 1, '数据库领域的经典教材，覆盖关系模型、SQL、事务、并发控制、恢复与分布式数据库等核心内容。', '/covers/computer.svg', '2012-03-01', 3, 3, 4.60, 54, 97, 520, 1),
(7, '计算机网络（第7版）', '谢希仁', '电子工业出版社', '9787121302954', 1, '国内广泛使用的计算机网络教材，按协议层次系统讲解物理层、数据链路层、网络层、运输层与应用层。', '/covers/computer.svg', '2017-01-01', 3, 3, 4.50, 73, 105, 610, 1),
(8, '人工智能：一种现代的方法（第4版）', 'Stuart Russell', '清华大学出版社', '9787302569036', 1, '人工智能领域的权威教材，涵盖搜索、知识表示、机器学习、自然语言处理、机器人与伦理等主题。', '/covers/computer.svg', '2020-09-01', 4, 4, 4.80, 62, 121, 720, 1),
(9, '深度学习', 'Ian Goodfellow', '人民邮电出版社', '9787115461476', 1, '深度学习领域奠基性著作，系统介绍线性代数基础、前馈网络、正则化、优化、卷积网络、循环网络与生成模型。', '/covers/computer.svg', '2017-08-01', 3, 3, 4.70, 58, 116, 690, 1),
(10, '流畅的Python', 'Luciano Ramalho', '人民邮电出版社', '9787115428028', 1, '深入讲解Python数据模型、序列、字典、函数、面向对象、迭代器与并发，帮助开发者写出地道的Python代码。', '/covers/computer.svg', '2017-05-01', 4, 4, 4.70, 77, 132, 700, 1),
(11, '活着', '余华', '作家出版社', '9787506365437', 2, '讲述福贵历经家庭变故与时代变迁仍顽强活着的故事，语言朴素却具有震撼人心的力量。', '/covers/literature.svg', '2012-08-01', 5, 4, 4.90, 231, 268, 1380, 1),
(12, '百年孤独', '加西亚·马尔克斯', '南海出版公司', '9787544253994', 2, '布恩迪亚家族七代人的传奇故事，魔幻现实主义文学的代表作，展现拉丁美洲百年的历史与孤独。', '/covers/literature.svg', '2011-06-01', 4, 4, 4.80, 187, 224, 1210, 1),
(13, '围城', '钱钟书', '人民文学出版社', '9787020090006', 2, '以幽默讽刺的笔触描写知识分子的婚姻、事业与人生困境，是现代中国文学的重要作品。', '/covers/literature.svg', '1991-02-01', 4, 4, 4.70, 143, 178, 940, 1),
(14, '平凡的世界', '路遥', '北京十月文艺出版社', '9787530216781', 2, '以孙少安、孙少平兄弟为主线，展现普通人在时代洪流中的奋斗与爱情，充满现实力量。', '/covers/literature.svg', '2017-06-01', 5, 5, 4.90, 205, 246, 1290, 1),
(15, '小王子', '安托万·德·圣埃克苏佩里', '人民文学出版社', '9787020042494', 2, '写给大人的童话，借小王子的星际旅行探讨爱、责任、孤独与成长，文字纯净富有哲理。', '/covers/literature.svg', '2003-08-01', 4, 4, 4.80, 176, 198, 1080, 1),
(16, '人类简史：从动物到上帝', '尤瓦尔·赫拉利', '中信出版社', '9787508647357', 3, '从认知革命、农业革命到科学革命，重新审视人类如何成为地球的主宰，视角宏大而富有启发。', '/covers/history.svg', '2014-11-01', 5, 5, 4.70, 162, 203, 1180, 1),
(17, '万历十五年', '黄仁宇', '生活·读书·新知三联书店', '9787108009821', 3, '以1587年为切入点，通过万历皇帝、张居正、海瑞等人物命运，剖析明代政治与社会结构。', '/covers/history.svg', '1997-05-01', 3, 3, 4.80, 118, 159, 830, 1),
(18, '苏东坡传', '林语堂', '湖南文艺出版社', '9787540479343', 3, '林语堂笔下苏东坡的一生，展现其文学成就、政治际遇与豁达人格，是人物传记的经典之作。', '/covers/history.svg', '2018-01-01', 3, 3, 4.70, 89, 124, 660, 1),
(19, '史记（点校本）', '司马迁', '中华书局', '9787101003048', 3, '中国第一部纪传体通史，记载从黄帝到汉武帝时期的历史，兼具史学与文学价值。', '/covers/history.svg', '1982-11-01', 3, 3, 4.90, 76, 112, 590, 1),
(20, '中国哲学简史', '冯友兰', '北京大学出版社', '9787301150396', 4, '以简洁清晰的笔法梳理中国哲学从先秦到现代的发展脉络，是了解中国思想的入门经典。', '/covers/philosophy.svg', '2013-01-01', 4, 4, 4.70, 84, 119, 640, 1),
(21, '理想国', '柏拉图', '商务印书馆', '9787100017565', 4, '以对话体探讨正义、国家、教育、哲学王等议题，是西方政治哲学与伦理学的源头性著作。', '/covers/philosophy.svg', '1986-08-01', 3, 3, 4.60, 65, 98, 520, 1),
(22, '沉思录', '马可·奥勒留', '中央编译出版社', '9787511706386', 4, '罗马皇帝马可·奥勒留的哲学札记，围绕自我反省、克制、责任与内心平静展开。', '/covers/philosophy.svg', '2010-04-01', 3, 3, 4.50, 57, 86, 470, 1),
(23, '经济学原理（第7版）', 'N.格里高利·曼昆', '北京大学出版社', '9787301256906', 5, '以十大经济学原理为主线，结合大量生活案例讲解微观经济学与宏观经济学基础。', '/covers/economics.svg', '2015-05-01', 5, 5, 4.70, 121, 166, 850, 1),
(24, '国富论', '亚当·斯密', '商务印书馆', '9787100084673', 5, '古典经济学的奠基之作，论述分工、市场、价值、资本积累与自由贸易，影响深远。', '/covers/economics.svg', '2011-04-01', 3, 3, 4.60, 48, 77, 420, 1),
(25, '影响力', '罗伯特·西奥迪尼', '北京联合出版公司', '9787550285682', 5, '总结互惠、承诺一致、社会认同、喜好、权威与稀缺六大说服原则，解析日常决策中的心理机制。', '/covers/economics.svg', '2016-09-01', 4, 4, 4.60, 132, 171, 910, 1),
(26, '时间简史', '史蒂芬·霍金', '湖南科学技术出版社', '9787535732309', 6, '面向大众讲解宇宙起源、黑洞、时间箭头与量子引力等现代物理议题，是科普写作的典范。', '/covers/science.svg', '2010-04-01', 4, 4, 4.70, 145, 184, 990, 1),
(27, '物种起源', '查尔斯·达尔文', '商务印书馆', '9787100071754', 6, '阐述自然选择与进化论思想，用大量观察证据说明物种如何演化，是生物学史上的里程碑。', '/covers/science.svg', '2009-01-01', 3, 3, 4.50, 52, 81, 450, 1),
(28, '数学之美', '吴军', '人民邮电出版社', '9787115373557', 6, '用通俗语言讲解搜索引擎、自然语言处理与机器学习背后的数学思想，兼顾趣味与深度。', '/covers/science.svg', '2012-05-01', 4, 4, 4.80, 109, 148, 780, 1),
(29, '艺术的故事', 'E.H.贡布里希', '广西美术出版社', '9787549401830', 7, '从史前艺术到现代艺术，系统讲述西方艺术发展历程，是广受欢迎的艺术史入门读物。', '/covers/art.svg', '2014-11-01', 3, 3, 4.80, 93, 127, 670, 1),
(30, '设计中的设计', '原研哉', '山东人民出版社', '9787209045315', 7, '日本设计大师原研哉关于设计本质的思考，强调设计是对日常生活的重新发现。', '/covers/art.svg', '2006-11-01', 3, 3, 4.60, 71, 101, 540, 1),
(31, '心理学与生活（第19版）', '理查德·格里格', '人民邮电出版社', '9787115137579', 8, '心理学经典入门教材，内容涵盖感觉、知觉、学习、记忆、情绪、人格与社会心理学。', '/covers/psychology.svg', '2016-06-01', 4, 4, 4.70, 136, 175, 920, 1),
(32, '思考，快与慢', '丹尼尔·卡尼曼', '中信出版社', '9787508633558', 8, '诺贝尔经济学奖得主卡尼曼介绍人类思维的快与慢两套系统，揭示直觉判断中的偏差。', '/covers/psychology.svg', '2012-07-01', 4, 3, 4.70, 168, 212, 1120, 1),
(33, '被讨厌的勇气', '岸见一郎', '机械工业出版社', '9787111495482', 8, '以对话形式介绍阿德勒心理学，讨论课题分离、自我接纳与活在当下的生活哲学。', '/covers/psychology.svg', '2015-03-01', 3, 3, 4.60, 118, 156, 840, 1),
(34, '新概念英语3：培养技能', 'L.G.亚历山大', '外语教学与研究出版社', '9787560022542', 9, '经典英语学习教材，通过地道课文与系统练习提升阅读、写作与语言运用能力。', '/covers/language.svg', '1997-10-01', 5, 5, 4.50, 87, 120, 620, 1),
(35, '英语语法新思维（中级）', '张满胜', '群言出版社', '9787802562240', 9, '从思维角度讲解英语语法，帮助学习者理解时态、从句、非谓语等难点，适合系统提升。', '/covers/language.svg', '2011-05-01', 4, 4, 4.40, 62, 94, 500, 1),
(36, '社会学（第7版）', '安东尼·吉登斯', '北京大学出版社', '9787301274986', 10, '系统介绍社会学基本概念、研究方法与现代社会的核心议题，视野开阔、结构清晰。', '/covers/social.svg', '2015-10-01', 3, 3, 4.50, 44, 68, 380, 1),
(37, '乡土中国', '费孝通', '北京大学出版社', '9787301174828', 10, '以差序格局、礼治秩序等概念分析中国基层社会结构，是理解中国社会的重要经典。', '/covers/social.svg', '2012-10-01', 4, 4, 4.80, 102, 139, 760, 1),
(38, '娱乐至死', '尼尔·波兹曼', '中信出版社', '9787508648286', 10, '批判电视媒介对公共话语的侵蚀，讨论媒介如何塑造认知，对当今信息社会仍有警示意义。', '/covers/social.svg', '2015-05-01', 3, 3, 4.60, 79, 108, 580, 1);

-- 用户行为（浏览、收藏、评分、借阅）
INSERT INTO user_behavior (user_id, book_id, behavior_type, remark, create_time) VALUES
(2, 1, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 3, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 4, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 8, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 9, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(2, 11, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 28, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(2, 1, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 4, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 8, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(2, 1, 'RATE', '评分 5 分', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 4, 'RATE', '评分 4 分', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 8, 'RATE', '评分 5 分', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(2, 2, 'BORROW', '借阅图书', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 3, 'BORROW', '借阅图书', DATE_SUB(NOW(), INTERVAL 40 DAY)),
(2, 3, 'RETURN', '归还图书', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, 2, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(3, 3, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 5, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(3, 8, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3, 9, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 10, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 28, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 3, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 9, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 3, 'RATE', '评分 5 分', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 9, 'RATE', '评分 5 分', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 9, 'BORROW', '借阅图书', DATE_SUB(NOW(), INTERVAL 60 DAY)),
(3, 9, 'RETURN', '归还图书', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4, 11, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, 12, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4, 13, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4, 14, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(4, 15, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 11, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, 13, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4, 11, 'RATE', '评分 5 分', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, 12, 'RATE', '评分 4 分', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4, 11, 'BORROW', '借阅图书', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(5, 16, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(5, 17, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(5, 19, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(5, 20, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 21, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 16, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(5, 20, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 16, 'RATE', '评分 5 分', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(5, 17, 'RATE', '评分 4 分', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(5, 17, 'BORROW', '借阅图书', DATE_SUB(NOW(), INTERVAL 45 DAY)),
(5, 17, 'RETURN', '归还图书', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(6, 23, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, 25, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6, 31, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 32, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(6, 33, 'VIEW', '浏览图书详情', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(6, 23, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, 31, 'FAVORITE', '收藏图书', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 23, 'RATE', '评分 4 分', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, 31, 'RATE', '评分 5 分', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 32, 'BORROW', '借阅图书', DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO user_favorite (user_id, book_id, create_time) VALUES
(2, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 4, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 8, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(3, 3, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 9, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 11, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, 13, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(5, 16, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(5, 20, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6, 23, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, 31, DATE_SUB(NOW(), INTERVAL 6 DAY));

INSERT INTO book_rating (user_id, book_id, score, create_time) VALUES
(2, 1, 5, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 4, 4, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 8, 5, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(3, 3, 5, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(3, 9, 5, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 11, 5, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, 12, 4, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(5, 16, 5, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(5, 17, 4, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(6, 23, 4, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, 31, 5, DATE_SUB(NOW(), INTERVAL 6 DAY));

INSERT INTO borrow_record (user_id, book_id, borrow_time, due_time, return_time, status) VALUES
(2, 2, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 24 DAY), NULL, 'BORROWED'),
(2, 3, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 'RETURNED'),
(3, 9, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), 'RETURNED'),
(4, 11, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_ADD(NOW(), INTERVAL 18 DAY), NULL, 'BORROWED'),
(5, 17, DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), 'RETURNED'),
(6, 32, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 27 DAY), NULL, 'BORROWED');

ALTER TABLE sys_user AUTO_INCREMENT = 100;
ALTER TABLE book_category AUTO_INCREMENT = 100;
ALTER TABLE book AUTO_INCREMENT = 100;

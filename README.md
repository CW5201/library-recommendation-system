# 高校图书馆图书推荐系统的设计与实现

一个面向本科毕业设计演示的完整 Web 系统：在传统高校图书馆管理功能之上，加入个性化推荐、语义检索和基于 RAG 的 AI 图书助手。

系统采用前后端分离架构，不依赖微服务、消息队列或必须外部部署的向量数据库。未配置大模型 API Key 时，图书管理、借阅、传统推荐和本地语义检索仍然可用。

## 一、功能概览

### 普通用户

- 注册、登录、退出登录、修改个人资料、修改密码
- 馆藏图书分页浏览、关键词搜索、分类筛选、多种排序
- 图书详情：书名、作者、出版社、ISBN、分类、简介、封面、馆藏及可借数量
- 浏览记录、收藏 / 取消收藏、评分、借阅、归还
- 首页热门图书、新书推荐、猜你喜欢
- 个性化推荐页：混合推荐、推荐理由、算法标识
- 图书详情页相似图书推荐
- AI 图书助手：自然语言问答、关联图书卡片、会话历史
- AI 语义搜索、AI 阅读清单、AI 推荐理由
- 个人中心：资料、收藏、浏览历史、借阅记录

### 管理员

- 数据概览、热门图书排行、最近借阅
- 用户管理：查询、启用 / 禁用、角色调整、删除
- 图书管理：新增、修改、删除、查询、上下架
- 分类管理：新增、修改、删除
- 借阅管理：查询、归还
- 数据统计：分类分布、借阅趋势、用户行为、热门图书、用户角色、借阅状态
- ECharts 图表展示
- 手动重建图书向量索引

## 二、技术栈

| 层次 | 技术 | 版本 |
| --- | --- | --- |
| 后端 | Java | 17+（本机验证使用 21） |
| 后端 | Spring Boot | 3.5.16 |
| 后端 | MyBatis-Plus | 3.5.18.1 |
| 后端 | Sa-Token | 1.46.0 |
| 后端 | MySQL | 8.0+ |
| 后端 | Maven | 3.9+（项目自带 Maven Wrapper） |
| 前端 | Vue | 3.5.x |
| 前端 | Vite | 6.x |
| 前端 | Element Plus | 2.9.x |
| 前端 | Vue Router / Pinia / Axios | 4.x / 3.x / 1.x |
| 前端 | ECharts | 5.x |
| AI | OpenAI 兼容 Chat Completions / Embeddings 接口 | 可切换供应商 |

## 三、项目结构

```text
.
├─ backend/
│  ├─ .env.example                 # 后端环境变量示例
│  ├─ .mvn/wrapper/                # Maven Wrapper
│  ├─ mvnw / mvnw.cmd
│  ├─ pom.xml
│  └─ src/
│     ├─ main/java/com/library/recommendation/
│     │  ├─ common/                # 统一响应、异常、分数工具
│     │  ├─ config/                # Sa-Token、MyBatis-Plus、AI 配置
│     │  ├─ controller/            # 用户、图书、行为、推荐、AI、管理端接口
│     │  ├─ dto/                   # 请求参数
│     │  ├─ entity/                # 数据库实体
│     │  ├─ mapper/                # MyBatis-Plus Mapper
│     │  ├─ service/               # 业务服务、推荐算法、RAG、向量检索
│     │  ├─ util/                  # 文本特征工具
│     │  └─ vo/                    # 响应对象
│     ├─ main/resources/application.yml
│     └─ test/                     # 推荐算法与工具测试
├─ frontend/
│  ├─ .env.example
│  ├─ package.json
│  └─ src/
│     ├─ api/                      # Axios 封装与接口定义
│     ├─ components/
│     ├─ layouts/
│     ├─ router/
│     ├─ stores/
│     ├─ styles/
│     ├─ utils/
│     └─ views/
│        ├─ user/                  # 普通用户页面
│        └─ admin/                 # 管理后台页面
├─ scripts/api-smoke-test.mjs      # 接口冒烟测试脚本
├─ sql/init.sql                    # 建表、索引、演示数据、默认账号
└─ README.md
```

## 四、快速开始

### 1. 环境要求

- JDK 17 或以上版本，建议 JDK 17 / 21
- MySQL 8.0 或以上版本
- Node.js 18 或以上版本，建议 20 / 22 / 24
- Maven 3.9+（也可以直接使用项目自带的 `mvnw.cmd` / `mvnw`）

确认版本：

```powershell
java -version
node -v
npm -v
```

如果本机默认 JDK 是 8，请先把 `JAVA_HOME` 指向 JDK 17+，并确保 `java` 命令来自该 JDK。Spring Boot 3.5 不能运行在 Java 8 上。

### 2. 初始化数据库

先启动 MySQL，然后执行：

```powershell
mysql -uroot -p < sql/init.sql
```

或者在 MySQL 客户端中执行：

```sql
SOURCE D:/你的项目路径/sql/init.sql;
```

脚本会创建数据库 `library_recommendation`，建立全部表、索引、外键、唯一约束，并写入演示账号、分类、图书、行为、收藏、评分和借阅数据。

> 注意：`sql/init.sql` 开头会删除并重建业务表，重复执行会恢复为初始演示数据。

### 3. 配置后端

复制环境变量示例：

```powershell
Copy-Item backend/.env.example backend/.env
```

默认配置可以直接对应本机常见的 `root/root` 数据库：

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=library_recommendation
DB_USERNAME=root
DB_PASSWORD=root
SERVER_PORT=8080
```

`application.yml` 通过 `spring.config.import: optional:file:./.env[.properties]` 读取后端工作目录下的 `.env` 文件。因此请在 `backend` 目录下启动后端，或者在 IDE 中把工作目录设置为 `backend`。

AI 配置见后文“AI 配置说明”。`.env` 已被 `.gitignore` 排除，不要提交真实密钥。

### 4. 启动后端

Windows：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Linux / macOS：

```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

也可以先打包再运行：

```powershell
cd backend
.\mvnw.cmd clean package
java -jar target/library-recommendation-1.0.0.jar
```

启动成功后接口地址为 `http://127.0.0.1:8080`。

### 5. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。

开发环境下 Vite 会把 `/api` 代理到 `http://127.0.0.1:8080`。生产构建：

```powershell
npm run build
```

构建产物位于 `frontend/dist`。`dev` / `build` / `preview` 脚本会把 Vite 临时目录指向 `frontend/.vite-tmp`，避免 Windows 用户 Temp 目录权限导致 esbuild 构建失败。

## 五、默认演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin@123` | 管理员 |
| `student` | `Student@123` | 普通用户 |
| `reader01` ~ `reader04` | `Demo@123` | 普通用户 |

数据库中密码使用 BCrypt 加密存储，不保存明文。

> 默认密码仅用于本地演示和答辩，部署或正式使用前请立即修改。

## 六、AI 配置说明

后端通过 OpenAI 兼容接口调用大模型，模型供应商可以替换，只要接口格式兼容即可。

在 `backend/.env` 中配置：

```properties
# 大语言模型
AI_ENABLED=true
AI_BASE_URL=https://api.deepseek.com/v1
AI_API_KEY=你的真实密钥
AI_CHAT_MODEL=deepseek-chat
AI_TIMEOUT_SECONDS=60
AI_TEMPERATURE=0.3
AI_MAX_CONTEXT_BOOKS=6

# Embedding（可选）
EMBEDDING_BASE_URL=
EMBEDDING_API_KEY=
EMBEDDING_MODEL=text-embedding-3-small
```

### 未配置 API Key 时

- 系统仍可正常启动。
- 图书管理、分类、借阅、收藏、评分、传统推荐、相似图书等功能不受影响。
- AI 页面会显示明确的“未配置大模型”提示。
- AI 对话会退化为“数据库检索 + 本地摘要”模式，只展示真实馆藏信息。
- 语义检索自动使用内置的本地哈希向量，不需要联网。

### RAG 流程

1. 用户提出问题。
2. 后端使用向量索引从数据库馆藏中检索相关图书。
3. 将图书名称、作者、出版社、ISBN、分类、馆藏数量、可借数量、评分和简介整理为上下文。
4. 将问题与上下文发送给大语言模型。
5. 返回回答，并把真实图书对象作为关联图书卡片返回给前端。

提示词明确要求模型：只能依据检索到的馆藏回答，不得编造书名、作者、ISBN、馆藏数量；检索不到时必须说明“当前馆藏中没有检索到相关图书”。馆藏数量来自数据库，而不是模型推测。

### Embedding 切换

- 不配置 `EMBEDDING_BASE_URL`：使用内置 `local-hash-v2` 向量，离线可演示。
- 配置 OpenAI 兼容的 Embeddings 接口：调用远程向量模型。
- 修改 Embedding 配置后，建议管理员在后台点击“重建向量索引”，或重启后端后重新触发索引检查。否则旧索引可能仍使用本地向量模型。

本项目没有强制引入 Qdrant。当前方案直接使用 MySQL 保存向量 JSON，在 Java 中计算余弦相似度，满足本科毕设的数据规模和演示需求，部署更简单。

## 七、推荐算法说明

推荐核心位于 `backend/src/main/java/com/library/recommendation/service/RecommendationEngine.java`，业务编排位于 `RecommendationService.java`。

### 1. 热门推荐

综合借阅次数、浏览次数、平均评分、评分人数和库存情况，使用对数平滑后按最大值归一化到 `[0,1]`：

```text
hot = 0.35 * log(1 + borrowCount)
    + 0.22 * log(1 + viewCount)
    + 0.23 * (ratingAvg / 5)
    + 0.15 * log(1 + ratingCount)
    + 0.05 * (availableStock / totalStock)
```

### 2. 基于内容的推荐

提取书名、作者、分类、出版社、简介等文本特征，构建用户历史行为画像，计算候选图书与用户画像的余弦相似度，并融合分类偏好：

```text
content = 0.7 * 文本余弦相似度 + 0.3 * 分类偏好
```

### 3. Item-based 协同过滤

将用户对图书的行为权重组织为物品-用户向量，计算物品之间的余弦相似度，再用用户历史物品加权预测候选图书分数。

行为权重：借阅 5、评分 4、收藏 3、浏览 1。

### 4. 混合推荐

```text
finalScore = hotWeight * hotScore
           + contentWeight * contentScore
           + itemCfWeight * itemCfScore
```

默认权重：

```properties
RECOMMEND_WEIGHT_HOT=0.35
RECOMMEND_WEIGHT_CONTENT=0.40
RECOMMEND_WEIGHT_ITEM_CF=0.25
```

各算法先独立归一化，再按权重融合；权重可以通过环境变量调整。

### 5. 冷启动与过滤

- 新用户没有历史行为时，直接使用热门推荐。
- 推荐结果会过滤用户已经收藏或已经借阅的图书。
- 推荐理由根据实际命中的算法和历史行为生成，例如“与你关注的《流畅的Python》同属计算机科学分类，内容主题相关”。
- 推荐结果会写入 `recommendation_log`，便于追踪推荐得分与理由。

## 八、主要接口

统一前缀：`/api`

登录成功后后端返回 Sa-Token，前端在请求头 `satoken` 中携带。

| 模块 | 方法与路径 | 说明 |
| --- | --- | --- |
| 认证 | `POST /auth/register` | 注册 |
| 认证 | `POST /auth/login` | 登录 |
| 认证 | `POST /auth/logout` | 退出 |
| 认证 | `GET /auth/me` | 当前用户 |
| 认证 | `PUT /auth/profile` | 修改资料 |
| 认证 | `PUT /auth/password` | 修改密码 |
| 图书 | `GET /books` | 分页、搜索、分类、排序 |
| 图书 | `GET /books/{id}` | 详情，并记录登录用户浏览行为 |
| 分类 | `GET /categories` | 分类列表 |
| 行为 | `POST /behaviors/view/{bookId}` | 浏览 |
| 行为 | `POST /behaviors/favorite/{bookId}` | 收藏 / 取消收藏 |
| 行为 | `POST /behaviors/rate/{bookId}` | 评分 |
| 行为 | `POST /behaviors/borrow/{bookId}` | 借阅 |
| 行为 | `POST /behaviors/return/{recordId}` | 归还 |
| 推荐 | `GET /recommendations/hot` | 热门推荐 |
| 推荐 | `GET /recommendations/new` | 新书推荐 |
| 推荐 | `GET /recommendations/personal` | 个性化混合推荐 |
| 推荐 | `GET /recommendations/similar/{bookId}` | 相似图书 |
| 个人 | `GET /me/favorites` | 收藏列表 |
| 个人 | `GET /me/history` | 浏览历史 |
| 个人 | `GET /me/borrows` | 借阅记录 |
| AI | `GET /ai/status` | AI 配置与索引状态 |
| AI | `POST /ai/chat` | RAG 问答 |
| AI | `POST /ai/semantic-search` | 语义搜索 |
| AI | `POST /ai/reading-plan` | 阅读清单 |
| AI | `POST /ai/recommend-reason` | 推荐理由 |
| AI | `GET /ai/history` | 对话历史 |
| 管理端 | `/admin/users` | 用户管理 |
| 管理端 | `/admin/books` | 图书管理 |
| 管理端 | `/admin/categories` | 分类管理 |
| 管理端 | `/admin/borrows` | 借阅管理 |
| 管理端 | `/admin/stats/*` | 统计接口 |
| 管理端 | `POST /admin/ai/rebuild-index` | 重建向量索引 |

所有管理端接口均要求 `ADMIN` 角色。

## 九、数据库表

| 表名 | 说明 |
| --- | --- |
| `sys_user` | 用户、密码、角色、状态 |
| `book_category` | 图书分类 |
| `book` | 图书信息与馆藏数量 |
| `user_behavior` | 浏览、收藏、评分、借阅、归还等行为 |
| `user_favorite` | 收藏关系 |
| `book_rating` | 用户评分 |
| `borrow_record` | 借阅记录 |
| `ai_chat_message` | AI 会话消息 |
| `recommendation_log` | 推荐结果、算法、得分和理由 |
| `book_embedding` | 图书文本向量、模型和内容摘要 |

关键约束包括用户名唯一、ISBN 唯一、分类名称唯一、收藏 / 评分用户与图书联合唯一、评分范围检查，以及用户、图书、行为之间的外键关系。

## 十、测试与验证

后端单元测试覆盖推荐算法、推荐服务和分数工具：

```powershell
cd backend
.\mvnw.cmd test
```

当前测试结果：`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。

前端生产构建：

```powershell
cd frontend
npm run build
```

构建成功。

项目还提供了可重复执行的接口冒烟测试脚本，后端启动后运行：

```powershell
node scripts/api-smoke-test.mjs
```

脚本会验证注册登录、图书增删改查、浏览 / 收藏 / 评分 / 借阅 / 归还、个性化推荐、热门推荐、新书推荐、AI 状态、语义搜索、RAG 对话、阅读清单、推荐理由、管理端用户 / 图书 / 借阅 / 统计接口以及普通用户越权访问拦截。

> 脚本会创建并禁用一个测试账号、下架一本测试图书。执行 `sql/init.sql` 可以恢复纯演示数据。

实际联调结果：全部冒烟检查通过；未配置大模型时 AI 对话自动使用数据库检索降级模式。

## 十一、答辩演示流程

1. 使用 `student / Student@123` 登录。
2. 在首页或馆藏列表搜索图书，进入图书详情。
3. 收藏、评分或借阅图书，系统写入真实行为数据。
4. 返回首页查看“猜你喜欢”，或进入个性化推荐页查看推荐结果和推荐理由。
5. 进入 AI 图书助手，询问“我想学习 Java 后端开发，有什么书推荐？”，查看检索到的真实图书卡片。
6. 打开个人中心，查看收藏、浏览历史和借阅记录。
7. 退出后使用 `admin / Admin@123` 登录管理后台。
8. 查看数据概览、用户管理、图书管理、借阅管理。
9. 打开统计分析页查看 ECharts 图表。
10. 如需展示远程 Embedding，配置后点击“重建向量索引”。

## 十二、常见问题

### 后端启动报数据库连接失败

检查 MySQL 是否启动、`backend/.env` 中的 `DB_USERNAME` / `DB_PASSWORD` 是否正确，以及是否已经执行 `sql/init.sql`。

### 后端报 `Unsupported class file major version` 或无法识别 Spring Boot 3

当前使用的不是 JDK 17+。请修改 `JAVA_HOME` 和 IDE Project SDK，确保 Maven 使用的 Java 版本至少为 17。

### 前端请求 401

登录状态失效或未登录。重新登录即可；前端会自动跳转登录页。

### AI 页面提示未配置大模型

这是预期的降级行为。在 `backend/.env` 中配置 `AI_BASE_URL`、`AI_API_KEY`、`AI_CHAT_MODEL` 后重启后端。

### 修改 Embedding 配置后检索结果没有变化

旧索引仍在数据库中使用本地向量。请在管理后台点击“重建向量索引”，或调用 `POST /api/admin/ai/rebuild-index`。

### 端口被占用

后端默认 `8080`，前端默认 `5173`。可通过 `SERVER_PORT` 修改后端端口，并同步修改 `frontend/vite.config.js` 中的代理地址。

## 十三、安全与提交注意事项

- `.env`、`node_modules`、构建产物、日志和 IDE 文件均已加入 `.gitignore`。
- 大模型 API Key 只存在于后端环境变量，不会下发到前端。
- 密码使用 BCrypt 加密存储。
- 接口通过 Sa-Token 校验登录状态，管理端接口额外校验 `ADMIN` 角色。
- 默认演示密码必须在正式使用前修改。
- AI 回答仅作为阅读参考；馆藏数量和可借状态以数据库和图书详情页为准。

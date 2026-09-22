# 企业知识库智能查询与治理系统

这是题目二的实现工程。系统采用模块化单体架构，以 Spring AI、PostgreSQL/pgvector、关键词检索和 RRF 为核心，逐步实现文档导入、混合检索、权限治理、引用回答、评测与可观测性。

## 当前进度

目前已完成：

- Markdown、TXT、PDF、DOCX 解析、清洗、切块和版本去重
- Spring AI 批量 Embedding 与 PostgreSQL/pgvector 持久化
- 向量检索、关键词检索和 RRF 混合检索
- 基于引用的 RAG 回答、上下文组装和 Prompt 注入防护
- JDBC Chat Memory、请求审计、指标和 AI 调用容错
- JWT 身份认证及租户、部门、密级权限过滤
- Spring AI 原生流式回答与 Prompt 数据库版本读取
- 数据库驱动的混合检索离线评测与权限拒答用例
- PostgreSQL、Nacos 与可选 Ollama 的 Docker Compose
- 10 份跨部门、跨权限和跨版本的 Mock 企业知识文档

管理页面将在后续步骤完成。

## 架构原则

- Spring AI 是唯一 AI 主框架，不混用 LangChain4j 主链路。
- PostgreSQL 保存业务数据，pgvector 保存真实向量。
- 关键词与向量召回使用 RRF 融合。
- 租户、部门和权限过滤必须在生成答案前完成。
- 引用由服务端基于真实检索结果生成。
- 默认使用 1024 维 Embedding，数据库向量列与模型维度必须一致。

## 本地基础设施

复制环境变量示例后启动：

```bash
cp .env.example .env
docker compose up -d postgres nacos
```

需要本地模型时：

```bash
docker compose --profile local-ai up -d
```

Nacos 控制台地址为 `http://localhost:8081/nacos/`，PostgreSQL 监听 `localhost:5432`。

## 构建

项目自带 Maven 启动脚本，首次执行会把 Maven 下载到项目的 `.mvn` 目录：

```bash
chmod +x mvnw
./mvnw test
```

## JWT 认证

所有 `/api/**` 接口都要求请求头 `Authorization: Bearer <JWT>`。JWT 使用 HS256 验签，必须包含：

- `iss`：默认是 `enterprise-knowledge-rag`
- `sub`：用户 ID，例如 `zhangsan`
- `tenant_id`：租户 UUID
- `exp`：过期时间
- `scope`：文档准备和导入还需要 `knowledge.write`

启动前必须提供一个至少 32 字节的签名密钥：

```bash
export RAG_JWT_SECRET="$(openssl rand -base64 32)"
```

问答和检索请求体不再接收 `tenantId`、`userId`；文档元数据也不再接收 `tenantId`。这些身份字段只从验签成功的 JWT 中取得，业务层随后再从数据库加载部门和密级权限。

生成一小时有效的普通用户令牌：

```bash
TOKEN="$(./scripts/generate-dev-jwt.sh)"
```

生成具有文档写入权限的令牌：

```bash
WRITE_TOKEN="$(./scripts/generate-dev-jwt.sh \
  zhangsan \
  00000000-0000-0000-0000-000000000001 \
  knowledge.write)"
```

调用检索接口时，请求体只包含业务参数：

```bash
curl -i http://localhost:8080/api/v1/search/vector \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"question":"如何登录 VPN？"}'
```

`generate-dev-jwt.sh` 只用于本地学习和联调。生产环境应由统一身份认证系统登录并签发令牌，业务服务只负责验签。

## 流式回答

同步问答接口仍为 `POST /api/v1/answers`。需要 SSE 事件流时使用：

```bash
curl -N http://localhost:8080/api/v1/answers/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"question":"如何登录 VPN？"}'
```

事件顺序为 `started`、一个或多个 `delta`、`citations`、`completed`。如果生成失败，连接中会收到 `error` 事件。`delta` 直接来自 Spring AI 的模型流；服务端一边转发、一边累积完整答案，只有流正常结束后才在短事务中保存用户消息、完整助手答案和引用。客户端中途断开时会取消模型订阅，并记录取消审计。

模型已经输出第一个片段后不会自动重试，因为重新请求模型可能从头生成，导致前端看到重复或互相矛盾的半段答案。流中断时返回 `error`，由用户明确重新发起请求。

## Prompt 版本

RAG 系统提示词不再写死在 Java 代码中。Flyway 的 `V6` 迁移会向 `prompt_template` 表写入并启用 `rag-answer-system` 的 `v1` 版本，回答服务在有检索证据、准备调用模型时读取当前启用版本。

同步回答和流式回答都会携带实际使用的 `promptVersion`，请求审计也保存同一个版本号。无检索证据时不会调用模型，版本记为 `none`；没有启用的模板时直接失败，不会悄悄退回某个硬编码 Prompt。

Prompt 管理接口需要 JWT 包含 `prompt.manage` scope。先生成管理令牌：

```bash
PROMPT_TOKEN="$(./scripts/generate-dev-jwt.sh \
  admin \
  00000000-0000-0000-0000-000000000001 \
  prompt.manage)"
```

创建新版本只会保存为未启用状态，不会立即改变线上回答：

```bash
curl -i http://localhost:8080/api/v1/admin/prompts/rag-answer-system/versions \
  -H "Authorization: Bearer $PROMPT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"version":"v2","content":"你是企业知识库问答助手……"}'
```

查看所有版本：

```bash
curl http://localhost:8080/api/v1/admin/prompts/rag-answer-system/versions \
  -H "Authorization: Bearer $PROMPT_TOKEN"
```

明确启用 `v2`：

```bash
curl -X PUT \
  http://localhost:8080/api/v1/admin/prompts/rag-answer-system/versions/v2/activate \
  -H "Authorization: Bearer $PROMPT_TOKEN"
```

需要回滚时，对旧版本执行同一个启用接口，例如把路径中的 `v2` 改成 `v1`。启用过程在一个短事务中锁定同一 Prompt 的版本记录，先取消旧版本再启用目标版本；数据库唯一索引同时保证最多只有一个活动版本。

## 离线检索评测

Flyway 的 `V7` 迁移会写入五条固定评测用例，覆盖正常召回、无资料拒答和严格受限资料拒答。运行前需要先导入 `src/main/resources/mock-documents` 中对应的 Mock 文档。

评测使用发起请求者本人的租户、部门和密级权限，因此可以同时检查检索质量和权限过滤。管理令牌需要 `evaluation.run` scope：

```bash
EVALUATION_TOKEN="$(./scripts/generate-dev-jwt.sh \
  zhangsan \
  00000000-0000-0000-0000-000000000001 \
  evaluation.run)"
```

使用默认的 `topK` 和最低相似度运行：

```bash
curl -X POST \
  http://localhost:8080/api/v1/admin/evaluations/retrieval-runs \
  -H "Authorization: Bearer $EVALUATION_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{}'
```

也可以覆盖本次运行参数：

```bash
curl -X POST \
  http://localhost:8080/api/v1/admin/evaluations/retrieval-runs \
  -H "Authorization: Bearer $EVALUATION_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"topK":8,"minScore":0.4}'
```

每道题都会保存召回片段、耗时、`Hit@K`、文档召回率和拒答判断结果。单题失败不会丢弃其他题的结果；运行状态会变为 `COMPLETED_WITH_ERRORS`。汇总结果包含总通过率、拒答判断准确率和可回答问题的文档命中率。

## 答案质量评测

答案评测会真正调用当前 RAG 回答链路，因此会产生模型调用和相应费用。它使用与检索评测相同的 `evaluation.run` 权限和请求参数：

```bash
curl -X POST \
  http://localhost:8080/api/v1/admin/evaluations/answer-runs \
  -H "Authorization: Bearer $EVALUATION_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{}'
```

当前采用确定性规则评测，不再额外调用一个大模型充当裁判：

- 可回答题必须生成有依据的答案，并包含评测用例配置的全部关键事实词。
- 至少一条引用必须来自预期文档。
- 答案正文中的引用编号必须真实存在，且至少一条已引用来源属于预期文档。
- 应拒答题不能被标记为 `grounded`，防止无依据回答。
- 每条结果都会保存完整答案、Prompt 版本、关键词覆盖率、引用检查结果和耗时。

答案评测仍然逐题隔离失败。某一道题模型调用异常时，其余题继续执行，已经完成的结果不会丢失。

## 评测历史与对比

查看当前租户最近 20 次评测：

```bash
curl 'http://localhost:8080/api/v1/admin/evaluations/runs?limit=20' \
  -H "Authorization: Bearer $EVALUATION_TOKEN"
```

查看一次运行的完整逐题结果：

```bash
curl http://localhost:8080/api/v1/admin/evaluations/runs/<runId> \
  -H "Authorization: Bearer $EVALUATION_TOKEN"
```

比较两次同类型运行，其中 `baselineRunId` 是改动前，`candidateRunId` 是改动后：

```bash
curl 'http://localhost:8080/api/v1/admin/evaluations/comparisons?baselineRunId=<旧运行ID>&candidateRunId=<新运行ID>' \
  -H "Authorization: Bearer $EVALUATION_TOKEN"
```

对比结果分别给出旧值、候选值和 `delta`。例如通过率从 `0.6` 提升到 `0.8` 时，`delta` 为 `0.2`。答案评测会从逐题结果汇总实际使用的 Prompt 版本；如果一次运行中出现多个版本，会全部列出，提示该次结果不适合作为纯粹的单版本对照实验。

历史查询始终按 JWT 的租户隔离。检索评测和答案评测不能互相比较，尚未完成的运行也不能参与比较。

## Mock 文档

`src/main/resources/mock-documents` 包含财务、采购、IT、HR、生产、售后和安全等文档，特意设置了版本冲突、部门权限和无答案测试场景。

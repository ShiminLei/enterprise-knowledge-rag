# 第一阶段架构基线

## 总体形态

项目采用单 Maven 模块、领域化分包的模块化单体。所有核心能力运行在同一个 Spring Boot 进程中，PostgreSQL/pgvector 是主数据与向量存储，Nacos 提供动态业务参数，Ollama 是可选本地模型运行环境。

## 计划中的请求链路

1. API 层解析租户、用户、部门与权限上下文。
2. 查询理解模块生成规范化查询和过滤条件。
3. BM25 与向量检索并行召回。
4. RRF 合并结果，执行权限、版本和业务过滤。
5. Reranker 生成最终上下文。
6. Prompt 服务加载指定版本模板。
7. Spring AI 调用模型并生成结构化结果。
8. 引用服务以真实 Chunk 为准构造引用。
9. 治理模块计算置信度、冲突和无答案原因。
10. 审计、指标和评测模块记录结果。

## 数据边界

- `tenant` 与 `tenant_user_permission`：租户和访问权限。
- `knowledge_document`：原始文档级元数据和处理状态。
- `knowledge_chunk`：切分文本、引用位置、过滤字段和向量。
- `ingestion_job`：摄取任务状态与错误。
- `conversation*`：对话及 Chat Memory 持久化。
- `prompt_template`：Prompt 版本、激活和回滚。
- `evaluation*`：评测用例、运行和结果。
- `rag_request_audit`：不保存敏感问题原文的请求审计。

## 关键约束

- Embedding 固定为 1024 维；更换维度必须执行数据库迁移和全量重建索引。
- 文档内容与向量均必须携带 tenantId 和 permissionLevel。
- 所有生成用上下文必须来自过滤后的检索结果。
- 旧版本文档保留用于追溯，但默认不得参与当前答案生成。
- Prompt、检索参数和模型参数都需要记录到评测运行配置中。

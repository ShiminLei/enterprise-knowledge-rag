# 功能验收矩阵

| 编号 | 功能 | 主要模块 | 预期验收证据 |
|---|---|---|---|
| 1 | Mock 文档管理 | document | 10 份跨部门、跨权限、跨版本文档 |
| 2 | 解析与清洗 | ingestion | Markdown/TXT/PDF/DOCX 导入结果与清洗日志 |
| 3 | Chunk 与元数据 | chunking | Chunk 列表包含版本、页码、权限等元数据 |
| 4 | Embedding 与向量库 | embedding | pgvector 真实向量及 HNSW 索引 |
| 5 | 检索链路 | retrieval | pgvector + 中文双字分词 Okapi BM25 + RRF 混合结果 |
| 6 | 重排和过滤 | retrieval/security | 双路命中奖励的二阶段重排，以及租户/部门/密级/分类过滤 |
| 7 | Prompt 模板 | prompt | 独立模板、版本号与激活记录 |
| 8 | 结构化输出 | generation | answer、citations、retrievedChunks、confidence、cannotAnswerReason |
| 9 | 引用回答 | citation | 标题、版本、Chunk、页码、段落范围、原文摘录和可用来源链接 |
| 10 | 基础评测 | evaluation | 20 条问题、检索/答案评测、历史对比及 Markdown 报告 |
| 11 | 异常与日志 | common/observability | 统一错误码、requestId、耗时和状态 |
| 12 | 可运行接口 | api | 导入、问答、评测和管理页面 |

## 拔高项

| 加分项 | 实现策略 |
|---|---|
| BM25 + 向量混合检索 | 权限过滤后用 Okapi BM25 与 pgvector 分路召回，再使用 RRF 融合 |
| Reranker | RRF 后按双路命中、语义分数和关键词分数做确定性二阶段业务重排 |
| Chat Memory | JDBC 持久化并按租户/用户/会话隔离 |
| Streaming | SSE 分阶段事件和最终结构化事件 |
| 多租户权限 | tenantId、department、permissionLevel 三层过滤 |
| Prompt 版本管理 | 数据库版本、激活、回滚和审计 |
| Nacos 配置 | 监听并热更新两路 TopK、最终 TopK、阈值与 RRF 参数；非法发布自动保留旧值 |
| Resilience4j | 模型和 Embedding 调用重试、限流、熔断、超时 |
| Micrometer | `rag.answer.requests` 按结果计数，`rag.answer.duration` 按结果统计耗时；Prometheus 端点导出 |
| 自定义 RAG 评测 | Hit@K、MRR、引用正确率、无答案准确率等 |
| 管理页面 | 文档、问答、评测、Prompt、指标页签 |
| Docker Compose | 多阶段应用镜像 + PostgreSQL + Nacos + 可选 Ollama，一条命令启动 |
| Markdown 报告 | 按租户导出单次批量评测的汇总与逐题 Markdown 报告 |

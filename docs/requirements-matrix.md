# 功能验收矩阵

| 编号 | 功能 | 主要模块 | 预期验收证据 |
|---|---|---|---|
| 1 | Mock 文档管理 | document | 10 份跨部门、跨权限、跨版本文档 |
| 2 | 解析与清洗 | ingestion | Markdown/TXT/PDF/DOCX 导入结果与清洗日志 |
| 3 | Chunk 与元数据 | chunking | Chunk 列表包含版本、页码、权限等元数据 |
| 4 | Embedding 与向量库 | embedding | pgvector 真实向量及 HNSW 索引 |
| 5 | 检索链路 | retrieval | Vector、BM25、Hybrid 三模式调试结果 |
| 6 | 重排和过滤 | rerank/security | 排名前后对比及权限过滤证据 |
| 7 | Prompt 模板 | prompt | 独立模板、版本号与激活记录 |
| 8 | 结构化输出 | generation | DTO、校验与模型格式异常降级 |
| 9 | 引用回答 | citation | 标题、版本、Chunk、页码和原文摘录 |
| 10 | 基础评测 | evaluation | 至少 20 条问题及 Markdown 评测报告 |
| 11 | 异常与日志 | common/observability | 统一错误码、requestId、耗时和状态 |
| 12 | 可运行接口 | api | 导入、问答、评测和管理页面 |

## 拔高项

| 加分项 | 实现策略 |
|---|---|
| BM25 + 向量混合检索 | 并行召回后使用 RRF 融合 |
| Reranker | 内置业务重排并支持远程模型扩展 |
| Chat Memory | JDBC 持久化并按租户/用户/会话隔离 |
| Streaming | SSE 分阶段事件和最终结构化事件 |
| 多租户权限 | tenantId、department、permissionLevel 三层过滤 |
| Prompt 版本管理 | 数据库版本、激活、回滚和审计 |
| Nacos 配置 | 动态管理 TopK、阈值、模型与限流参数 |
| Resilience4j | 模型和 Embedding 调用重试、限流、熔断、超时 |
| Micrometer | 导入、检索、模型、失败率、无答案率指标 |
| 自定义 RAG 评测 | Hit@K、MRR、引用正确率、无答案准确率等 |
| 管理页面 | 文档、问答、评测、Prompt、指标页签 |
| Docker Compose | PostgreSQL、Nacos、可选 Ollama，最终加入应用镜像 |
| Markdown 报告 | 单次查询报告与批量评测报告 |

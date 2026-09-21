# 企业知识库智能查询与治理系统

这是题目二的实现工程。系统采用模块化单体架构，以 Spring AI、PostgreSQL/pgvector、BM25 和 RRF 为核心，覆盖文档导入、混合检索、权限治理、结构化回答、引用、评测、可观测性与全部拔高项。

## 当前进度

第一阶段已完成：

- Maven/Spring Boot 工程骨架
- 核心领域模型
- PostgreSQL/pgvector 数据库迁移
- PostgreSQL、Nacos 与可选 Ollama 的 Docker Compose
- 10 份 Mock 企业知识文档
- 基础配置和包结构
- Maven Wrapper 启动脚本
- 架构基线与功能验收矩阵

尚未实现文档摄取、检索、问答等业务逻辑；这些将在后续阶段逐步完成。

## 架构原则

- Spring AI 是唯一 AI 主框架，不混用 LangChain4j 主链路。
- PostgreSQL 保存业务数据，pgvector 保存真实向量。
- BM25 与向量并行召回，使用 RRF 融合。
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

## Mock 文档

`src/main/resources/mock-documents` 包含财务、采购、IT、HR、生产、售后和安全等文档，特意设置了版本冲突、部门权限和无答案测试场景。

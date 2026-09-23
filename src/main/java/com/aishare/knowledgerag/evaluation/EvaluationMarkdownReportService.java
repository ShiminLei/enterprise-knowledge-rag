package com.aishare.knowledgerag.evaluation;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class EvaluationMarkdownReportService {

    private final EvaluationHistoryService historyService;

    public EvaluationMarkdownReportService(EvaluationHistoryService historyService) {
        this.historyService = historyService;
    }

    public String render(UUID tenantId, UUID runId) {
        EvaluationRunDetails details = historyService.details(tenantId, runId);
        EvaluationRunRecord run = details.run();
        EvaluationRunSummary summary = run.summary();
        StringBuilder markdown = new StringBuilder()
                .append("# RAG 评测报告\n\n")
                .append("- 运行 ID：`").append(run.runId()).append("`\n")
                .append("- 类型：").append(escape(run.type())).append("\n")
                .append("- 状态：").append(escape(run.status())).append("\n")
                .append("- 开始时间：").append(run.startedAt()).append("\n")
                .append("- 完成时间：").append(run.completedAt()).append("\n")
                .append("- Prompt 版本：").append(details.promptVersions()).append("\n\n")
                .append("## 汇总\n\n")
                .append("| 总数 | 通过 | 失败 | 异常 | 通过率 | 决策准确率 | 可回答命中率 |\n")
                .append("|---:|---:|---:|---:|---:|---:|---:|\n")
                .append("|").append(summary.totalCases())
                .append("|").append(summary.passedCases())
                .append("|").append(summary.failedCases())
                .append("|").append(summary.errorCases())
                .append("|").append(percent(summary.passRate()))
                .append("|").append(percent(summary.decisionAccuracy()))
                .append("|").append(percent(summary.answerableHitRate())).append("|\n\n")
                .append("## 用例明细\n\n")
                .append("| 用例 | 结果 | 耗时(ms) | 问题 | 指标 | 错误 |\n")
                .append("|---|---|---:|---|---|---|\n");
        for (EvaluationCaseResult result : details.results()) {
            markdown.append("|").append(escape(result.caseKey()))
                    .append("|").append(result.passed() ? "通过" : "失败")
                    .append("|").append(result.latencyMs())
                    .append("|").append(escape(result.question()))
                    .append("|").append(escape(formatMetrics(result.metrics())))
                    .append("|").append(escape(result.errorMessage())).append("|\n");
        }
        return markdown.toString();
    }

    private String formatMetrics(Map<String, Object> metrics) {
        return metrics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String percent(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f%%", value * 100);
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
                .replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ");
    }
}

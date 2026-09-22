package com.aishare.knowledgerag.evaluation;

import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EvaluationHistoryService {

    private final EvaluationRepository repository;

    public EvaluationHistoryService(EvaluationRepository repository) {
        this.repository = repository;
    }

    public List<EvaluationRunRecord> list(UUID tenantId, int limit) {
        return repository.findRuns(tenantId, limit);
    }

    public EvaluationRunDetails details(UUID tenantId, UUID runId) {
        EvaluationRunRecord run = findOwned(tenantId, runId);
        List<EvaluationCaseResult> results = repository.findResults(runId);
        return new EvaluationRunDetails(run, promptVersions(results), results);
    }

    public EvaluationRunComparison compare(
            UUID tenantId,
            UUID baselineRunId,
            UUID candidateRunId
    ) {
        EvaluationRunDetails baseline = details(tenantId, baselineRunId);
        EvaluationRunDetails candidate = details(tenantId, candidateRunId);
        if (!baseline.run().type().equals(candidate.run().type())) {
            throw new EvaluationComparisonException(
                    "只能比较相同类型的评测运行: "
                            + baseline.run().type() + " 与 " + candidate.run().type()
            );
        }
        requireCompleted(baseline.run());
        requireCompleted(candidate.run());
        EvaluationRunSummary before = baseline.run().summary();
        EvaluationRunSummary after = candidate.run().summary();
        return new EvaluationRunComparison(
                baselineRunId,
                candidateRunId,
                baseline.run().type(),
                baseline.promptVersions(),
                candidate.promptVersions(),
                EvaluationMetricComparison.between(
                        before.passRate(), after.passRate()),
                EvaluationMetricComparison.between(
                        before.decisionAccuracy(), after.decisionAccuracy()),
                EvaluationMetricComparison.between(
                        before.answerableHitRate(), after.answerableHitRate())
        );
    }

    private EvaluationRunRecord findOwned(UUID tenantId, UUID runId) {
        return repository.findRun(tenantId, runId)
                .orElseThrow(() -> new EvaluationRunNotFoundException(
                        "评测运行不存在或无权访问: " + runId
                ));
    }

    private Set<String> promptVersions(List<EvaluationCaseResult> results) {
        Set<String> versions = new LinkedHashSet<>();
        for (EvaluationCaseResult result : results) {
            Object value = result.metrics().get("promptVersion");
            if (value != null
                    && !value.toString().equals("none")
                    && !value.toString().equals("unknown")) {
                versions.add(value.toString());
            }
        }
        return Set.copyOf(versions);
    }

    private void requireCompleted(EvaluationRunRecord run) {
        if (!run.status().startsWith("COMPLETED")) {
            throw new EvaluationComparisonException(
                    "评测运行尚未完成，不能比较: " + run.runId()
            );
        }
    }
}

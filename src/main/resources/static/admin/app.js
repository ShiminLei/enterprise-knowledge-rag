const state = {
    token: sessionStorage.getItem("rag.admin.token") || "",
    prompts: [],
    runs: []
};

const views = {
    overview: ["overviewView", "运行概览"],
    documents: ["documentsView", "文档导入"],
    prompts: ["promptsView", "Prompt 版本"],
    evaluations: ["evaluationsView", "质量评测"]
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatDate(value) {
    if (!value) return "—";
    return new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
    }).format(new Date(value));
}

function percent(value) {
    return Number.isFinite(Number(value)) ? `${Math.round(Number(value) * 100)}%` : "—";
}

function toast(message, isError = false) {
    const item = document.createElement("div");
    item.className = `toast${isError ? " error" : ""}`;
    item.textContent = message;
    $("#toastRegion").append(item);
    setTimeout(() => item.remove(), 4200);
}

async function api(path, options = {}) {
    if (!state.token) {
        openTokenDialog();
        throw new Error("请先设置访问令牌");
    }
    const headers = new Headers(options.headers || {});
    headers.set("Authorization", `Bearer ${state.token}`);
    if (options.body && !(options.body instanceof FormData)) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(path, {...options, headers});
    if (!response.ok) {
        let body = {};
        try { body = await response.json(); } catch (_) { /* ignore non-json response */ }
        if (response.status === 401) openTokenDialog();
        throw new Error(body.message || body.code || `请求失败 (${response.status})`);
    }
    if (response.status === 204) return null;
    return response.json();
}

function setBusy(button, busy, text) {
    if (!button) return;
    if (busy) {
        button.dataset.originalText = button.textContent;
        button.textContent = text || "处理中";
        button.classList.add("loading");
        button.disabled = true;
    } else {
        button.textContent = button.dataset.originalText || button.textContent;
        button.classList.remove("loading");
        button.disabled = false;
    }
}

function showView(name) {
    const view = views[name] || views.overview;
    $$(".view").forEach((element) => element.classList.remove("active"));
    $$(".nav-item").forEach((element) => element.classList.toggle("active", element.dataset.view === name));
    $(`#${view[0]}`).classList.add("active");
    $("#pageTitle").textContent = view[1];
}

function tokenClaims() {
    try {
        let part = state.token.split(".")[1].replaceAll("-", "+").replaceAll("_", "/");
        part = part.padEnd(Math.ceil(part.length / 4) * 4, "=");
        return JSON.parse(atob(part));
    } catch (_) {
        return {};
    }
}

function updateTokenStatus() {
    const claims = tokenClaims();
    const connected = Boolean(state.token);
    $("#tokenStatusDot").classList.toggle("connected", connected);
    $("#tokenStatusText").textContent = connected
        ? `${claims.sub || "已连接"} · ${claims.scope || "普通权限"}`
        : "尚未连接";
}

function openTokenDialog() {
    $("#tokenInput").value = state.token;
    const dialog = $("#tokenDialog");
    if (!dialog.open) dialog.showModal();
}

async function loadPrompts() {
    state.prompts = await api("/api/v1/admin/prompts/rag-answer-system/versions");
    renderPrompts();
    renderOverview();
}

function renderPrompts() {
    const list = $("#promptList");
    if (!state.prompts.length) {
        list.className = "stack-list empty-state";
        list.textContent = "还没有 Prompt 版本";
        return;
    }
    list.className = "stack-list";
    list.innerHTML = state.prompts.map((item) => `
        <article class="stack-item ${item.active ? "active-version" : ""}">
            <div class="stack-item-header">
                <div><strong>${escapeHtml(item.version)}</strong><small>${formatDate(item.createdAt)} · ${escapeHtml(item.createdBy)}</small></div>
                ${item.active
                    ? '<span class="pill success">当前启用</span>'
                    : `<button class="secondary-button activate-prompt" data-version="${escapeHtml(item.version)}" type="button">启用</button>`}
            </div>
            <p>${escapeHtml(item.content.length > 230 ? `${item.content.slice(0, 230)}…` : item.content)}</p>
            <small>SHA-256 · ${escapeHtml(item.checksum.slice(0, 16))}…</small>
        </article>`).join("");
}

async function loadRuns() {
    state.runs = await api("/api/v1/admin/evaluations/runs?limit=50");
    renderRuns();
    renderOverview();
}

function typeLabel(type) {
    return type === "RAG_ANSWER" ? "答案评测" : type === "HYBRID_RETRIEVAL" ? "检索评测" : type;
}

function statusPill(status) {
    const className = status === "COMPLETED" ? "success" : status === "RUNNING" ? "warning" : "danger";
    return `<span class="pill ${className}">${escapeHtml(status)}</span>`;
}

function renderRuns() {
    const body = $("#runsTableBody");
    if (!state.runs.length) {
        body.innerHTML = '<tr><td colspan="6" class="empty-state">还没有评测运行</td></tr>';
    } else {
        body.innerHTML = state.runs.map((run) => `
            <tr>
                <td>${formatDate(run.startedAt)}</td>
                <td>${escapeHtml(typeLabel(run.configuration.type))}</td>
                <td>${statusPill(run.status)}</td>
                <td>${percent(run.summary.passRate)}</td>
                <td>${percent(run.summary.decisionAccuracy)}</td>
                <td><button class="text-button view-run" data-run-id="${run.runId}" type="button">查看详情</button></td>
            </tr>`).join("");
    }
    const options = ['<option value="">请选择</option>'].concat(state.runs
        .filter((run) => run.status.startsWith("COMPLETED"))
        .map((run) => `<option value="${run.runId}">${escapeHtml(typeLabel(run.configuration.type))} · ${formatDate(run.startedAt)} · ${percent(run.summary.passRate)}</option>`))
        .join("");
    $("#baselineRun").innerHTML = options;
    $("#candidateRun").innerHTML = options;
}

function renderOverview() {
    const activePrompt = state.prompts.find((item) => item.active);
    const latest = state.runs[0];
    $("#activePromptMetric").textContent = activePrompt?.version || "—";
    $("#runCountMetric").textContent = state.runs.length ? String(state.runs.length) : "0";
    $("#passRateMetric").textContent = latest ? percent(latest.summary.passRate) : "—";
    $("#passRateCaption").textContent = latest ? `${typeLabel(latest.configuration.type)} · ${formatDate(latest.startedAt)}` : "等待评测数据";
    $("#systemStatusMetric").textContent = state.token ? "已连接" : "待连接";

    const recent = $("#recentRuns");
    if (!state.runs.length) {
        recent.className = "compact-list empty-state";
        recent.textContent = "暂无评测运行";
        return;
    }
    recent.className = "compact-list";
    recent.innerHTML = state.runs.slice(0, 4).map((run) => `
        <div class="compact-item">
            <div><strong>${escapeHtml(typeLabel(run.configuration.type))}</strong><small>${formatDate(run.startedAt)}</small></div>
            <strong>${percent(run.summary.passRate)}</strong>
        </div>`).join("");
}

async function refreshAll() {
    if (!state.token) return openTokenDialog();
    const button = $("#refreshButton");
    setBusy(button, true, "刷新中");
    const results = await Promise.allSettled([loadPrompts(), loadRuns()]);
    const failures = results.filter((result) => result.status === "rejected");
    if (failures.length) toast(failures[0].reason.message, true);
    else toast("数据已刷新");
    setBusy(button, false);
}

async function activatePrompt(version, button) {
    if (!confirm(`确认启用 Prompt ${version}？当前活动版本会被停用。`)) return;
    setBusy(button, true, "启用中");
    try {
        await api(`/api/v1/admin/prompts/rag-answer-system/versions/${encodeURIComponent(version)}/activate`, {method: "PUT"});
        toast(`Prompt ${version} 已启用`);
        await loadPrompts();
    } catch (error) {
        toast(error.message, true);
    } finally {
        setBusy(button, false);
    }
}

async function runEvaluation(type, button) {
    const topK = Number($("#evaluationTopK").value);
    const minScore = Number($("#evaluationMinScore").value);
    const endpoint = type === "answer" ? "answer-runs" : "retrieval-runs";
    const label = type === "answer" ? "答案评测" : "检索评测";
    if (type === "answer" && !confirm("答案评测会真实调用模型并可能产生费用，确认继续？")) return;
    setBusy(button, true, "运行中");
    try {
        const report = await api(`/api/v1/admin/evaluations/${endpoint}`, {
            method: "POST", body: JSON.stringify({topK, minScore})
        });
        toast(`${label}完成，通过率 ${percent(report.summary.passRate)}`);
        await loadRuns();
        await showRunDetails(report.runId);
    } catch (error) {
        toast(error.message, true);
    } finally {
        setBusy(button, false);
    }
}

async function showRunDetails(runId) {
    const container = $("#runDetails");
    container.className = "loading";
    container.textContent = "加载详情";
    try {
        const details = await api(`/api/v1/admin/evaluations/runs/${runId}`);
        container.className = "";
        const versions = details.promptVersions?.length ? details.promptVersions.join(", ") : "不涉及";
        container.innerHTML = `
            <div class="compact-item"><div><strong>${escapeHtml(typeLabel(details.run.configuration.type))}</strong><small>Prompt ${escapeHtml(versions)}</small></div><strong>${percent(details.run.summary.passRate)}</strong></div>
            ${details.results.map((result) => `
                <details class="case-result">
                    <summary>${result.passed ? "✓" : "×"} ${escapeHtml(result.caseKey)} · ${result.latencyMs}ms</summary>
                    <p>${escapeHtml(result.question)}</p>
                    ${result.answer ? `<pre>${escapeHtml(result.answer)}</pre>` : ""}
                    <pre>${escapeHtml(JSON.stringify(result.metrics, null, 2))}</pre>
                </details>`).join("")}`;
    } catch (error) {
        container.className = "empty-state";
        container.textContent = error.message;
        toast(error.message, true);
    }
}

async function compareRuns(button) {
    const baseline = $("#baselineRun").value;
    const candidate = $("#candidateRun").value;
    if (!baseline || !candidate) return toast("请选择基准运行和候选运行", true);
    setBusy(button, true, "对比中");
    try {
        const query = new URLSearchParams({baselineRunId: baseline, candidateRunId: candidate});
        const comparison = await api(`/api/v1/admin/evaluations/comparisons?${query}`);
        const rows = [
            ["总通过率", comparison.passRate],
            ["拒答判断准确率", comparison.decisionAccuracy],
            ["预期文档命中率", comparison.answerableHitRate]
        ];
        $("#comparisonResult").className = "comparison-grid";
        $("#comparisonResult").innerHTML = `
            <p><strong>${escapeHtml(typeLabel(comparison.evaluationType))}</strong><br><small>Prompt ${escapeHtml(comparison.baselinePromptVersions.join(", ") || "—")} → ${escapeHtml(comparison.candidatePromptVersions.join(", ") || "—")}</small></p>
            ${rows.map(([label, metric]) => `
                <div class="comparison-row">
                    <div><strong>${label}</strong><small>${percent(metric.baseline)} → ${percent(metric.candidate)}</small></div>
                    <strong class="${metric.delta >= 0 ? "delta-positive" : "delta-negative"}">${metric.delta >= 0 ? "+" : ""}${Math.round(metric.delta * 100)}%</strong>
                </div>`).join("")}`;
    } catch (error) {
        toast(error.message, true);
    } finally {
        setBusy(button, false);
    }
}

$$('.nav-item').forEach((button) => button.addEventListener("click", () => showView(button.dataset.view)));
$$('[data-go]').forEach((button) => button.addEventListener("click", () => showView(button.dataset.go)));
$("#changeTokenButton").addEventListener("click", openTokenDialog);
$("#refreshButton").addEventListener("click", refreshAll);
$("#reloadPromptsButton").addEventListener("click", () => loadPrompts().catch((error) => toast(error.message, true)));
$("#reloadRunsButton").addEventListener("click", () => loadRuns().catch((error) => toast(error.message, true)));
$("#runRetrievalButton").addEventListener("click", (event) => runEvaluation("retrieval", event.currentTarget));
$("#runAnswerButton").addEventListener("click", (event) => runEvaluation("answer", event.currentTarget));
$("#compareRunsButton").addEventListener("click", (event) => compareRuns(event.currentTarget));

$("#promptList").addEventListener("click", (event) => {
    const button = event.target.closest(".activate-prompt");
    if (button) activatePrompt(button.dataset.version, button);
});
$("#runsTableBody").addEventListener("click", (event) => {
    const button = event.target.closest(".view-run");
    if (button) showRunDetails(button.dataset.runId);
});

$("#tokenForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    state.token = $("#tokenInput").value.trim();
    if (!state.token) return;
    sessionStorage.setItem("rag.admin.token", state.token);
    $("#tokenDialog").close();
    updateTokenStatus();
    await refreshAll();
});
$("#clearTokenButton").addEventListener("click", () => {
    state.token = "";
    state.prompts = [];
    state.runs = [];
    sessionStorage.removeItem("rag.admin.token");
    $("#tokenInput").value = "";
    updateTokenStatus();
    renderPrompts(); renderRuns(); renderOverview();
    toast("访问令牌已清除");
});

$("#promptForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = event.submitter;
    const data = new FormData(event.currentTarget);
    setBusy(button, true, "保存中");
    try {
        await api("/api/v1/admin/prompts/rag-answer-system/versions", {
            method: "POST",
            body: JSON.stringify({version: data.get("version"), content: data.get("content")})
        });
        event.currentTarget.reset();
        toast("Prompt 版本已保存，尚未启用");
        await loadPrompts();
    } catch (error) {
        toast(error.message, true);
    } finally {
        setBusy(button, false);
    }
});

const fileInput = $("#documentFile");
fileInput.addEventListener("change", () => {
    $("#selectedFileName").textContent = fileInput.files[0]?.name || "支持 Markdown、TXT、PDF、DOCX，最大 20MB";
});
["dragenter", "dragover"].forEach((name) => $("#fileDrop").addEventListener(name, () => $("#fileDrop").classList.add("dragging")));
["dragleave", "drop"].forEach((name) => $("#fileDrop").addEventListener(name, () => $("#fileDrop").classList.remove("dragging")));

$("#documentForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = event.submitter;
    const values = new FormData(event.currentTarget);
    const file = fileInput.files[0];
    if (!file) return toast("请选择文档", true);
    const metadata = {
        externalDocumentId: values.get("externalDocumentId"),
        title: values.get("title"), source: values.get("source"),
        category: values.get("category"), version: values.get("version"),
        updatedAt: new Date(values.get("updatedAt")).toISOString(),
        permissionLevel: values.get("permissionLevel"), department: values.get("department")
    };
    const body = new FormData();
    body.append("file", file);
    body.append("metadata", new Blob([JSON.stringify(metadata)], {type: "application/json"}));
    setBusy(button, true, "导入中");
    try {
        const result = await api("/api/v1/documents/import", {method: "POST", body});
        const box = $("#documentResult");
        box.classList.remove("hidden");
        box.textContent = `导入结果：${result.outcome}\n文档 ID：${result.documentId}\n切块数量：${result.chunkCount}`;
        toast("文档处理完成");
    } catch (error) {
        toast(error.message, true);
    } finally {
        setBusy(button, false);
    }
});

$("#documentForm [name=updatedAt]").value = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16);
updateTokenStatus();
renderOverview();
if (state.token) refreshAll(); else openTokenDialog();

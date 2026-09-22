package com.aishare.knowledgerag.answer;

import com.aishare.knowledgerag.conversation.ConversationTurn;
import com.aishare.knowledgerag.conversation.MessageRole;
import com.aishare.knowledgerag.retrieval.HybridSearchResult;
import com.aishare.knowledgerag.retrieval.HybridSearchService;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.prompt.PromptTemplate;
import com.aishare.knowledgerag.prompt.PromptTemplateService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagAnswerService {

    private static final String NO_EVIDENCE_ANSWER =
            "根据当前可访问的企业知识库资料，无法找到足够依据回答这个问题。";

    private final HybridSearchService hybridSearchService;
    private final ChatGateway chatGateway;
    private final AnswerProperties properties;
    private final PromptTemplateService promptTemplateService;

    public RagAnswerService(
            HybridSearchService hybridSearchService,
            ChatGateway chatGateway,
            AnswerProperties properties,
            PromptTemplateService promptTemplateService
    ) {
        this.hybridSearchService = hybridSearchService;
        this.chatGateway = chatGateway;
        this.properties = properties;
        this.promptTemplateService = promptTemplateService;
    }

    public GroundedAnswer answer(VectorSearchQuery query) {
        return answer(query, List.of());
    }

    public GroundedAnswer answer(
            VectorSearchQuery query,
            List<ConversationTurn> history
    ) {
        AnswerPlan plan = prepare(query, history);
        String answer = plan.grounded()
                ? chatGateway.generate(plan.prompt().content(), plan.userPrompt())
                : NO_EVIDENCE_ANSWER;
        return new GroundedAnswer(
                answer, plan.grounded(), plan.retrievedCount(), plan.citations(),
                plan.promptVersion()
        );
    }

    public RagAnswerStream stream(
            VectorSearchQuery query,
            List<ConversationTurn> history
    ) {
        AnswerPlan plan = prepare(query, history);
        Flux<String> content = plan.grounded()
                ? chatGateway.stream(plan.prompt().content(), plan.userPrompt())
                : Flux.just(NO_EVIDENCE_ANSWER);
        return new RagAnswerStream(
                plan.grounded(), plan.retrievedCount(), plan.citations(),
                plan.promptVersion(), content
        );
    }

    private AnswerPlan prepare(
            VectorSearchQuery query,
            List<ConversationTurn> history
    ) {
        List<HybridSearchResult> retrieved = hybridSearchService.search(
                contextualizeForRetrieval(query, history)
        );
        if (retrieved.isEmpty()) {
            return new AnswerPlan(false, 0, List.of(), "", null);
        }

        PromptTemplate prompt = promptTemplateService.activeRagAnswerPrompt();
        ContextBundle context = buildContext(retrieved);
        String conversationHistory = buildConversationHistory(history);
        String userPrompt = """
                <conversation_history>
                %s
                </conversation_history>

                <question>
                %s
                </question>

                <sources>
                %s
                </sources>
                """.formatted(conversationHistory, safe(query.question()), context.text());
        return new AnswerPlan(true, retrieved.size(), context.citations(), userPrompt, prompt);
    }

    private VectorSearchQuery contextualizeForRetrieval(
            VectorSearchQuery query,
            List<ConversationTurn> history
    ) {
        for (int index = history.size() - 1; index >= 0; index--) {
            ConversationTurn turn = history.get(index);
            if (turn.role() == MessageRole.USER) {
                return new VectorSearchQuery(
                        "上一问题：" + turn.content() + "\n当前问题：" + query.question(),
                        query.accessContext(),
                        query.category(),
                        query.topK(),
                        query.minScore()
                );
            }
        }
        return query;
    }

    private String buildConversationHistory(List<ConversationTurn> history) {
        StringBuilder result = new StringBuilder();
        for (ConversationTurn turn : history) {
            result.append("<message role=\"")
                    .append(turn.role().name())
                    .append("\">")
                    .append(safe(turn.content()))
                    .append("</message>\n");
        }
        return result.toString();
    }

    private ContextBundle buildContext(List<HybridSearchResult> results) {
        StringBuilder context = new StringBuilder();
        List<AnswerCitation> citations = new ArrayList<>();
        for (HybridSearchResult result : results) {
            RetrievedChunk chunk = result.chunk();
            int marker = citations.size() + 1;
            String header = """
                    <source id="[%d]" documentId="%s" title="%s" page="%s" version="%s">
                    """.formatted(
                    marker,
                    chunk.documentId(),
                    safe(chunk.titlePath()),
                    chunk.pageNumber() == null ? "" : chunk.pageNumber(),
                    safe(chunk.documentVersion())
            );
            String footer = "\n</source>\n";
            int available = properties.maxContextCharacters()
                    - context.length() - header.length() - footer.length();
            if (available <= 0) {
                break;
            }
            String escapedContent = safe(chunk.content());
            String content = escapedContent.length() <= available
                    ? escapedContent
                    : escapedContent.substring(0, available);
            context.append(header).append(content).append(footer);
            citations.add(new AnswerCitation(
                    marker,
                    chunk.chunkId(),
                    chunk.documentId(),
                    chunk.titlePath(),
                    chunk.pageNumber(),
                    chunk.source(),
                    chunk.documentVersion()
            ));
            if (content.length() < escapedContent.length()) {
                break;
            }
        }
        return new ContextBundle(context.toString(), citations);
    }

    private String safe(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private record ContextBundle(String text, List<AnswerCitation> citations) {
    }

    private record AnswerPlan(
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            String userPrompt,
            PromptTemplate prompt
    ) {
        private String promptVersion() {
            return prompt == null ? "none" : prompt.version();
        }
    }
}

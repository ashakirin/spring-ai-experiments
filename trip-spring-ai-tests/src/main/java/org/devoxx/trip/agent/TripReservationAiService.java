package org.devoxx.trip.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class TripReservationAiService {
    private final static Logger logger = LoggerFactory.getLogger(TripReservationAiService.class);

    private static final String DEFAULT_SYSTEM_PROMPT = """
            I assistant helps to organize trip: reserve fight, rent car, book the hotel, arrange the events and so on
            """;

    private final ChatClient chatClient;
    private final ChatClient.Builder chatClientBuilder;
    private final ChatResponseAudit chatResponseAudit;
    private final VectorStore vectorStore;

    public TripReservationAiService(ChatClient.Builder chatClientBuilder,
                                    ChatResponseAudit chatResponseAudit,
                                    VectorStore vectorStore) {
        this.chatClientBuilder = chatClientBuilder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT);
        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(ragAdvisor)
                .build();
        this.chatResponseAudit = chatResponseAudit;
        this.vectorStore = vectorStore;
    }

    public String chat(String prompt) {
        var chatResponse = chatClient
                .prompt()
                .user(prompt)
                .call()
                .chatResponse();
        String response = (chatResponse != null) ? chatResponse.getResult().getOutput().getText() : null;
        chatResponseAudit.logAuditRecord(prompt, response);
        return response;
    }

    public Flux<String> chatStream(String prompt) {
        return chatClient.prompt().user(prompt)
                .stream()
                .content()
                .onErrorResume(TimeoutException.class, ex -> {
                    logger.warn("Stream timed out: {}", ex.getMessage());
                    return Flux.just("⚠️ LLM response timed out. Please try again.");
                });
    }

    public void loadDataToVectorStore(String content) {
        vectorStore.add(List.of(new Document(content)));
    }
}

package org.devoxx.trip.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeoutException;

@Service
public class TripReservationAiService {
    private final static Logger logger = LoggerFactory.getLogger(TripReservationAiService.class);

    private static final String DEFAULT_SYSTEM_PROMPT = """
            I assistant helps to organize trip: reserve fight, rent car, book the hotel, arrange the events and so on.
            Use the available tools to help with hotel bookings and availability checks.
            """;

    private ChatClient chatClient;
    private final ChatClient.Builder chatClientBuilder;

    public TripReservationAiService(ChatClient.Builder chatClientBuilder,
                                    ToolCallbackProvider tools) {
        this.chatClientBuilder = chatClientBuilder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultToolCallbacks(tools);
        this.chatClient = chatClientBuilder.build();
    }

    public String chat(String prompt) {
        var chatResponse = chatClient
                .prompt()
                .user(prompt)
                .call()
                .chatResponse();
        String response = (chatResponse != null) ? chatResponse.getResult().getOutput().getText() : null;
        return response;
    }

    public Flux<String> chatStream(String prompt) {
        return chatClient.prompt().user(prompt)
                .stream()
                .content()
                .onErrorResume(TimeoutException.class, ex -> {
                    logger.warn("Stream timed out: {}", ex.getMessage());
                    chatClient = chatClientBuilder.build();
                    return Flux.just("⚠️ LLM response timed out. Please try again.");
                });
    }
}

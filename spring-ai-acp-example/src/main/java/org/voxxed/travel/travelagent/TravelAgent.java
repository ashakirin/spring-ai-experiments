package org.voxxed.travel.travelagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class TravelAgent {
    private static final String SYSTEM_PROMPT = """
    You are a helpful AI agent for travel and expense management.
    Be friendly, helpful, and concise in your responses.
    """;

    private static final String DEFAULT_CONVERSATION_ID = "testConversationId";

    private final ChatClient chatClient;

    public TravelAgent(ChatClient.Builder chatClientBuilder,
                       ChatMemory chatMemory) {
        // Spring AI auto-configures a default in-memory ChatMemory
        // (MessageWindowChatMemory + InMemoryChatMemoryRepository). Each conversation id
        // (e.g. an ACP session id) keeps its own history.
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String chat(String prompt) {
        return chat(prompt, DEFAULT_CONVERSATION_ID);
    }

    public Flux<String> chatStream(String prompt) {
        return chatStream(prompt, DEFAULT_CONVERSATION_ID);
    }

    /**
     * Blocking chat scoped to a specific conversation/memory thread.
     *
     * @param prompt         the user prompt
     * @param conversationId the memory thread id (e.g. an ACP session id)
     */
    public String chat(String prompt, String conversationId) {
        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /**
     * Streaming chat scoped to a specific conversation/memory thread.
     *
     * @param prompt         the user prompt
     * @param conversationId the memory thread id (e.g. an ACP session id)
     */
    public Flux<String> chatStream(String prompt, String conversationId) {
        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}

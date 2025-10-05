package org.devoxx.trip.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripReservationAiServiceTest {
    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;
    @Mock
    ChatResponseAudit mockAudit;
    @Mock
    VectorStore vectorStore;

    @InjectMocks
    private TripReservationAiService tripReservationAiService;

    @BeforeEach
    void setup() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
    }

    @Test
    void auditRecordPassed() {
        // Arrange
        when(chatClient.prompt().user("test prompt").call().chatResponse().getResult().getOutput().getText())
                .thenReturn("test response");
        
        TripReservationAiService service = new TripReservationAiService(chatClientBuilder, mockAudit, vectorStore);

        // Act
        String result = service.chat("test prompt");

        // Assert
        assertEquals("test response", result);
        verify(mockAudit).logAuditRecord("test prompt", "test response");
    }
}
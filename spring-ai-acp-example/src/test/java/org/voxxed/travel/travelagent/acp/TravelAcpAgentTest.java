package org.voxxed.travel.travelagent.acp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.agentclientprotocol.sdk.agent.Command;
import com.agentclientprotocol.sdk.agent.CommandResult;
import com.agentclientprotocol.sdk.agent.SyncPromptContext;
import com.agentclientprotocol.sdk.client.AcpClient;
import com.agentclientprotocol.sdk.client.AcpSyncClient;
import com.agentclientprotocol.sdk.agent.support.AcpAgentSupport;
import com.agentclientprotocol.sdk.spec.AcpSchema.AgentMessageChunk;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.Resource;
import com.agentclientprotocol.sdk.spec.AcpSchema.ResourceLink;
import com.agentclientprotocol.sdk.spec.AcpSchema.StopReason;
import com.agentclientprotocol.sdk.spec.AcpSchema.TextContent;
import com.agentclientprotocol.sdk.spec.AcpSchema.TextResourceContents;
import com.agentclientprotocol.sdk.test.InMemoryTransportPair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.voxxed.travel.travelagent.TravelAgent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the ACP adapter's three explicit-cue behaviours: attached-file context,
 * "curl &lt;City&gt;" weather fetch, and "save" file write. The two end-to-end tests
 * use the in-memory transport pair; the rest exercise the adapter directly with a
 * mocked {@link SyncPromptContext} so ACP protocol handlers aren't required.
 */
class TravelAcpAgentTest {

    private InMemoryTransportPair transports;
    private AcpAgentSupport support;
    private AcpSyncClient client;

    @BeforeEach
    void setUp() {
        transports = InMemoryTransportPair.create();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (support != null) {
            support.close();
        }
    }

    @Test
    void streamsModelResponseBackToClient() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenReturn(Flux.just("Bonjour! ", "I can help ", "plan your trip."));

        support = AcpAgentSupport.create(new TravelAcpAgent(travelAgent))
                .transport(transports.agentTransport())
                .build();
        support.start();

        StringBuilder received = new StringBuilder();
        client = AcpClient.sync(transports.clientTransport())
                .sessionUpdateConsumer(notification -> {
                    if (notification.update() instanceof AgentMessageChunk chunk
                            && chunk.content() instanceof TextContent text) {
                        received.append(text.text());
                    }
                })
                .build();

        client.initialize();
        NewSessionResponse session = client.newSession(new NewSessionRequest("/workspace", List.of()));
        assertThat(session.sessionId()).isNotBlank();

        PromptResponse response = client.prompt(new PromptRequest(
                session.sessionId(),
                List.of(new TextContent("Plan a 3-day trip to Paris"))));

        assertThat(response.stopReason()).isEqualTo(StopReason.END_TURN);
        assertThat(received.toString()).isEqualTo("Bonjour! I can help plan your trip.");
    }

    @Test
    void usesAcpSessionIdAsConversationId() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        AtomicReference<String> capturedConversationId = new AtomicReference<>();
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    capturedConversationId.set(invocation.getArgument(1));
                    return Flux.just("ok");
                });

        support = AcpAgentSupport.create(new TravelAcpAgent(travelAgent))
                .transport(transports.agentTransport())
                .build();
        support.start();

        client = AcpClient.sync(transports.clientTransport()).build();
        client.initialize();
        NewSessionResponse session = client.newSession(new NewSessionRequest("/workspace", List.of()));
        client.prompt(new PromptRequest(session.sessionId(), List.of(new TextContent("hi"))));

        assertThat(capturedConversationId.get()).isEqualTo(session.sessionId());
    }

    @Test
    void injectsContextFromAttachedResourceLink() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    capturedPrompt.set(invocation.getArgument(0));
                    return Flux.just("ok");
                });

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");
        when(ctx.tryReadFile(eq("/workspace/travel-preferences.md")))
                .thenReturn(Optional.of("- Diet: vegetarian\n- Budget: EUR 100/day"));

        ResourceLink attachment = new ResourceLink(
                "resource_link",
                "travel-preferences.md",
                "file:///workspace/travel-preferences.md",
                null, null, "text/markdown", null, null, null);

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1",
                        List.of(new TextContent("Please plan my next trip"), attachment)),
                ctx);

        assertThat(capturedPrompt.get())
                .contains("Diet: vegetarian")
                .contains("Budget: EUR 100/day")
                .contains("travel-preferences.md")
                .contains("Please plan my next trip");
        verify(ctx).sendThought(contains("Reading attached resources"));
        verify(ctx, never()).execute(any(Command.class));
        verify(ctx, never()).askPermission(anyString());
    }

    @Test
    void injectsContextFromInlineResource() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    capturedPrompt.set(invocation.getArgument(0));
                    return Flux.just("ok");
                });

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");

        Resource inline = new Resource(
                "resource",
                new TextResourceContents(
                        "Selection: prefer trains over flights when trip < 8h",
                        "file:///workspace/notes.md",
                        "text/markdown"),
                null, null);

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1",
                        List.of(new TextContent("Give me some general tips"), inline)),
                ctx);

        assertThat(capturedPrompt.get())
                .contains("prefer trains over flights")
                .contains("Give me some general tips");
        verify(ctx, never()).tryReadFile(anyString());
        verify(ctx).sendThought(contains("Reading attached resources"));
    }

    @Test
    void passesUserTextThroughWhenNoCuesPresent() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    capturedPrompt.set(invocation.getArgument(0));
                    return Flux.just("ok");
                });

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1", List.of(new TextContent("Give me general travel advice"))),
                ctx);

        assertThat(capturedPrompt.get()).isEqualTo("Give me general travel advice");
        verify(ctx, never()).tryReadFile(anyString());
        verify(ctx, never()).execute(any(Command.class));
        verify(ctx, never()).askPermission(anyString());
        verify(ctx, never()).sendThought(anyString());
    }

    @Test
    void fetchesWeatherWhenUserSaysCurlCity() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    capturedPrompt.set(invocation.getArgument(0));
                    return Flux.just("ok");
                });

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");
        when(ctx.execute(any(Command.class))).thenReturn(new CommandResult("Sunny +18°C", 0, false));

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1",
                        List.of(new TextContent("curl Rome and plan a weekend"))),
                ctx);

        assertThat(capturedPrompt.get())
                .contains("Rome: Sunny +18°C")
                .contains("curl Rome and plan a weekend");
        verify(ctx).sendThought(contains("Running curl wttr.in/Rome"));
        verify(ctx).execute(any(Command.class));
    }

    @Test
    void doesNotFetchWeatherWithoutCurlKeyword() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        when(travelAgent.chatStream(anyString(), anyString())).thenReturn(Flux.just("ok"));

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");

        // Prompt mentions a city but doesn't ask for weather via curl.
        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1", List.of(new TextContent("Plan a weekend in Rome"))),
                ctx);

        verify(ctx, never()).execute(any(Command.class));
    }

    @Test
    void skipsWeatherWhenShellCommandFails() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    capturedPrompt.set(invocation.getArgument(0));
                    return Flux.just("ok");
                });

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");
        when(ctx.execute(any(Command.class))).thenReturn(new CommandResult("", 1, false));

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1", List.of(new TextContent("curl Rome and give me tips"))),
                ctx);

        // curl was invoked but returned failure — the raw prompt goes through unchanged.
        assertThat(capturedPrompt.get()).isEqualTo("curl Rome and give me tips");
        verify(ctx).execute(any(Command.class));
    }

    @Test
    void savesResponseWhenUserSaysSaveAndApproves() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        when(travelAgent.chatStream(anyString(), anyString()))
                .thenReturn(Flux.just("# Rome plan\n\nDay 1: ", "Colosseum. ", "Day 2: Vatican."));

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");
        when(ctx.askPermission(anyString())).thenReturn(true);

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1",
                        List.of(new TextContent("Plan a weekend and save it"))),
                ctx);

        String expectedFilename = "itinerary-" + LocalDate.now() + ".md";
        verify(ctx).writeFile(eq(expectedFilename), contains("Day 1: Colosseum."));
        verify(ctx).sendMessage(contains("Saved to"));
    }

    @Test
    void doesNotWriteWhenUserDeclinesPermission() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        when(travelAgent.chatStream(anyString(), anyString())).thenReturn(Flux.just("some plan"));

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");
        when(ctx.askPermission(anyString())).thenReturn(false);

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1", List.of(new TextContent("Please save this response"))),
                ctx);

        verify(ctx).askPermission(anyString());
        verify(ctx, never()).writeFile(anyString(), anyString());
    }

    @Test
    void doesNotOfferSaveWithoutSaveKeyword() {
        TravelAgent travelAgent = mock(TravelAgent.class);
        when(travelAgent.chatStream(anyString(), anyString())).thenReturn(Flux.just("some content"));

        SyncPromptContext ctx = mock(SyncPromptContext.class);
        when(ctx.getSessionId()).thenReturn("session-1");

        new TravelAcpAgent(travelAgent).prompt(
                new PromptRequest("session-1",
                        List.of(new TextContent("What is the capital of France?"))),
                ctx);

        verify(ctx, never()).askPermission(anyString());
        verify(ctx, never()).writeFile(anyString(), anyString());
    }

}

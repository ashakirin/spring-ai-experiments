package org.voxxed.travel.travelagent.acp;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.agentclientprotocol.sdk.agent.Command;
import com.agentclientprotocol.sdk.agent.CommandResult;
import com.agentclientprotocol.sdk.agent.SyncPromptContext;
import com.agentclientprotocol.sdk.annotation.AcpAgent;
import com.agentclientprotocol.sdk.annotation.Initialize;
import com.agentclientprotocol.sdk.annotation.NewSession;
import com.agentclientprotocol.sdk.annotation.Prompt;
import com.agentclientprotocol.sdk.spec.AcpSchema.ContentBlock;
import com.agentclientprotocol.sdk.spec.AcpSchema.InitializeResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.Resource;
import com.agentclientprotocol.sdk.spec.AcpSchema.ResourceLink;
import com.agentclientprotocol.sdk.spec.AcpSchema.TextResourceContents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.voxxed.travel.travelagent.TravelAgent;

/**
 * Exposes the Spring AI {@link TravelAgent} over the Agent Client Protocol (ACP).
 *
 * <p>Three ACP capabilities are demonstrated, each triggered by an explicit user cue:
 * <ul>
 *   <li><b>File attachments</b> — files the user {@code @}-mentions in the client
 *       (Zed) arrive as content blocks. Their text is read via
 *       {@code fs/read_text_file} and prepended to the model prompt as context.</li>
 *   <li><b>Shell execution</b> — when the user types {@code curl <City>} in the
 *       prompt, the adapter runs {@code curl wttr.in/<City>} via ACP's terminal
 *       capability and injects the current weather into the prompt.</li>
 *   <li><b>File write</b> — when the user includes {@code save} in the prompt, after
 *       the model finishes streaming the adapter asks permission and writes the
 *       response to a markdown file via {@code fs/write_text_file}. Zed then shows a
 *       diff for final approval.</li>
 * </ul>
 */
@AcpAgent(name = "travel-agent", version = "1.0.0")
public class TravelAcpAgent {

    private static final Logger log = LoggerFactory.getLogger(TravelAcpAgent.class);

    /** User asks for weather by typing {@code curl <City>} (e.g. "curl Rome"). */
    private static final Pattern CURL_REQUEST_PATTERN = Pattern.compile(
            "(?i)\\bcurl\\s+([A-Z][A-Za-z]+)");

    /** User asks to save the response by including {@code save} in the prompt. */
    private static final String SAVE_KEYWORD = "save";

    private final TravelAgent travelAgent;

    /** ACP session id → workspace cwd, needed to build absolute paths for writeFile. */
    private final Map<String, String> sessionCwds = new ConcurrentHashMap<>();

    public TravelAcpAgent(TravelAgent travelAgent) {
        this.travelAgent = travelAgent;
    }

    @Initialize
    public InitializeResponse init() {
        return InitializeResponse.ok();
    }

    @NewSession
    public NewSessionResponse newSession(NewSessionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        if (request.cwd() != null && !request.cwd().isBlank()) {
            sessionCwds.put(sessionId, request.cwd());
            log.info("Opened session {} in workspace {}", sessionId, request.cwd());
        }
        return new NewSessionResponse(sessionId, null, null);
    }

    @Prompt
    public PromptResponse prompt(PromptRequest request, SyncPromptContext ctx) {
        String userText = request.text();
        String enrichedPrompt = enrichPrompt(userText, request, ctx);

        // Stream the response, and also accumulate it in case the user asked to save.
        StringBuilder fullResponse = new StringBuilder();
        travelAgent.chatStream(enrichedPrompt, ctx.getSessionId())
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnNext(chunk -> {
                    ctx.sendMessage(chunk);
                    fullResponse.append(chunk);
                })
                .blockLast();

        if (userWantsToSave(userText)) {
            saveItinerary(fullResponse.toString(), ctx);
        }
        return PromptResponse.endTurn();
    }

    // ---------- Prompt enrichment: attachments + weather ---------------------

    private String enrichPrompt(String userText, PromptRequest request, SyncPromptContext ctx) {
        StringBuilder context = new StringBuilder();
        appendAttachedResources(context, request, ctx);
        appendWeatherIfRequested(context, userText, ctx);
        if (context.length() == 0) {
            return userText;
        }
        return context.append("Request: ").append(userText).toString();
    }

    /** Reads any files the user attached via @-mention and adds their content as context. */
    private void appendAttachedResources(StringBuilder context, PromptRequest request, SyncPromptContext ctx) {
        List<AttachedResource> attached = collectAttachedResources(request, ctx);
        if (attached.isEmpty()) {
            return;
        }
        String names = String.join(", ", attached.stream().map(AttachedResource::name).toList());
        log.info("Enriched prompt with {} attached resource(s): {}", attached.size(), names);
        ctx.sendThought("Reading attached resources: " + names + ".");
        context.append("The user attached these files; use them as context.\n\n");
        for (AttachedResource r : attached) {
            context.append("File: ").append(r.name()).append("\n---\n")
                    .append(r.text().strip()).append("\n---\n\n");
        }
    }

    /** If the user typed "curl &lt;City&gt;", fetch current weather and add it as context. */
    private void appendWeatherIfRequested(StringBuilder context, String userText, SyncPromptContext ctx) {
        if (userText == null) {
            return;
        }
        Matcher m = CURL_REQUEST_PATTERN.matcher(userText);
        if (!m.find()) {
            return;
        }
        String city = m.group(1);
        ctx.sendThought("Running curl wttr.in/" + city + " to fetch the weather.");
        try {
            Command cmd = Command.of("curl", "-s", "--max-time", "5",
                            "wttr.in/" + city + "?format=%C+%t")
                    .outputByteLimit(1024);
            CommandResult result = ctx.execute(cmd);
            if (result.success() && !result.output().isBlank()) {
                String weather = city + ": " + result.output().trim();
                log.info("Fetched weather: {}", weather);
                context.append("Current weather (fetched live via curl wttr.in):\n---\n")
                        .append(weather).append("\n---\n\n");
            }
        }
        catch (Exception ex) {
            log.debug("Weather fetch for {} failed: {}", city, ex.getMessage());
        }
    }

    private List<AttachedResource> collectAttachedResources(PromptRequest request, SyncPromptContext ctx) {
        List<AttachedResource> out = new ArrayList<>();
        for (ContentBlock block : request.prompt()) {
            if (block instanceof ResourceLink link) {
                readByUri(link.uri(), ctx)
                        .filter(text -> !text.isBlank())
                        .ifPresent(text -> out.add(new AttachedResource(link.name(), text)));
            }
            else if (block instanceof Resource resource
                    && resource.resource() instanceof TextResourceContents contents
                    && contents.text() != null && !contents.text().isBlank()) {
                out.add(new AttachedResource(contents.uri(), contents.text()));
            }
        }
        return out;
    }

    private Optional<String> readByUri(String uri, SyncPromptContext ctx) {
        if (uri == null || uri.isBlank()) {
            return Optional.empty();
        }
        String path = uri.startsWith("file://") ? uri.substring("file://".length()) : uri;
        try {
            return ctx.tryReadFile(path);
        }
        catch (Exception ex) {
            log.debug("Could not read {}: {}", uri, ex.getMessage());
            return Optional.empty();
        }
    }

    // ---------- Save flow ---------------------------------------------------

    private static boolean userWantsToSave(String userText) {
        return userText != null && userText.toLowerCase().contains(SAVE_KEYWORD);
    }

    private void saveItinerary(String plan, SyncPromptContext ctx) {
        if (plan == null || plan.isBlank()) {
            return;
        }
        boolean approved;
        try {
            approved = ctx.askPermission("Save this response to a markdown file in your workspace?");
        }
        catch (Exception ex) {
            log.debug("askPermission not supported by client: {}", ex.getMessage());
            return;
        }
        if (!approved) {
            log.info("User declined to save.");
            return;
        }
        String filename = "itinerary-" + LocalDate.now() + ".md";
        String path = resolveWorkspacePath(ctx.getSessionId(), filename);
        try {
            ctx.writeFile(path, plan);
            log.info("Wrote {} ({} chars)", path, plan.length());
            ctx.sendMessage("\n\n💾 Saved to `" + filename + "`.");
        }
        catch (Exception ex) {
            log.warn("Failed to write {}: {}", path, ex.getMessage());
            ctx.sendMessage("\n\n⚠️  Could not save: " + ex.getMessage());
        }
    }

    /** Zed rejects relative paths for {@code fs/write_text_file} — build the absolute one. */
    private String resolveWorkspacePath(String sessionId, String filename) {
        String cwd = sessionCwds.get(sessionId);
        return (cwd == null || cwd.isBlank()) ? filename : Path.of(cwd, filename).toString();
    }

    private record AttachedResource(String name, String text) {
    }

}

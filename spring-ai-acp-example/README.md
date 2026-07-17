# Spring AI + ACP Example (Travel Agent)

A Spring AI agent (Amazon Bedrock Converse + AgentCore memory) exposed over the
[Agent Client Protocol (ACP)](https://agentclientprotocol.com/) so it can be driven by
ACP-aware editors such as **IntelliJ Idea** or **Zed**.

The conversational logic is unchanged from the original Spring AI app. ACP is added as a
thin adapter layer:

```
ACP editor  ⇄  stdio (JSON-RPC)  ⇄  AcpStdioRunner
                                       └─ TravelAcpAgent (@AcpAgent)   ← protocol adapter
                                            └─ TravelAgent (ChatClient) ← unchanged Spring AI logic
                                                 └─ Bedrock Converse + AgentCore memory
```

The same Spring Boot application can run in **two modes**:

| Mode | How | Transport | Web server |
|------|-----|-----------|------------|
| Web app (REST) | `java -jar app.jar` | HTTP (`/chat`, `/chat-stream`) | yes (Netty/WebFlux) |
| ACP agent | `java -jar app.jar --acp` | stdio JSON-RPC | no |

## What was added

- `acp/TravelAcpAgent.java` — annotation-based ACP agent (`@AcpAgent`, `@Initialize`,
  `@NewSession`, `@Prompt`). Streams the model response back to the client as
  `agent_message_chunk` updates. The ACP `sessionId` is used as the Spring AI memory
  conversation id, so each session keeps its own history.
- `acp/AcpStdioRunner.java` — `@Profile("acp")` runner that boots the agent over stdio.
- `TravelAgentApplication` — switches to a non-web, `acp`-profile app when launched with
  `--acp`.
- `TravelAgent` — uses Spring AI's in-memory `ChatMemory` (`MessageWindowChatMemory` +
  `InMemoryChatMemoryRepository`) instead of AgentCore memory, and added
  conversation-id-scoped `chat`/`chatStream` overloads (the original no-arg-conversation
  methods still work).
- `pom.xml` — added `com.agentclientprotocol:acp-agent-support` (and `acp-test` for tests);
  removed the `spring-ai-agentcore-memory` dependency.

### Why stdout is protected

ACP stdio agents speak newline-delimited JSON-RPC on **stdout**; any stray output corrupts
the stream. `AcpStdioRunner` captures the real stdout for the protocol and redirects
`System.out` to stderr (banner is disabled, logs go to stderr and to a file). This is why
the launcher uses a dedicated `--acp` mode rather than running the web server.

## Prerequisites

- Java 17+
- The ACP Java SDK installed in your local Maven repo. This project depends on
  `0.15.0-SNAPSHOT`. From the SDK checkout:
  ```bash
  cd /path/to/java-sdk
  ./mvnw -DskipTests install
  ```
- AWS credentials in `~/.aws` with permission to invoke Amazon Bedrock. The app reads them
  via the AWS profile configured in `application.properties`:
  ```properties
  spring.ai.bedrock.aws.region=us-east-1
  spring.ai.bedrock.aws.profile.name=default
  ```
  Change `profile.name` to use a different `~/.aws` profile, or override at runtime with
  `-Dspring.ai.bedrock.aws.profile.name=<profile>`. Verify a profile works with:
  ```bash
  aws sts get-caller-identity --profile default
  ```
  (No AgentCore memory id is required — conversation memory is now in-process.)

## Build

```bash
./mvnw -DskipTests package
# -> target/spring-ai-acp-example-0.0.1-SNAPSHOT.jar
```

## Test

The ACP wiring is verified end-to-end over an in-memory transport with a mocked
`TravelAgent`, so it needs **no AWS access**:

```bash
./mvnw -Dtest=TravelAcpAgentTest test
```

`TravelAcpAgentTest` performs a real `initialize → session/new → session/prompt` roundtrip
through the ACP runtime and asserts the streamed chunks and the END_TURN stop reason.

> Note: the default `contextLoads` Spring Boot test starts the full application context and
> therefore requires valid AWS/Bedrock configuration. Run the ACP test in isolation (as
> above) in environments without AWS.

## Run

### As a normal web app

```bash
java -jar target/spring-ai-acp-example-0.0.1-SNAPSHOT.jar
# POST http://localhost:8080/chat
# POST http://localhost:8080/chat-stream
```

### As an ACP stdio agent

```bash
java -jar target/spring-ai-acp-example-0.0.1-SNAPSHOT.jar --acp
```

It now reads JSON-RPC from stdin and writes responses to stdout. Logs go to stderr and to
`travel-acp-agent.log` (override with `ACP_LOG_FILE`).

## Integrating with IntelliJ Idea

IntelliJ Idea natively supports ACP agents as JetBrains is co-leading the developing of ACP protocol. You can connect your own ACP agent to IntelliJ IDE by configuring an access to it via `~/.jetbrains/acp.json` config file, where the agent run command, arguments, and environment variables should be defined in JSON format:

```
{
  "agent_servers": {
    "Travel Agent": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/spring-ai-acp-example/target/spring-ai-acp-example-0.0.1-SNAPSHOT.jar",
        "--acp"
      ],
      "env": {
        "AWS_PROFILE": "default"
      }
    },
  }
}
```

Then open the AI Chat in IntelliJ Idea, pick **Travel Agent** from the agent picker dropdown, and chat.
IntelliJ Idea handles `initialize`, opens a `session/new`, and sends your messages as `session/prompt`;
the streamed `agent_message_chunk` updates appear in the panel in real time.

Tips:
- Use an **absolute** path to the jar;
- Credentials come from `~/.aws`. The jar already pins a profile via
  `application.properties`; the `AWS_PROFILE` env entry above is optional and just lets you
  override it per editor. The subprocess inherits your `HOME`, so `~/.aws` is found
  automatically.
- If startup fails, check `travel-acp-agent.log` and IntelliJ Idea's agent logs (Command search -> Get ACP Logs).

## Integrating with Zed

Zed launches ACP agents as a subprocess and talks to them over stdio. Add an entry to your
Zed `settings.json` (`zed: open settings`):

```json
{
  "agent_servers": {
    "Travel Agent": {
      "type": "custom",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/spring-ai-acp-example/target/spring-ai-acp-example-0.0.1-SNAPSHOT.jar",
        "--acp"
      ],
      "env": {
        "AWS_PROFILE": "default"
      }
    }
  }
}
```

Then open the Agent Panel in Zed, pick **Travel Agent** from the agent menu, and chat.
Zed handles `initialize`, opens a `session/new`, and sends your messages as `session/prompt`;
the streamed `agent_message_chunk` updates appear in the panel in real time.

Tips:
- Use an **absolute** path to the jar; Zed does not resolve relative paths or shell aliases.
- Credentials come from `~/.aws`. The jar already pins a profile via
  `application.properties`; the `AWS_PROFILE` env entry above is optional and just lets you
  override it per editor. The subprocess inherits your `HOME`, so `~/.aws` is found
  automatically.
- If startup fails, check `travel-acp-agent.log` and Zed's agent logs (`dev: open acp logs`).

## Sample prompts (demo the ACP capabilities)

The adapter maps three ACP capabilities to explicit user cues in the prompt. Each cue is
a keyword the audience can see, so what triggers what stays transparent during a demo.

| Cue | ACP capability | What it does |
|---|---|---|
| `@filename` | `fs/read_text_file` | Attached file is read; content injected as prompt context |
| `curl <City>` | `terminal/exec` | Runs `curl wttr.in/<City>`; live weather injected as context |
| `save` | `session/request_permission` + `fs/write_text_file` | Asks Yes/No, then writes `itinerary-<today>.md` in the workspace |

### 1. Attachment only

```
Plan a weekend in Rome. @travel-preferences.md
```

Zed sends `travel-preferences.md` as a `ResourceLink`. The agent reads it via
`fs/read_text_file` and personalises the response — vegetarian food, mid-range hotels,
transport preferences. No shell exec, no save.

### 2. Shell only

```
curl Rome and plan a weekend
```

The `curl <City>` cue triggers `curl -s wttr.in/Rome?format=%C+%t` through ACP's
terminal capability. The plan reflects the actual current temperature and conditions.
Cross-check by running the same command in your terminal — the temperatures should
match.

### 3. Save only

```
Plan a weekend in Rome and save it
```

After the plan streams, Zed shows a Yes/No permission prompt: *"Save this response to
a markdown file in your workspace?"* On Yes, Zed shows a diff for
`itinerary-<today>.md`. On approval, the file lands in the workspace tree.

### 4. All three at once (the money shot)

```
curl Kyoto, plan a 3-day trip, save it. @travel-preferences.md
```

Reads preferences → fetches Kyoto weather → streams a personalised, weather-aware plan
→ offers to save. Four ACP protocol interactions in one turn (fs read, terminal exec,
permission request, fs write).

### Control (no cues)

```
Give me some general travel advice
```

No `@`, no `curl`, no `save` → no side effects. The agent just chats.

### Watching the demo happen

Tail the log alongside Zed:

```bash
tail -f travel-acp-agent.log
```

Depending on which cues fired, you'll see:

```
INFO ... Opened session <uuid> in workspace /Users/.../spring-ai-acp-example
INFO ... Enriched prompt with 1 attached resource(s): travel-preferences.md
INFO ... Fetched weather: Rome: Sunny +22°C
INFO ... Wrote /Users/.../itinerary-2026-07-02.md (2143 chars)
```

Each line pairs with a visible interaction in Zed's UI: attachment icon, thought bubble,
Yes/No prompt, diff prompt.

## Integrating with other lightweight ACP clients

Any ACP-compatible client uses the same launch contract — run the jar with `--acp` and
speak JSON-RPC over stdio. The shapes differ only slightly per editor:

- **opencode / Mistral Vibe / Kimi / Blackbox CLI in Zed**: same `agent_servers` block with
  `"command": "java"` and `"args": ["-jar", "<jar>", "--acp"]`.
- **Custom ACP client (e.g. the ACP Java SDK client)**: spawn the process and connect a
  `StdioAcpClientTransport` to its stdin/stdout:
  ```java
  var params = AgentParameters.builder("java")
      .arg("-jar").arg("/abs/path/spring-ai-acp-example-0.0.1-SNAPSHOT.jar").arg("--acp")
      .build();
  var transport = new StdioAcpClientTransport(params);
  AcpSyncClient client = AcpClient.sync(transport)
      .sessionUpdateConsumer(n -> { /* render agent_message_chunk */ })
      .build();
  client.initialize();
  var session = client.newSession(new NewSessionRequest("/workspace", List.of()));
  client.prompt(new PromptRequest(session.sessionId(),
      List.of(new TextContent("Plan a 3-day trip to Paris"))));
  ```

## Capabilities

The example exercises four ACP methods, each mapped to an explicit user cue in the
prompt (see [Sample prompts](#sample-prompts-demo-the-acp-capabilities) above):

| ACP method | User cue | SDK entry point |
|---|---|---|
| `fs/read_text_file` | `@filename` attachment | `SyncPromptContext#tryReadFile` |
| `terminal/exec` | `curl <City>` | `SyncPromptContext#execute(Command)` |
| `session/request_permission` | `save` | `SyncPromptContext#askPermission` |
| `fs/write_text_file` | `save` (after permission granted) | `SyncPromptContext#writeFile` |

The core lifecycle handlers are `@Initialize`, `@NewSession`, and `@Prompt`. To go
further — session persistence, cancellation, tool discovery — add handlers such as
`@LoadSession`, `@Cancel`, or `@ListProviders`. See the SDK for the full set of
annotations and `SyncPromptContext` methods.

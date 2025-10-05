# Trip Spring AI MCP Client Tests

A Spring Boot application that demonstrates integration between Spring AI and Model Context Protocol (MCP) for trip reservation services.

## Overview

This project showcases how to build an AI-powered trip assistant using Spring AI with MCP client capabilities. The application can help organize trips by handling flight reservations, car rentals, hotel bookings, and event arrangements through AI-powered conversations.

## Features

- **AI Chat Interface**: REST endpoints for both synchronous and streaming chat interactions
- **MCP Integration**: Connects to MCP servers for extended tool capabilities
- **Trip Planning**: AI assistant specialized in travel and reservation services
- **AWS Bedrock Integration**: Uses Claude 3.7 Sonnet model for AI responses
- **Reactive Streaming**: Supports real-time streaming responses

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.3**
- **Spring AI 1.1.0-SNAPSHOT**
- **AWS Bedrock Converse API**
- **Model Context Protocol (MCP)**
- **Maven**

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- AWS credentials configured for Bedrock access
- MCP server running on `http://localhost:8081/mcp`

## Configuration

The application is configured via `application.properties`:

```properties
spring.ai.bedrock.aws.region=us-east-1
spring.ai.bedrock.converse.chat.options.model=us.anthropic.claude-3-7-sonnet-20250219-v1:0
spring.ai.mcp.client.toolcallback.enabled=true
spring.ai.mcp.client.streamable-http.connections.server1.url=http://localhost:8081/mcp
```

## API Endpoints

### Chat Endpoint
```
POST /api/chat
Content-Type: application/json

{
  "prompt": "Help me book a hotel in Paris for next week"
}
```

### Streaming Chat Endpoint
```
POST /api/chat/stream
Content-Type: application/json

{
  "prompt": "Find flights from New York to London"
}
```

## Running the Application

1. Clone the repository
2. Ensure AWS credentials are configured
3. Start your MCP server on port 8081
4. Run the application:

```bash
./mvnw spring-boot:run
```

The application will start on the default port 8080.

## Project Structure

```
src/main/java/org/devoxx/trip/agent/
├── TripSpringAiApplication.java    # Main application class
├── ChatController.java             # REST endpoints for chat
├── TripReservationAiService.java   # Core AI service
├── McpClientConfig.java            # MCP client configuration
└── ChatResponseAudit.java          # Response auditing
```

## Development

This is a snapshot version using Spring AI experimental features. The project demonstrates:

- Integration patterns between Spring AI and MCP
- Reactive streaming with AI responses
- Tool callback mechanisms for extended functionality
- Error handling and timeout management

## License

This project is part of Spring AI experiments and follows the respective licensing terms.
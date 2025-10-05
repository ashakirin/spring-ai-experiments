# Hotel MCP Server - Spring AI Experiments

A Spring Boot application implementing a Model Context Protocol (MCP) server for hotel booking operations. This project demonstrates the integration of Spring AI with MCP to provide hotel availability checking and booking functionality.

## Project Description

This application serves as an MCP server that exposes hotel-related tools through the Model Context Protocol. It provides two main functionalities:

- **Hotel Availability**: Check available hotels in a city for specific dates
- **Hotel Booking**: Book a hotel with guest information and dates

The server uses Spring AI's MCP integration to expose these services as tools that can be consumed by MCP clients.

## Technologies Used

- **Java 21**
- **Spring Boot 3.5.3**
- **Spring AI 1.1.0-SNAPSHOT**
- **Model Context Protocol SDK 0.14.0**
- **Maven** for dependency management

## Project Structure

```
src/
├── main/java/org/devoxx/mcp/trip/hotel/
│   ├── HotelMcpApplication.java    # Main Spring Boot application
│   ├── HotelMcpServer.java         # MCP server with @McpTool annotations
│   └── HotelService.java           # Business logic for hotel operations
├── test/java/org/devoxx/mcp/trip/hotel/
│   ├── HotelMcpServerTest.java     # Integration tests for MCP functionality
│   ├── HotelAvailabilityRequest.java
│   ├── HotelAvailabilityResult.java
│   ├── HotelBookingRequest.java
│   └── HotelBookingResult.java
└── resources/
    └── application.properties      # Server configuration
```

## Available MCP Tools

### 1. getAvailability
- **Description**: Get available hotels in a city for given dates
- **Parameters**:
  - `city` (String): Target city
  - `checkInDate` (String): Check-in date
  - `checkOutDate` (String): Check-out date

### 2. bookHotel
- **Description**: Book a hotel by name, city, and guest info
- **Parameters**:
  - `city` (String): Target city
  - `hotelName` (String): Name of the hotel
  - `guestName` (String): Guest name
  - `checkInDate` (String): Check-in date
  - `checkOutDate` (String): Check-out date

## Setup and Running

### Prerequisites
- Java 21 or higher
- Maven 3.6+

### Build and Run
```bash
# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The server will start on port 8081 (configured in application.properties).

## Testing

### Run Unit Tests
```bash
./mvnw test
```

### Test with curl

Once the application is running, you can test the MCP server endpoints using curl:

#### 1. List Available Tools
```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }'
```

#### 2. Test Hotel Availability
```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "getAvailability",
      "arguments": {
        "city": "Paris",
        "checkInDate": "2024-12-01",
        "checkOutDate": "2024-12-03"
      }
    }
  }'
```

#### 3. Test Hotel Booking
```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "bookHotel",
      "arguments": {
        "city": "Paris",
        "hotelName": "Hotel Ritz",
        "guestName": "John Doe",
        "checkInDate": "2024-12-01",
        "checkOutDate": "2024-12-03"
      }
    }
  }'
```

## Test Description

The project includes comprehensive integration tests in `HotelMcpServerTest.java`:

- **testMcpClientListTools**: Verifies that the MCP server correctly exposes the available tools
- **testGetAvailabilityToolCall**: Tests the hotel availability functionality through MCP client
- **testBookHotelTool**: Tests the hotel booking functionality through MCP client

Tests use:
- Spring Boot Test with random port
- Mockito for service layer mocking
- MCP Sync Client for integration testing
- AssertJ for assertions

## Configuration

The application is configured via `application.properties`:
- Server runs on port 8081
- MCP protocol is set to STREAMABLE

## Development Notes

This is a demonstration project showing how to integrate Spring AI with Model Context Protocol. The hotel service provides mock data for testing purposes. In a production environment, you would integrate with actual hotel booking APIs and databases.
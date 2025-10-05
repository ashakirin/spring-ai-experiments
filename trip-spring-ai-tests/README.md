# Trip Spring AI Tests

A Spring Boot application that uses AWS Bedrock for AI capabilities with comprehensive testing suite.

## Prerequisites

- Java 21
- Maven 3.6+
- Docker & Docker Compose
- AWS credentials configured with access to Bedrock

## Quick Start

### 1. Start PostgreSQL Database

```bash
docker-compose up -d
```

This starts a PostgreSQL database with pgvector extension for vector storage.

### 2. Run the Application

```bash
# Set your AWS credentials
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=us-east-1

# Run the application
mvn spring-boot:run
```

### 3. Test the API

```bash
# Chat API
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about travel requirements"}'

# Streaming Chat API
curl -X POST "http://localhost:8080/api/chat/stream" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Tell me about travel requirements"}'
```

## Testing

### Running All Tests

```bash
# Run all tests
mvn test

# Run tests with verbose output
mvn test -Dtest.verbose=true

# Run specific test class
mvn test -Dtest=TripReservationAiServiceTest
```

### Test Categories

#### 1. Unit Tests
- **TripReservationAiServiceTest**: Mock-based unit tests for the AI service
- **UnicornSpringAiAgentApplicationTests**: Basic Spring Boot application context tests

#### 2. Integration Tests with Testcontainers
- **ChatClientRagTest**: Tests RAG (Retrieval Augmented Generation) with PostgreSQL/pgvector
- **ChatClientEvaluatorTest**: Tests AI response evaluation using Ollama models
- **DeepEvalTest**: Tests AI evaluation using DeepEval framework

#### 3. AI Evaluation Tests

**ChatClientEvaluatorTest** includes:
- Direct model response testing
- Relevancy evaluation
- Fact-checking evaluation (positive and negative cases)

**DeepEvalTest** includes:
- Answer relevancy evaluation
- Faithfulness evaluation

### Test Requirements

- **Docker**: Required for Testcontainers (PostgreSQL, Ollama, DeepEval service)
- **Internet**: Required to pull Docker images and AI models
- **AWS Credentials**: Required for Bedrock integration tests

### Running Individual Test Suites

```bash
# Unit tests only
mvn test -Dtest="*Test" -DexcludeGroups="integration"

# Integration tests with Testcontainers
mvn test -Dtest="*RagTest,*EvaluatorTest,*DeepEvalTest"

# RAG tests only
mvn test -Dtest=ChatClientRagTest

# Evaluation tests only
mvn test -Dtest=ChatClientEvaluatorTest

# DeepEval tests only
mvn test -Dtest=DeepEvalTest
```

### Test Configuration

Tests use Testcontainers for:
- **PostgreSQL with pgvector**: For vector storage testing
- **Ollama**: For local AI model evaluation
- **DeepEval Service**: For advanced AI evaluation metrics

### Troubleshooting Tests

#### Docker Issues
```bash
# Check Docker is running
docker info

# Clean up test containers
docker container prune -f
docker image prune -f
```

#### Testcontainers Issues
```bash
# Enable Testcontainers logging
export TESTCONTAINERS_RYUK_DISABLED=true
mvn test -Dlogback.configurationFile=logback-test.xml
```

#### Model Download Issues
- Ollama models are downloaded automatically but may take time on first run
- Models are cached in `/tmp/ollama-models` for reuse

## Development

### Building the Application

```bash
# Compile
mvn compile

# Package
mvn package

# Build Docker image
mvn spring-boot:build-image
```

## Troubleshooting

### Application Issues

```bash
# Check application logs
mvn spring-boot:run

# Check if PostgreSQL is running
docker-compose ps

# Connect to PostgreSQL
docker exec -it postgres-db psql -U chatuser -d ai-agent-db
```

### Test Issues

```bash
# Clean test containers
docker container prune -f

# Check Docker daemon
docker info

# Run single test with debug
mvn test -Dtest=TripReservationAiServiceTest -X
```

### AWS Bedrock Issues

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Check Bedrock model access
aws bedrock list-foundation-models --region us-east-1
```
# CodeArena Runner

A backend microservice that **compiles, runs, and judges user-submitted source code** inside hardened Docker containers. Built by [CodexSphere](https://codexsphere.com).

![Java](https://img.shields.io/badge/Java-21-E76F00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=flat&logo=springboot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-28.x-2496ED?style=flat&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)
![Version](https://img.shields.io/badge/Version-1.0.0-brightgreen)

---

## Overview

CodeArena Runner is the execution engine for a competitive programming platform. It accepts source code via a REST API or Kafka consumer, compiles and runs it in a sandboxed environment, and returns the result with a verdict.

**Supported languages:** Java, Python, C++, JavaScript

**Two execution modes:**
- **RUN** -- Execute code once, return output
- **JUDGE** -- Execute against multiple test cases, return verdict (ACCEPTED, WRONG_ANSWER, etc.)

---

## Features

- **Multi-language support** -- Java 21, Python 3.12, C++ (GCC 14), JavaScript (Node 22)
- **Docker sandboxing** -- Hardened containers with no network, read-only FS, dropped capabilities, memory/CPU/PID limits
- **Dual execution modes** -- RUN (single execution) and JUDGE (multi-test-case grading)
- **Dual backend** -- Docker (sandboxed) or LOCAL (ProcessBuilder)
- **Rate limiting** -- Sliding window, 30 req/60s per IP for public clients
- **Concurrency control** -- Semaphore-based, max 10 concurrent executions
- **Bearer token auth** -- Stateless security, trusted services bypass rate limiting
- **Persistence** -- MySQL with Flyway migrations, execution logs and metrics
- **Observability** -- Spring Boot Actuator + Prometheus metrics
- **Kafka integration** -- Optional consumer for async submission processing
- **Production-ready** -- Health checks, graceful shutdown, startup validation

---

## Architecture

```
                    +-----------------------------+
                    |      Entry Points           |
                    |  REST API / Kafka Consumer   |
                    +-----------------------------+
                                   |
                                   v
                    +-----------------------------+
                    |     ExecutionManager        |  <-- Orchestrator
                    +-----------------------------+
                                   |
        +--------------------------------------------------+
        | Validation | Concurrency | Context | Workspace   |
        | Source     | Docker      | Compiler | Runner     |
        | Judge      | Rate Limit  | Response | Mapper     |
        +--------------------------------------------------+
                                   |
                    +-----------------------------+
                    |      ProcessExecutor        |
                    |  LocalProcessExecutor       |
                    |  DockerProcessExecutor      |
                    +-----------------------------+
                                   |
                    +-----------------------------+
                    |    ExecutionResponseMapper   |
                    +-----------------------------+
                                   |
                                Response
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Web | Spring WebMVC (REST API) |
| Security | Spring Security (stateless, Bearer token) |
| Messaging | Spring Kafka (consumer, optional) |
| Persistence | Spring Data JPA + Flyway + MySQL 8.4 |
| Sandboxing | docker-java 3.5.0 |
| Build | Maven |
| Container Runtime | Docker (docker-outside-of-docker) |
| Monitoring | Spring Boot Actuator + Micrometer + Prometheus |
| Server Port | 8081 |

---

## Quick Start

### Prerequisites

- Java 21+
- Docker Engine
- Maven 3.9+ (or use the included `mvnw` wrapper)

### 1. Clone the repository

```bash
git clone https://github.com/Brajeshkumar1107/Code-Arena.git
cd Code-Arena
```

### 2. Run with Docker Compose (recommended)

```bash
# Create workspace directory
sudo mkdir -p /var/lib/codearena/workspaces
sudo chown 1000:1000 /var/lib/codearena/workspaces

# Generate secrets
cp .env.example .env
sed -i "s/^RUNNER_TOKEN=.*/RUNNER_TOKEN=$(openssl rand -hex 32)/" .env
sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$(openssl rand -hex 16)/" .env

# Start services (MySQL + Kafka + Runner)
docker compose up -d --build

# Verify
curl http://localhost:8081/actuator/health
```

### 3. Test the API

```bash
curl -X POST http://localhost:8081/api/v1/runner/execute \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "RUN",
    "language": "PYTHON",
    "sourceCode": "print(42)",
    "testCases": [{"input": ""}]
  }'
```

**Response:**
```json
{
  "success": true,
  "verdict": "ACCEPTED",
  "stdout": "42\n",
  "stderr": "",
  "passed": 0,
  "total": 0,
  "executionTime": 45,
  "memoryUsed": 18,
  "testCases": []
}
```

---

## API Documentation

### Execute Code

```
POST /api/v1/runner/execute
Content-Type: application/json
Authorization: Bearer <token>    (optional for public, rate-limited)
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `mode` | `RUN` \| `JUDGE` | Yes | Execution mode |
| `language` | `JAVA` \| `PYTHON` \| `CPP` \| `JAVASCRIPT` | Yes | Programming language |
| `sourceCode` | `string` | Yes | Source code to execute (max 256 KB) |
| `testCases` | `array` | Yes | At least one test case (max 20) |
| `testCases[].input` | `string` | No | Stdin input (defaults to empty) |
| `testCases[].expectedOutput` | `string` | Required in JUDGE mode | Expected stdout for comparison |
| `submissionId` | `long` | No | Correlation ID |
| `problemId` | `long` | No | Problem identifier |

**Response:**

| Field | Type | Description |
|-------|------|-------------|
| `success` | `boolean` | Whether execution succeeded |
| `verdict` | `string` | See verdict table below |
| `stdout` | `string` | Program output (RUN mode) |
| `stderr` | `string` | Error output |
| `passed` | `int` | Test cases passed (JUDGE mode) |
| `total` | `int` | Total test cases (JUDGE mode) |
| `executionTime` | `long` | Execution time in milliseconds |
| `memoryUsed` | `long` | Peak memory in MB |
| `testCases` | `array` | Per-case results (JUDGE mode) |

### Verdicts

| Verdict | Description |
|---------|-------------|
| `ACCEPTED` | Code compiled and ran correctly |
| `WRONG_ANSWER` | Output didn't match expected (JUDGE mode) |
| `COMPILATION_ERROR` | Code failed to compile |
| `RUNTIME_ERROR` | Non-zero exit code |
| `TIME_LIMIT_EXCEEDED` | Execution timed out |
| `MEMORY_LIMIT_EXCEEDED` | Exceeded memory limit |
| `OUTPUT_LIMIT_EXCEEDED` | Output exceeded size limit |

### Health Check

```
GET /actuator/health
```

**Response:**
```json
{"status": "UP", "components": {"db": {"status": "UP"}, ...}}
```

### Error Response

```json
{
  "timestamp": "2026-08-18T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Execution mode is required.",
  "path": "/api/v1/runner/execute"
}
```

| Status | Description |
|--------|-------------|
| 400 | Validation error / malformed request |
| 401 | Missing or invalid bearer token |
| 429 | Rate limit exceeded (public clients) |
| 503 | All concurrency slots busy |
| 500 | Internal server error |

---

## Execution Modes

### RUN Mode

Executes the program **once** with the first test case's input and returns the output. No comparison against expected output.

```json
{
  "mode": "RUN",
  "language": "JAVA",
  "sourceCode": "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        System.out.println(sc.nextInt() * 2);\n    }\n}",
  "testCases": [
    {"input": "5"},
    {"input": "10"}
  ]
}
```

> Only the first test case (`5`) is executed in RUN mode.

### JUDGE Mode

Executes the program against **all** test cases, compares output, and returns aggregated results.

```json
{
  "mode": "JUDGE",
  "language": "PYTHON",
  "sourceCode": "a, b = map(int, input().split())\nprint(a + b)",
  "testCases": [
    {"input": "2 3", "expectedOutput": "5"},
    {"input": "10 20", "expectedOutput": "30"},
    {"input": "7 8", "expectedOutput": "15"}
  ]
}
```

**Response (all pass):**
```json
{
  "success": true,
  "verdict": "ACCEPTED",
  "passed": 3,
  "total": 3,
  "testCases": [
    {"order": 1, "verdict": "ACCEPTED", "executionTime": 38},
    {"order": 2, "verdict": "ACCEPTED", "executionTime": 40},
    {"order": 3, "verdict": "ACCEPTED", "executionTime": 42}
  ]
}
```

**Response (one fails):**
```json
{
  "success": false,
  "verdict": "WRONG_ANSWER",
  "passed": 2,
  "total": 3,
  "testCases": [
    {"order": 1, "verdict": "ACCEPTED"},
    {"order": 2, "verdict": "ACCEPTED"},
    {"order": 3, "verdict": "WRONG_ANSWER", "actual": "16\n"}
  ]
}
```

---

## Supported Languages

| Language | Compile Command | Run Command | Docker Image |
|----------|----------------|-------------|--------------|
| Java | `javac Main.java` | `java Main` | `eclipse-temurin:21-jdk` |
| C++ | `g++ -o main main.cpp` | `./main` | `gcc:14` |
| Python | None (interpreted) | `python3 main.py` | `python:3.12` |
| JavaScript | None (interpreted) | `node main.js` | `node:22` |

---

## Security

- **Stateless authentication** -- Bearer token via `Authorization` header
- **Rate limiting** -- 30 requests per 60 seconds per IP (public clients)
- **Docker sandbox** -- No privileged mode, all capabilities dropped, no network, read-only root FS, memory/CPU/PID limits
- **Input validation** -- Jakarta Bean Validation on all request fields
- **No sessions** -- CSRF disabled, stateless session policy

### Authentication

```bash
# Authenticated request (bypasses rate limit)
curl -X POST http://localhost:8081/api/v1/runner/execute \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{...}'

# Public request (rate-limited)
curl -X POST http://localhost:8081/api/v1/runner/execute \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `RUNNER_TOKEN` | Bearer token for API auth (required in prod) | -- |
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/code_arena` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `1234` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address | `localhost:9092` |
| `KAFKA_ENABLED` | Enable Kafka consumer | `true` |
| `WORKSPACE_ROOT` | Path for execution workspaces | system temp |
| `JAVA_OPTS` | JVM options | -- |
| `SPRING_PROFILES_ACTIVE` | Spring profile (`dev`, `prod`) | default |

### Key Settings (`application.yml`)

```yaml
runner:
  environment: DOCKER              # DOCKER or LOCAL
  concurrency:
    max-executions: 10             # concurrent execution slots
    acquire-timeout-seconds: 60
  docker:
    memory-mb: 256                 # per-container memory limit
    cpu-count: 1
    pids-limit: 64
    network-enabled: false
    read-only-root-fs: true
  rate-limit:
    enabled: true
    max-requests: 30
    window-seconds: 60
  compile:
    timeout: 10                    # seconds
  execution:
    timeout: 2                     # seconds
```

---

## Deployment

### Local Development

```bash
# Build and run locally
./mvnw spring-boot:run

# Or with Docker
docker compose up -d --build
```

### Docker Compose (Full Stack)

```bash
docker compose up -d --build
# Includes: MySQL 8.4 + Kafka KRaft + Runner
```

### AWS Free Tier (Lean Stack)

```bash
# Requires: AWS t4g.small instance (2 GB RAM, free until Dec 2026)
./deploy-aws.sh ubuntu@<AWS_IP>
# Includes: MySQL + Runner only (Kafka disabled to save memory)
```

### Oracle Cloud Always Free

```bash
# Requires: Oracle Cloud ARM VM (24 GB RAM, free forever)
./deploy.sh ubuntu@<ORACLE_IP>
# Full stack: MySQL + Kafka + Runner
```

See [DEPLOY-ORACLE.md](DEPLOY-ORACLE.md) for detailed Oracle Cloud instructions.

### Kubernetes

```bash
kubectl apply -f k8s/codearena.yaml
```

See [k8s/codearena.yaml](k8s/codearena.yaml) for the full manifest.

---

## Project Structure

```
Code-Arena/
├── src/main/java/com/codexsphere/codearena/
│   ├── CodeArenaApplication.java           # Entry point
│   ├── config/                             # Security, properties
│   ├── controller/                         # REST controllers
│   ├── dto/                                # Request/Response DTOs
│   ├── entity/                             # JPA entities
│   ├── enums/                              # Language, Verdict, Mode
│   ├── exception/                          # Custom exceptions + handler
│   ├── execution/                          # Core execution engine
│   │   ├── ExecutionManager.java           # Central orchestrator
│   │   ├── command/                        # Command builders per language
│   │   ├── compiler/                       # Language compilers
│   │   ├── concurrency/                    # Semaphore-based limiter
│   │   ├── docker/                         # Docker sandbox layer
│   │   ├── judge/                          # JUDGE mode logic
│   │   ├── process/                        # Process execution (Local/Docker)
│   │   ├── ratelimit/                      # Sliding window rate limiter
│   │   ├── runner/                         # Language runners
│   │   ├── source/                         # Source file writers
│   │   ├── validation/                     # Request validator
│   │   └── workspace/                      # Workspace management
│   ├── kafka/                              # Kafka consumer (optional)
│   ├── repository/                         # Spring Data JPA repos
│   └── service/                            # Log + test case services
├── src/main/resources/
│   ├── application.yml                     # Main config
│   ├── application-prod.yml                # Production config
│   └── db/migration/V1__init.sql           # Flyway migration
├── src/test/                               # 14 JUnit test classes
├── tests/                                  # Python integration tests
├── Dockerfile                              # Multi-stage build
├── docker-compose.yml                      # Full stack (MySQL + Kafka + Runner)
├── docker-compose.aws.yml                  # Lean stack (MySQL + Runner only)
├── deploy.sh                               # Deploy script (local/remote)
├── deploy-aws.sh                           # AWS deploy script
├── setup-vm.sh                             # VM bootstrap script
├── k8s/codearena.yaml                      # Kubernetes manifests
└── pom.xml                                 # Maven build
```

---

## Testing

### Unit Tests

```bash
./mvnw test
```

14 JUnit test classes covering:
- ExecutionManager, RequestValidator, RateLimiter, ConcurrencyLimiter
- LocalProcessExecutor, DockerProcessExecutor, ProcessExecutorFactory
- JudgeServiceImpl, TestCaseExecutor, VerdictComparator
- DockerServiceImpl, DockerResultCallback, DockerMemoryStatsCallback

### Integration Tests

```bash
# Requires running services (docker compose up)
cd tests
python run_release_suite.py
```

16 Python test files covering:
- API tests, language tests (Python, Java, C++)
- Verdict tests, limits tests, Docker security tests
- Authentication tests, rate limit tests, recovery tests
- Load tests (100, 500, 1000 concurrent requests)

---

## How It Works

### Request Lifecycle

```
1. Validate request           --> 400 if invalid
2. Acquire concurrency slot   --> 503 if busy
3. Create execution context   --> UUID execution ID
4. Create workspace           --> temp directory
5. Create Docker container    --> sandboxed (DOCKER mode)
6. Write source file          --> Main.java / main.py / etc.
7. Compile code               --> COMPILATION_ERROR if failed
8. Execute (RUN or JUDGE)     --> run command in container
9. Map response               --> ExecuteCodeResponse
10. Cleanup                   --> remove container, delete workspace, release slot
```

### Docker Sandbox

Each code execution runs in a hardened Docker container:
- **No privileged mode**
- **All Linux capabilities dropped**
- **No network access** (`network: none`)
- **Read-only root filesystem**
- **Memory limit**: 256 MB
- **CPU limit**: 1 core
- **PID limit**: 64 processes
- **User**: 1000:1000 (non-root)

### Verdict Priority

```
TIME_LIMIT_EXCEEDED    (highest priority)
MEMORY_LIMIT_EXCEEDED
OUTPUT_LIMIT_EXCEEDED
RUNTIME_ERROR
WRONG_ANSWER / ACCEPTED  (lowest priority)
```

---

## Request Limits

| Limit | Value |
|-------|-------|
| Source code size | 256 KB |
| Test cases per request | 20 |
| Input size per test case | 256 KB |
| Expected output size | 256 KB |
| stdout/stderr per process | 1 MB each |
| Compile timeout | 10 seconds |
| Execution timeout | 2 seconds |
| Concurrent executions | 10 |
| Public rate limit | 30 req / 60s |
| Docker memory per container | 256 MB |
| Docker CPU per container | 1 core |

---

## Database Schema

4 tables managed by Flyway:

| Table | Purpose |
|-------|---------|
| `execution_logs` | Execution start/completion, language, status, container info |
| `execution_metrics` | Verdict, execution time, memory, pass/fail counts |
| `test_cases` | Test case definitions per problem |
| `execution_test_case_results` | Per-case verdict, actual output, stderr |

---

## Documentation

- [overall-flow.md](overall-flow.md) -- Architecture and execution pipeline
- [run-mode-flow.md](run-mode-flow.md) -- Detailed RUN mode walkthrough
- [judge-mode-flow.md](judge-mode-flow.md) -- Detailed JUDGE mode walkthrough
- [DEPLOY-ORACLE.md](DEPLOY-ORACLE.md) -- Oracle Cloud deployment guide

---

## License

Licensed under the [Apache License 2.0](LICENSE).

---

## Author

**CodexSphere** -- [codexsphere.com](https://codexsphere.com)

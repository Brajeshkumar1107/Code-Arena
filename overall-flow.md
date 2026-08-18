# CodeArena Runner — Overall Flow

> **Project:** CodeArena Runner Service
> **Document:** Overall architecture and shared execution pipeline

---

## Table of Contents

1. Overview
2. Technology Stack
3. Entry Points
4. High-Level Architecture
5. Core Execution Pipeline (`ExecutionManager`)
6. Request Validation Flow
7. Rate Limiting Flow
8. Concurrency Flow
9. Workspace Flow
10. Docker Sandbox Flow
11. Security Flow
12. Response Mapping Flow
13. Exception Flow
14. Request Limits
15. Current Gaps

---

## 1. Overview

CodeArena Runner is a backend service that compiles, runs, and judges
user-submitted source code. It supports four languages:

- **Java**
- **Python**
- **C++**
- **JavaScript**

It supports two execution modes:

- **RUN** — execute the program once and return its output.
- **JUDGE** — execute the program against multiple test cases and return a verdict.

Execution can run on two backends, selected through configuration:

- **LOCAL** — uses `ProcessBuilder` on the host machine.
- **DOCKER** — runs inside a sandboxed Docker container.

The default backend is **DOCKER** (`runner.environment: DOCKER` in `application.yml`).

---

## 2. Technology Stack

| Layer         | Technology                                       |
|---------------|--------------------------------------------------|
| Language      | Java 21                                          |
| Framework     | Spring Boot 4.1.0                                |
| Web           | Spring WebMVC (REST API)                         |
| Security      | Spring Security (stateless, Bearer token)        |
| Messaging     | Spring Kafka (consumer only)                     |
| Persistence   | Spring Data JPA + Flyway + MySQL |
| Sandboxing    | docker-java 3.5.0                                |
| Build         | Maven                                            |
| Server Port   | 8081                                             |

Key configuration file: `src/main/resources/application.yml`

```yaml
runner:
  environment: DOCKER          # LOCAL or DOCKER
  concurrency:
    max-executions: 10         # concurrent slots
    acquire-timeout-seconds: 60
  limits:
    max-source-code-bytes: 1048576
    max-test-cases: 20
    max-input-bytes: 1048576
    max-expected-output-bytes: 1048576
  output-limits:
    max-stdout-bytes: 1048576
    max-stderr-bytes: 1048576
  rate-limit:
    enabled: true
    max-requests: 30
    window-seconds: 60
  docker:
    memory-mb: 256
    cpu-count: 1
    pids-limit: 64
    network-enabled: false
    read-only-root-fs: true
  compile:
    timeout: 10                # seconds
  execution:
    timeout: 2                 # seconds
```

---

## 3. Entry Points

There are two production entry points plus one dev-only test endpoint.

### 3.1 HTTP REST

`POST /api/v1/runner/execute` — `RunnerController.java`

- Accepts an `ExecuteCodeRequest` JSON body.
- Runs `@Valid` bean validation.
- Applies the public rate limiter **unless** the request carries a valid
  trusted-service token.
- Delegates to `ExecutionManager.execute(request)`.

### 3.2 Kafka Consumer

`submission-topic` — `SubmissionConsumer.java`

- `@KafkaListener(topics = "submission-topic", groupId = "runner-group")`.
- Consumes an `ExecuteCodeRequest` message.
- Delegates to `ExecutionManager.execute(request)` (fire-and-forget).

### 3.3 Dev-Only Test Endpoint

`GET /docker/test` — `DockerTestController.java` (active only with `@Profile("dev")`)

- Creates a Java container, runs `java -version` inside it, and removes it.
- Useful for verifying Docker connectivity.

---

## 4. High-Level Architecture

```
                    +-----------------------------+
                    |   Entry Points              |
                    |   RunnerController /        |
                    |   SubmissionConsumer        |
                    +-----------------------------+
                                   |
                                   v
                    +-----------------------------+
                    |   ExecutionManager          |  <-- Orchestrator
                    +-----------------------------+
                                   |
        +------------------------------------------------+
        |   Validation  |  Concurrency  |  Context       |
        |   Workspace   |  Docker       |  Source Writer |
        |   Compiler    |  Runner       |  Judge         |
        +------------------------------------------------+
                                   |
                    +-----------------------------+
                    |   ProcessExecutor           |  <-- LOCAL or DOCKER
                    |   LocalProcessExecutor      |
                    |   DockerProcessExecutor     |
                    +-----------------------------+
                                   |
                    +-----------------------------+
                    |   ExecutionResponseMapper   |
                    +-----------------------------+
                                   |
                                Response
```

`ExecutionManager` is the single orchestrator. It never compiles or executes
directly — it coordinates dedicated components.

---

## 5. Core Execution Pipeline

`ExecutionManager.execute()` — `ExecutionManager.java:50`

```
 Request
    |
 1. Validate request
    |
 2. Acquire execution slot
    |
 3. Create ExecutionContext (UUID executionId)
    |
 4. Create temporary workspace
    |
 5. [DOCKER only] Create + start container
    |
 6. Persist execution start (ExecutionLogService)
    |
 7. Write source file
    |
 8. Compile source code
    |
 9. Compilation failed?  --> return COMPILATION_ERROR response
    |
10. RUN or JUDGE mode?
      /             \
   RUN mode       JUDGE mode
     |                |
11. Build response via ExecutionResponseMapper
    |
12. finally: persist result, cleanup workspace/container, release slot
```

### Step-by-step

| # | Action | Implementation | Notes |
|---|--------|----------------|-------|
| 1 | Validate request | `ExecutionRequestValidator.validate()` | Rejects null, empty source, oversized payloads, invalid test cases. Throws `InvalidExecutionRequestException`. |
| 2 | Acquire slot | `ExecutionConcurrencyLimiter.acquire()` | Fair semaphore, default 10 permits, 60s wait. Throws `ExecutionConcurrencyException` if busy. |
| 3 | Create context | `DefaultExecutionContextFactory.create()` | Generates a random `UUID` executionId and stores the request. |
| 4 | Create workspace | `WorkspaceServiceImpl.createWorkspace()` | `Files.createTempDirectory("exec-")` under `runner.workspace.root-directory` (or system temp); sets POSIX permissions so the non-root container user can access it. |
| 5 | Docker container | `ExecutionManager.createDockerContainer()` | Only when `runner.environment == DOCKER`. Container runs `tail -f /dev/null` and bind-mounts the workspace at `/workspace`. Registered on the context immediately so cleanup can always find it. |
| 6 | Persist start | `ExecutionLogService.createExecutionLog()` | Best-effort. Records language, status=RUNNING, containerId, dockerImage. Rejects duplicate `submissionId` via `DuplicateSubmissionException`. |
| 7 | Write source | `SourceFileWriterFactory` → language writer | Creates `Main.java`, `main.cpp`, `main.py`, or `main.js`. |
| 8 | Compile | `LanguageCompilerFactory` → language compiler | Java/C++ invoke the process executor; Python/JS return success immediately (interpreted). |
| 9 | Compilation check | `compileResult.isSuccess()` | On failure, returns a `COMPILATION_ERROR` response and skips execution. |
| 10 | Mode branch | `request.getMode()` | `RUN` → `executeRunMode()`; `JUDGE` → `executeJudgeMode()`. |
| 11 | Map response | `ExecutionResponseMapper` | Converts `CompileResult`, `ExecutionResult`, or `JudgeResult` into `ExecuteCodeResponse`. |
| 12 | Cleanup | `ExecutionManager.persistExecution()` + `cleanup()` + `release()` | Best-effort persist result (success/failure), stop/remove container, delete workspace (5 retries), always release the concurrency slot. |

### Lifecycle guarantees

- The concurrency slot is **always released** in the `finally` block, even on error.
- Cleanup failures are logged and never replace the original result/exception.
- Docker container creation failure triggers best-effort removal and rethrows.

---

## 6. Request Validation Flow

`ExecutionRequestValidator.java`

```
 ExecuteCodeRequest
   |
   |-- null?  -------------------------> INVALID_REQUEST
   |
   |-- mode == null?  -----------------> "Execution mode is required."
   |-- language == null?  -------------> "Programming language is required."
   |
   |-- sourceCode blank?  -------------> "Source code cannot be empty."
   |-- sourceCode bytes > max?  -------> "Source code exceeds max size."
   |
   |-- testCases null/empty?  ---------> "At least one test case is required."
   |-- testCases.size() > max?  -------> "Maximum allowed test cases is N."
   |
   |-- per test case:
       |-- testCase null?  ------------> "Test case N cannot be null."
       |-- input bytes > max?  --------> "Input ... exceeds max size."
       |-- JUDGE mode:
           |-- expectedOutput null?  --> "Expected output is required."
           |-- expectedOutput > max?  -> "Expected output ... exceeds max size."
```

All failures throw `InvalidExecutionRequestException`
(mapped to `400 BAD REQUEST` by the global exception handler).

---

## 7. Rate Limiting Flow

`ExecutionRateLimiter.java` — applied by `RunnerController` **only for
non-authenticated (public) requests**.

- Sliding fixed window per client IP address.
- Default: 30 requests / 60 seconds.
- In-memory `ConcurrentHashMap` of clients with a scheduled cleanup task.
- Exceeding the limit throws `ExecutionRateLimitException`
  (mapped to `429 TOO MANY REQUESTS`).
- Authenticated trusted-service requests (`runner-service` principal)
  **bypass** the rate limiter.

```
 Request
   |
   |-- authenticated (valid token)?
   |       | yes --> skip rate limit
   |       | no  --> checkRateLimit(clientIp)
   |                     |
   |                     |-- within window?  --> increment, allow
   |                     |-- exceeded?       --> 429 error
```

---

## 8. Concurrency Flow

`ExecutionConcurrencyLimiter.java`

- Backed by a **fair `Semaphore`** with N permits
  (`runner.concurrency.max-executions`, default 10).
- `acquire()` uses `tryAcquire(timeout)` — waits up to
  `acquire-timeout-seconds` (default 60).
- If no slot is available → `ExecutionConcurrencyException`
  (mapped to `503 SERVICE UNAVAILABLE` / service-busy message).
- `release()` is guaranteed via the `finally` block in `ExecutionManager`.

This protects the runner from being overwhelmed by concurrent executions.

---

## 9. Workspace Flow

`WorkspaceServiceImpl.java`

```
 Create
   Files.createTempDirectory("exec-")
   set POSIX permissions (owner/group rwx, others r-x)
   Workspace{ root } attached to ExecutionContext
     |
   Use
   Source file + input.txt + compiled artifacts live here
     |
   Delete (cleanup)
   Files.walk(root) sorted reverse-order, delete each
   Retries up to 5 times with 200ms backoff
```

- Local mode: processes execute with the workspace as the working directory.
- Docker mode: the workspace is bind-mounted into the container at `/workspace`,
  so the container sees source, input, and compiled files.
- Permissions are relaxed (755) so the container's non-root user
  (`1000:1000`) can read/write the mounted directory.

---

## 10. Docker Sandbox Flow

```
 DockerClient (docker-java)
     |
     |-- DefaultDockerContainerFactory.create(metadata, workspace)
     |     |-- ensureImageExists()  -> pull image if missing
     |     |-- HostConfig:
     |     |     |-- Bind workspace -> /workspace
     |     |     |-- memory limit (256 MB)
     |     |     |-- nano CPUs (1)
     |     |     |-- pids limit (64)
     |     |     |-- privileged=false
     |     |     |-- drop ALL capabilities
     |     |     |-- no-new-privileges
     |     |     |-- read-only root filesystem
     |     |     |-- network mode: none (or bridge)
     |     |-- user 1000:1000
     |     |-- working dir /workspace
     |     |-- command: tail -f /dev/null   (keep-alive)
     |
     |-- startContainer() -> runs keep-alive
     |
     |-- docker exec <command>  (attach stdout/stderr/stdin)
     |     |-- DockerResultCallback     -> captures output, enforces byte limits
     |     |-- DockerMemoryStatsCallback -> samples peak memory
     |     |-- timeout deadline poll loop
     |     |-- OOMKilled detection via inspect
     |
     |-- cleanup:
           stopContainer -> removeContainer
```

Sandbox hardening (all via `HostConfig`):
- No privileged mode.
- All Linux capabilities dropped.
- `no-new-privileges: true`.
- Read-only root filesystem.
- No network access.
- Strict memory / CPU / pids limits.

---

## 11. Security Flow

`SecurityConfig.java` + `RunnerTokenFilter.java`

- **Stateless** session policy, CSRF disabled.
- `POST /api/v1/runner/execute` and `GET /actuator/health` are `permitAll`.
- Everything else requires authentication.
- `RunnerTokenFilter`:
  - Reads `Authorization: Bearer <token>`.
  - Compares against `runner.security.token`.
  - On match, sets principal `runner-service` in the security context.
  - On mismatch/malformed header → `401 Unauthorized`.
  - No token → treated as anonymous (rate limiter applies).

The `runner-service` principal is how the controller distinguishes
trusted internal services from public clients.

---

## 12. Response Mapping Flow

`DefaultExecutionResponseMapper.java`

Three internal models are mapped to the shared `ExecuteCodeResponse`:

| Source model | Trigger | Verdict derivation |
|--------------|---------|--------------------|
| `CompileResult` | Compilation failed | `COMPILATION_ERROR` |
| `ExecutionResult` | RUN mode | timeout → `TIME_LIMIT_EXCEEDED`, memory → `MEMORY_LIMIT_EXCEEDED`, output → `OUTPUT_LIMIT_EXCEEDED`, exit != 0 → `RUNTIME_ERROR`, else `ACCEPTED` |
| `JudgeResult` | JUDGE mode | verdict passed through from the judge aggregator |

`ExecuteCodeResponse` fields:

```json
{
  "success": true,
  "verdict": "ACCEPTED",
  "stdout": "...",
  "stderr": "...",
  "passed": 0,
  "total": 0,
  "executionTime": 12,
  "memoryUsed": 45,
  "testCases": []
}
```

---

## 13. Exception Flow

`GlobalExceptionHandler.java` — a `@RestControllerAdvice`

```
                Exception
                   |
   +---------------+--------------+
   |               |              |
 BaseException   Validation     Generic
 (custom)        (@Valid)       Exception
   |               |              |
 HTTP status    400 BAD      500 INTERNAL
 from exception  REQUEST      SERVER ERROR
   |               |              |
   +---------------+--------------+
                   |
            ErrorResponse
              timestamp
              status
              error
              message
              path
```

Custom exception hierarchy (`exception/` package):

- `BaseException` (abstract root)
  - `InvalidExecutionRequestException`
  - `WorkspaceException`
  - `SourceFileException`
  - `CompilationException`
  - `ExecutionException`
  - `ProcessExecutionException`
  - `JudgeException`
  - `DockerExecutionException`
  - `ExecutionRateLimitException`
  - `ExecutionConcurrencyException`
  - `DuplicateSubmissionException`

Each custom exception exposes its own HTTP status, which the handler uses
to build the `ErrorResponse`.

---

## 14. Request Limits (Summary)

| Limit | Default | Enforced by |
|-------|---------|-------------|
| Source code size | 1 MB | `ExecutionRequestValidator` |
| Test cases per request | 20 | `ExecutionRequestValidator` |
| Input size per test case | 1 MB | `ExecutionRequestValidator` |
| Expected output size | 1 MB | `ExecutionRequestValidator` |
| stdout / stderr per process | 1 MB each | `DockerResultCallback` (Docker) |
| Compile timeout | 10 s | `ProcessRequest.timeout` |
| Execution timeout | 2 s | `ProcessRequest.timeout` |
| Concurrent executions | 10 | `ExecutionConcurrencyLimiter` |
| Public rate limit | 30 req / 60 s | `ExecutionRateLimiter` |
| Docker memory | 256 MB | `DefaultResourceConfigurer` |
| Docker CPU | 1 core | `DefaultResourceConfigurer` |
| Docker pids | 64 | `DefaultResourceConfigurer` |

---

## 15. Current Gaps

### Resolved in 1.0.0

- **Persistence wired.** `ExecutionLogService` is now called from
  `ExecutionManager.execute()`: a log entry is created at execution start,
  completed on success/error. `ExecutionMetric` and per-test-case results
  are persisted. `submissionId`/`problemId` are optional correlation IDs
  on `ExecuteCodeRequest`.
- **Dead code removed.** `WorkspaceCleaner`, `CommandBuilderFactory`/`Impl`,
  `ExecutionLogMapper`, `WorkingDirectoryResolver`, and the `executor/`
  language subpackages have been deleted.
- **Legacy config keys cleaned.** `runner.java.*`, `runner.python.*`,
  `runner.cpp.*`, `runner.execution.timeout-seconds`,
  `runner.workspace.auto-cleanup`, and `topics.*` removed from
  `application.yml`.
- **Docker host configurable.** `runner.docker.host` and per-language images
  (`runner.languages.*`) are now bindable.
- **Workspace root configurable.** `runner.workspace.root-directory` supports
  docker-outside-of-docker deployments where the workspace path must match
  between host and container.
- **Deployment artifacts.** `Dockerfile` (multi-stage, non-root 1000:1000),
  `.dockerignore`, `docker-compose.yml` (MySQL 8.4 + Kafka KRaft + runner),
  and `k8s/codearena.yaml` (Deployment + Service + ServiceAccount) added.
- **Flyway migration.** `V1__init.sql` covers all persistence tables.
  Production profile uses `ddl-auto: validate` + Flyway (Spring Boot 4.1
  requires `spring-boot-starter-flyway`).
- **Prod fail-fast.** `RunnerSecurityStartupValidator` refuses to start
  if `runner.security.enabled=true` and `RUNNER_TOKEN` is blank.
- **Docker timeout bug fixed.** `timedOut` flag was never set; the monitor
  loop now correctly breaks and returns a timeout result.
- **Empty expectedOutput treated as no-assertion.** `DefaultVerdictComparator`
  returns `ACCEPTED` when `expectedOutput` is blank — the program must exit
  cleanly but stdout is not compared.

### Known Issues (not regressions)

- **Python syntax errors → RUNTIME_ERROR, not COMPILATION_ERROR.**
  Python is interpreted; syntax errors are caught at import time, not at
  a separate compilation step. The runner correctly classifies them as
  `RUNTIME_ERROR` (exit code 1).
- **Java "Main method not found" → RUNTIME_ERROR, not COMPILATION_ERROR.**
  The Java compiler succeeds but the JVM throws at startup. This is
  pre-existing and correct per the language runtime.
- **Security filter ordering.** Unknown endpoints return 401 (security
  filter) before the controller returns 404. Acceptable for a token-guarded
  service.

### Remaining (Kafka — out of scope for this release)

- **Kafka result publishing is missing.** `SubmissionConsumer` consumes
  `ExecuteCodeRequest` and calls `ExecutionManager`, but no result is ever
  published back to a result topic.
- **Kafka event DTOs unused.** `SubmissionEvent` is consumed by
  `SubmissionConsumer` but `ResultEvent` is never published.

---

## Related Documents

- `Runner-Architecture-Sprint1.md` — original sprint-1 architecture document.
- `run-mode-flow.md` — detailed RUN mode walkthrough.
- `judge-mode-flow.md` — detailed JUDGE mode walkthrough.

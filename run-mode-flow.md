# CodeArena Runner — RUN Mode Flow

> **Project:** CodeArena Runner Service
> **Document:** Detailed RUN mode execution flow

---

## Table of Contents

1. What is RUN Mode?
2. Entry into RUN Mode
3. Step-by-Step Flow
4. Command Building
5. Process Execution
6. Result Mapping
7. Verdict Decisions
8. Example Request / Response
9. Failure Paths

---

## 1. What is RUN Mode?

RUN mode executes a compiled program **once**, feeding it the input of
**the first test case only**. It returns the program's stdout (and stderr)
to the client. There is no comparison against expected output.

Typical use case: a client wants to see what a program prints for a given
input before submitting it for judging.

---

## 2. Entry into RUN Mode

`ExecutionManager.execute()` branches after compilation:

```java
if (request.getMode() == ExecutionMode.RUN) {
    return executeRunMode(context);   // RunnerController.java / SubmissionConsumer
}
```

Entry point: `ExecutionManager.executeRunMode()` — `ExecutionManager.java:313`

Prerequisites (already done by the time RUN starts):

- Request validated.
- Concurrency slot acquired.
- ExecutionContext created (with UUID `executionId`).
- Workspace created.
- Docker container created/started (in DOCKER environment).
- Source file written.
- Code compiled successfully.

---

## 3. Step-by-Step Flow

```
 Compile success
      |
      v
 executeRunMode(context)
      |
      | 1. Set current test case = testCases[0]
      |
      | 2. runnerFactory.getRunner(language)   -> LanguageRunner
      |
      | 3. runner.run(context)                 -> ExecutionResult
      |      |
      |      |-- commandBuilder.build(context) -> ProcessRequest
      |      |-- processExecutorFactory.getExecutor() -> ProcessExecutor
      |      |-- executor.execute(request)      -> ProcessResult
      |      |-- map ProcessResult -> ExecutionResult
      |
      | 4. responseMapper.fromExecutionResult(executionResult)
      |
      v
 ExecuteCodeResponse
```

### Detailed table

| # | Step | Implementation | What happens |
|---|------|----------------|--------------|
| 1 | Set test case | `context.setCurrentTestCase(request.getTestCases().get(0))` | Only the first test case's input is used in RUN mode. |
| 2 | Get runner | `LanguageRunnerFactory.getRunner(language)` | Returns `JavaLanguageRunner`, `CppLanguageRunner`, `PythonLanguageRunner`, or `JavaScriptLanguageRunner`. |
| 3 | Run | `LanguageRunner.run(context)` | Builds a `ProcessRequest`, executes it, and produces an `ExecutionResult`. |
| 4 | Map | `ExecutionResponseMapper.fromExecutionResult()` | Converts the execution result into the API response with a verdict. |

---

## 4. Command Building

Each language has a command builder. They all follow the same pattern but
differ in the actual command and input handling.

### Environment switch

Every builder checks `runnerProperties.getEnvironment()`:

- **DOCKER** → working directory is `/workspace`, input is fed through
  an `input.txt` file mounted into the container.
- **LOCAL** → working directory is the host workspace, input is fed through
  the process stdin stream.

### Java — `JavaRunCommandBuilder.java`

1. Resolves metadata (`Main.java`, executable `Main`).
2. Writes the test-case input to `{workspace}/input.txt`
   (appends a trailing newline if missing).
3. Builds the command:

   | Environment | Command |
   |-------------|---------|
   | DOCKER      | `sh -c "java Main < /workspace/input.txt"` |
   | LOCAL       | `java Main` |

4. Builds `ProcessRequest`:

   | Field | DOCKER | LOCAL |
   |-------|--------|-------|
   | input (stdin) | `null` (file redirection) | `ByteArrayInputStream` of input |
   | inputFile | `{workspace}/input.txt` | `{workspace}/input.txt` |
   | workingDirectory | `/workspace` | `{workspace}/root` |
   | timeout | execution timeout (default 2 s) | same |
   | containerId | container id | `null` |

### Python — `PythonRunCommandBuilder.java`

Same pattern as Java:

| Environment | Command |
|-------------|---------|
| DOCKER      | `sh -c "python3 main.py < /workspace/input.txt"` |
| LOCAL       | `python3 main.py` |

### C++ — `CppRunCommandBuilder.java`

Runs the compiled binary:

| Environment | Command |
|-------------|---------|
| DOCKER      | `sh -c "./main < /workspace/input.txt"` |
| LOCAL       | `./main` |

### JavaScript — `JavaScriptRunCommandBuilder.java`

Runs the interpreter:

| Environment | Command |
|-------------|---------|
| DOCKER      | `sh -c "node main.js < /workspace/input.txt"` |
| LOCAL       | `node main.js` |

---

## 5. Process Execution

`LanguageRunner.run()` gets the process executor from
`ProcessExecutorFactory.getExecutor()`:

- `runner.environment == DOCKER` → `DockerProcessExecutor`
- otherwise → `LocalProcessExecutor`

### LocalProcessExecutor (host)

1. `ProcessBuilder(command)` with workspace as directory.
2. Starts the process.
3. Drains stdout and stderr concurrently on a 2-thread pool
   (prevents pipe-buffer deadlock).
4. Writes stdin to the process if provided; closes stdin otherwise.
5. `waitFor(timeout)` with the configured execution timeout.
6. Timeout → destroy, 500 ms grace, then `destroyForcibly()` → returns a
   timeout result.
7. Otherwise collects stdout/stderr, exit code, and execution time.

### DockerProcessExecutor (sandbox)

1. Delegates to `DockerServiceImpl.execute(containerId, request)`.
2. Runs `docker exec` inside the container with stdout/stderr attached.
3. `DockerResultCallback` captures output and enforces stdout/stderr
   byte limits → `outputLimitExceeded`.
4. `DockerMemoryStatsCallback` streams memory statistics in parallel.
5. A poll loop enforces the execution deadline → timeout → `killContainer`
   (`SIGKILL`).
6. After completion, inspects the exec for the exit code and the container
   for `OOMKilled` → `memoryLimitExceeded`.

---

## 6. Result Mapping

### ProcessResult → ExecutionResult

`JavaLanguageRunner.java:36` (identical in all runners):

```java
ExecutionResult.builder()
    .success(!timeout && !memoryLimitExceeded
             && !outputLimitExceeded && exitCode == 0)
    .timeout(result.isTimeout())
    .stdout(result.getStdout())
    .stderr(result.getStderr())
    .memoryUsedMb(result.getMemoryUsedMb())
    .exitCode(result.getExitCode())
    .executionTimeMillis(result.getExecutionTimeMillis())
    .memoryLimitExceeded(result.isMemoryLimitExceeded())
    .outputLimitExceeded(result.isOutputLimitExceeded())
    .build();
```

### ExecutionResult → ExecuteCodeResponse

`DefaultExecutionResponseMapper.fromExecutionResult()` — `ExecutionResponseMapper.java:43`

---

## 7. Verdict Decisions

The verdict is derived in this order:

```
 ExecutionResult
     |
     |-- isTimeout? ----------------> TIME_LIMIT_EXCEEDED
     |-- isMemoryLimitExceeded? ----> MEMORY_LIMIT_EXCEEDED
     |-- isOutputLimitExceeded? ----> OUTPUT_LIMIT_EXCEEDED
     |-- !isSuccess() (exit != 0) -> RUNTIME_ERROR
     |-- else ---------------------> ACCEPTED
```

| Condition | Verdict | success flag |
|-----------|---------|--------------|
| Timeout | `TIME_LIMIT_EXCEEDED` | false |
| Memory limit exceeded | `MEMORY_LIMIT_EXCEEDED` | false |
| Output limit exceeded | `OUTPUT_LIMIT_EXCEEDED` | false |
| Non-zero exit code | `RUNTIME_ERROR` | false |
| Everything normal | `ACCEPTED` | true |

Note: RUN mode does **not** compare output to any expected value — it
simply reports execution success and prints the output.

---

## 8. Example Request / Response

### Request (LOCAL environment, RUN mode)

```json
{
  "mode": "RUN",
  "language": "JAVA",
  "sourceCode": "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt();\n        int b = sc.nextInt();\n        System.out.println(a + b);\n    }\n}\n",
  "testCases": [
    { "input": "2 3" },
    { "input": "10 20" }
  ]
}
```

> Only the **first** test case (`2 3`) is executed in RUN mode.

### Response — ACCEPTED

```json
{
  "success": true,
  "verdict": "ACCEPTED",
  "stdout": "5\n",
  "stderr": "",
  "passed": 0,
  "total": 0,
  "executionTime": 45,
  "memoryUsed": 32,
  "testCases": []
}
```

### Response — TIME_LIMIT_EXCEEDED

```json
{
  "success": false,
  "verdict": "TIME_LIMIT_EXCEEDED",
  "stdout": "",
  "stderr": "Process execution timed out.",
  "passed": 0,
  "total": 0,
  "executionTime": 2005,
  "memoryUsed": 0,
  "testCases": []
}
```

### Response — RUNTIME_ERROR

```json
{
  "success": false,
  "verdict": "RUNTIME_ERROR",
  "stdout": "",
  "stderr": "Exception in thread \"main\" java.lang.ArithmeticException: / by zero",
  "passed": 0,
  "total": 0,
  "executionTime": 12,
  "memoryUsed": 28,
  "testCases": []
}
```

---

## 9. Failure Paths

| Failure | Where detected | Outcome |
|---------|----------------|---------|
| Invalid request | `ExecutionRequestValidator` | `400 BAD REQUEST` (no slot consumed) |
| Rate limit exceeded | `ExecutionRateLimiter` | `429 TOO MANY REQUESTS` (public HTTP only) |
| Concurrency busy | `ExecutionConcurrencyLimiter` | `503` busy response |
| Workspace creation fails | `WorkspaceServiceImpl` | `WorkspaceException` → mapped status |
| Docker container fails | `ExecutionManager.createDockerContainer` | best-effort remove, error rethrown |
| Source write fails | `SourceFileWriter` | `SourceFileException` |
| Compilation fails | `CompileResult.isSuccess() == false` | `COMPILATION_ERROR` response, execution skipped |
| Process spawn fails | `ProcessExecutor` | `ProcessExecutionException` / `DockerExecutionException` |
| Timeout / OOM / output-limit | `ProcessExecutor` | Verdict-based response (TLE/MLE/OLE) |

Cleanup always runs in the `finally` block: container stopped/removed,
workspace deleted, concurrency slot released.

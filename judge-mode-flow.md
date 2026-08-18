# CodeArena Runner — JUDGE Mode Flow

> **Project:** CodeArena Runner Service
> **Document:** Detailed JUDGE mode execution flow

---

## Table of Contents

1. What is JUDGE Mode?
2. Entry into JUDGE Mode
3. Step-by-Step Flow
4. Per-Test-Case Execution
5. Verdict Determination Order
6. Output Comparison
7. Result Aggregation
8. Final Verdict Logic
9. Response Mapping
10. Example Request / Response
11. Failure Paths

---

## 1. What is JUDGE Mode?

JUDGE mode executes a compiled program against **all** test cases in the
request. For each test case the actual output is compared against the
expected output, a per-case verdict is computed, and the results are
aggregated into a single overall verdict (with passed/total counts).

Typical use case: grading a competitive-programming submission.

---

## 2. Entry into JUDGE Mode

`ExecutionManager.execute()` branches after compilation:

```java
return executeJudgeMode(context);   // ExecutionManager.java:358
```

Entry point: `ExecutionManager.executeJudgeMode()` — `ExecutionManager.java:358`

Prerequisites (already done by the time JUDGE starts):

- Request validated (each test case must have `expectedOutput` in JUDGE mode).
- Concurrency slot acquired.
- ExecutionContext created.
- Workspace created.
- Docker container created/started (in DOCKER environment).
- Source file written.
- Code compiled **once** — compilation never repeats across test cases.

---

## 3. Step-by-Step Flow

```
 Compile success
      |
      v
 executeJudgeMode(context)
      |
      | judgeService.judge(context)
      |      |
      |      for each test case:
      |      |  testCaseExecutor.execute(context, testCase, order)
      |      |      |-- set current test case
      |      |      |-- runner.run(context)          -> ExecutionResult
      |      |      |-- determineVerdict()           -> per-case Verdict
      |      |      `-- build TestCaseResult
      |
      | aggregate: passed count, total time, max memory,
      |            first-failure stderr, final verdict
      |
      | responseMapper.fromJudgeResult(judgeResult)
      |
      v
 ExecuteCodeResponse
```

---

## 4. Per-Test-Case Execution

`JudgeServiceImpl.judge()` — `JudgeServiceImpl.java:21`

```java
for (int i = 0; i < testCases.size(); i++) {
    TestCaseResult result =
        testCaseExecutor.execute(context, testCase, i + 1);
    results.add(result);
    // accumulate executionTime, max memory
}
```

`TestCaseExecutorImpl.execute()` — `TestCaseExecutorImpl.java:21`

| # | Step | Implementation |
|---|------|----------------|
| 1 | Select case | `context.setCurrentTestCase(testCase)` — the runner reads input from this. |
| 2 | Get runner | `runnerFactory.getRunner(language)` |
| 3 | Run | `languageRunner.run(context)` → `ExecutionResult` (same as RUN mode) |
| 4 | Verdict | `determineVerdict(testCase, executionResult)` |
| 5 | Build result | `TestCaseResult{ order, input, expected, actual, stderr, verdict, executionTime, memoryUsed }` |

The command-building and process-execution internals are identical to RUN
mode — see `run-mode-flow.md` sections 4 and 5.

---

## 5. Verdict Determination Order

`TestCaseExecutorImpl.determineVerdict()` — `TestCaseExecutorImpl.java:59`

```
 ExecutionResult
     |
     |-- isTimeout()? ----------------> TIME_LIMIT_EXCEEDED
     |-- isMemoryLimitExceeded()? ----> MEMORY_LIMIT_EXCEEDED
     |-- isOutputLimitExceeded()? ----> OUTPUT_LIMIT_EXCEEDED
     |-- exitCode != 0? --------------> RUNTIME_ERROR
     |-- else ------------------------> comparator.compare(testCase, result)
```

The limit checks take **priority** over exit code and output comparison:

| Priority | Condition | Verdict |
|----------|-----------|---------|
| 1 | Process timed out | `TIME_LIMIT_EXCEEDED` |
| 2 | Memory limit exceeded | `MEMORY_LIMIT_EXCEEDED` |
| 3 | Output limit exceeded | `OUTPUT_LIMIT_EXCEEDED` |
| 4 | Non-zero exit code | `RUNTIME_ERROR` |
| 5 | Output mismatch/ok | compare (see below) |

---

## 6. Output Comparison

`DefaultVerdictComparator.compare()` — `DefaultVerdictComparator.java`

```
 expected = testCase.expectedOutput.trim()   (null -> "")
 actual   = result.stdout.trim()             (null -> "")
 expected.equals(actual)?
     | yes -> ACCEPTED
     | no  -> WRONG_ANSWER
```

Normalization rules:

- Both sides are trimmed (leading/trailing whitespace removed).
- Comparison is **exact** after trimming — no case folding, no
  whitespace-collapse, no numeric tolerance.
- A trailing newline difference is tolerated because trimming removes it.

---

## 7. Result Aggregation

`JudgeServiceImpl.judge()` aggregation:

| Metric | Rule |
|--------|------|
| `passed` | count of test cases with verdict `ACCEPTED` |
| `total` | number of test cases |
| `executionTime` | **sum** of all test-case execution times (ms) |
| `memoryUsed` | **max** memory across test cases (MB) |
| `stderr` | first non-blank stderr among failed test cases |
| `stdout` | always `null` (each case has its own actual output) |

---

## 8. Final Verdict Logic

`JudgeServiceImpl.java:67`

```
 passed == total?
     | yes -> ACCEPTED
     | no  -> first non-ACCEPTED verdict in execution order
               (fallback WRONG_ANSWER)
```

- If **every** test case passes → `ACCEPTED`.
- Otherwise the overall verdict is the **first** failed verdict
  encountered in test-case order.
- The fallback (defensive) is `WRONG_ANSWER`.

---

## 9. Response Mapping

`DefaultExecutionResponseMapper.fromJudgeResult()` — `ExecutionResponseMapper.java:100`

- `success` = `verdict == ACCEPTED`
- `verdict` = final verdict
- `passed` / `total` = aggregated counts
- `executionTime` / `memoryUsed` = aggregated metrics
- `stderr` = first failed case's stderr
- `stdout` = `""` (empty)
- `testCases` = list of `TestCaseResult` (per-case verdicts)

---

## 10. Example Request / Response

### Request (JUDGE mode)

```json
{
  "mode": "JUDGE",
  "language": "PYTHON",
  "sourceCode": "a, b = map(int, input().split())\nprint(a + b)\n",
  "testCases": [
    { "input": "2 3",   "expectedOutput": "5" },
    { "input": "10 20", "expectedOutput": "30" },
    { "input": "7 8",   "expectedOutput": "15" }
  ]
}
```

### Response — ACCEPTED (all pass)

```json
{
  "success": true,
  "verdict": "ACCEPTED",
  "stdout": "",
  "stderr": "",
  "passed": 3,
  "total": 3,
  "executionTime": 120,
  "memoryUsed": 18,
  "testCases": [
    { "order": 1, "input": "2 3",   "expected": "5",  "actual": "5\n",  "stderr": "", "verdict": "ACCEPTED", "executionTime": 38, "memoryUsed": 18 },
    { "order": 2, "input": "10 20", "expected": "30", "actual": "30\n", "stderr": "", "verdict": "ACCEPTED", "executionTime": 40, "memoryUsed": 18 },
    { "order": 3, "input": "7 8",   "expected": "15", "actual": "15\n", "stderr": "", "verdict": "ACCEPTED", "executionTime": 42, "memoryUsed": 18 }
  ]
}
```

### Response — WRONG_ANSWER (case 3 fails)

```json
{
  "success": false,
  "verdict": "WRONG_ANSWER",
  "stdout": "",
  "stderr": "",
  "passed": 2,
  "total": 3,
  "executionTime": 118,
  "memoryUsed": 18,
  "testCases": [
    { "order": 1, "input": "2 3",   "expected": "5",  "actual": "5\n",  "stderr": "", "verdict": "ACCEPTED",      "executionTime": 38, "memoryUsed": 18 },
    { "order": 2, "input": "10 20", "expected": "30", "actual": "30\n", "stderr": "", "verdict": "ACCEPTED",      "executionTime": 40, "memoryUsed": 18 },
    { "order": 3, "input": "7 8",   "expected": "15", "actual": "16\n", "stderr": "", "verdict": "WRONG_ANSWER",  "executionTime": 40, "memoryUsed": 18 }
  ]
}
```

### Response — TIME_LIMIT_EXCEEDED (case 2 hangs)

```json
{
  "success": false,
  "verdict": "TIME_LIMIT_EXCEEDED",
  "stdout": "",
  "stderr": "Process execution timed out.",
  "passed": 1,
  "total": 3,
  "executionTime": 4020,
  "memoryUsed": 18,
  "testCases": [
    { "order": 1, "input": "2 3",   "expected": "5",  "actual": "5\n",  "stderr": "",                "verdict": "ACCEPTED",          "executionTime": 40, "memoryUsed": 18 },
    { "order": 2, "input": "10 20", "expected": "30", "actual": "",     "stderr": "Process execution timed out.", "verdict": "TIME_LIMIT_EXCEEDED", "executionTime": 2005, "memoryUsed": 18 },
    { "order": 3, "input": "7 8",   "expected": "15", "actual": "15\n", "stderr": "",                "verdict": "ACCEPTED",          "executionTime": 40, "memoryUsed": 18 }
  ]
}
```

---

## 11. Failure Paths

| Failure | Where detected | Outcome |
|---------|----------------|---------|
| Missing `expectedOutput` in JUDGE mode | `ExecutionRequestValidator` | `400 BAD REQUEST` before any execution |
| Workspace / Docker / source-write / compile failures | shared pipeline | same handling as RUN mode (see `run-mode-flow.md` section 9) |
| Individual test case times out / OOM / output-limit / crashes | `TestCaseExecutorImpl.determineVerdict` | that case gets TLE / MLE / OLE / RUNTIME_ERROR; remaining cases still execute |
| Entire process fails mid-judging | `ProcessExecutor` | `ProcessExecutionException` / `DockerExecutionException` → error response |

Cleanup always runs in the `finally` block: container stopped/removed,
workspace deleted, concurrency slot released — regardless of verdict.

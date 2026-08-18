"""06 - Resource and size limit boundary tests.

Constants mirror application.yml (max-source-code-bytes=262144,
max-input-bytes=262144, max-test-cases=20, memory-mb=256).
"""
from common import Config, Result, run_payload, post, check_ok, load_config

ACCEPTED = "ACCEPTED"
MAX_SRC = 262144
MAX_IN = 262144
MAX_TC = 20


def run(conf: Config) -> Result:
    res = Result()

    # --- input boundary ---
    for n in (MAX_IN - 1, MAX_IN):
        check_ok(conf, res, f"IN-0 input at {n} bytes accepted",
                 run_payload("print(len(input()))", "PYTHON", inp="x" * n,
                             expected=str(n)), ACCEPTED)

    status, body, raw, _ = post(conf, run_payload(
        "print(len(input()))", "PYTHON", inp="x" * (MAX_IN + 1)))
    res.check("IN-1 input over limit rejected",
              status == 400 and isinstance(body, dict),
              detail=f"status={status} body={raw[:200]}")

    # --- source boundary ---
    base = "print(1)\n#"
    for n in (MAX_SRC - 1, MAX_SRC):
        src = base + "a" * (n - len(base))
        check_ok(conf, res, f"SRC source at {n} bytes accepted",
                 run_payload(src, "PYTHON", expected="1"), ACCEPTED)

    src = base + "a" * (MAX_SRC + 1 - len(base))
    status, body, raw, _ = post(conf, run_payload(src, "PYTHON", expected="1"))
    res.check("SRC source over limit rejected",
              status == 400 and isinstance(body, dict),
              detail=f"status={status} body={raw[:200]}")

    # --- test-case boundary ---
    tcs = [{"input": "1", "expectedOutput": "1"} for _ in range(MAX_TC)]
    status, body, raw, _ = post(conf, {"mode": "JUDGE", "language": "PYTHON",
                                       "sourceCode": "print(input())",
                                       "testCases": tcs})
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("TC at 20 accepted",
              status == 200 and verdict == ACCEPTED,
              detail=f"status={status} verdict={verdict}")

    tcs = [{"input": "1", "expectedOutput": "1"} for _ in range(MAX_TC + 1)]
    status, body, raw, _ = post(conf, {"mode": "JUDGE", "language": "PYTHON",
                                       "sourceCode": "print(input())",
                                       "testCases": tcs})
    res.check("TC at 21 rejected",
              status == 400 and isinstance(body, dict),
              detail=f"status={status} body={raw[:200]}")

    # --- memory ---
    check_ok(conf, res, "MEM-1 normal memory",
             run_payload("print('ok')", "PYTHON", expected="ok"), ACCEPTED)
    check_ok(conf, res, "MEM-2 ~100 MB",
             run_payload("x = bytearray(100 * 1024 * 1024)\nprint(len(x))",
                         "PYTHON", expected=str(100 * 1024 * 1024)), ACCEPTED)
    check_ok(conf, res, "MEM-3 over limit -> OOM",
             run_payload("x = bytearray(1024 * 1024 * 1024)\nprint('x')",
                         "PYTHON"), "MEMORY_LIMIT_EXCEEDED")
    check_ok(conf, res, "MEM-4 recovery after OOM",
             run_payload("print('ok')", "PYTHON", expected="ok"), ACCEPTED)

    # --- timeout ---
    check_ok(conf, res, "TIME-1 fast program",
             run_payload("import time; time.sleep(0.1); print('ok')", "PYTHON",
                         expected="ok"), ACCEPTED)
    check_ok(conf, res, "TIME-2 > 2s program -> TLE",
             run_payload("import time; time.sleep(5); print('ok')", "PYTHON"),
             "TIME_LIMIT_EXCEEDED")
    check_ok(conf, res, "TIME-3 recovery after timeout",
             run_payload("print('ok')", "PYTHON", expected="ok"), ACCEPTED)

    # --- compile timeout (10s) ---
    slow = "public class Main { public static void main(String[] a) { }\n" + \
           "".join(f"void m{i}() {{ }}\n" for i in range(20000)) + "}"
    status, body, raw, _ = post(conf, run_payload(slow, "JAVA", expected=""))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("TIME-4 slow compile contained",
              status == 200 and verdict in ("COMPILATION_ERROR",
                                            "TIME_LIMIT_EXCEEDED"),
              detail=f"status={status} verdict={verdict}")

    # --- pid limit ---
    status, body, raw, _ = post(conf, run_payload(
        "import subprocess\n"
        "ps = [subprocess.Popen(['sleep', '10']) for _ in range(200)]\n"
        "print(len(ps))", "PYTHON"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("PID over limit contained",
              status == 200 and verdict in ("RUNTIME_ERROR",
                                            "TIME_LIMIT_EXCEEDED",
                                            "MEMORY_LIMIT_EXCEEDED"),
              detail=f"status={status} verdict={verdict}")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Limits tests: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  FAIL", f)

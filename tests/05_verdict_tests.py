"""05 - Verdict handling and output comparison tests."""
from common import Config, Result, run_payload, post, load_config

EXPECTED_FIELDS = {"success", "verdict", "stdout", "stderr", "passed",
                   "total", "executionTime", "memoryUsed", "testCases"}


def judge(src, expected, inp=""):
    return {"mode": "JUDGE", "language": "PYTHON", "sourceCode": src,
            "testCases": [{"input": inp, "expectedOutput": expected}]}


def run(conf: Config) -> Result:
    res = Result()

    verdict_cases = [
        ("VER-01 ACCEPTED", judge("print('ok')", "ok"), "ACCEPTED"),
        ("VER-02 WRONG_ANSWER", judge("print('a')", "b"), "WRONG_ANSWER"),
        ("VER-03 COMPILATION_ERROR", judge("def f(:", "x"), "COMPILATION_ERROR"),
        ("VER-04 RUNTIME_ERROR", judge("raise ValueError()", "x"), "RUNTIME_ERROR"),
        ("VER-05 TIME_LIMIT_EXCEEDED", judge("while True: pass", "x"),
         "TIME_LIMIT_EXCEEDED"),
        ("VER-06 MEMORY_LIMIT_EXCEEDED",
         judge("x = 'a' * (1024 * 1024 * 1024)", "x"), "MEMORY_LIMIT_EXCEEDED"),
        ("VER-07 OUTPUT_LIMIT_EXCEEDED",
         judge("print('x' * (1024 * 1024 * 2))", "x"), "OUTPUT_LIMIT_EXCEEDED"),
    ]
    for name, payload, expected in verdict_cases:
        status, body, raw, _ = post(conf, payload)
        verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
        res.check(name, status == 200 and verdict == expected,
                  detail=f"status={status} verdict={verdict} body={raw[:200]}")

    status, body, raw, _ = post(conf, judge("print('ok')", "ok"))
    body = body if isinstance(body, dict) else {}
    missing = EXPECTED_FIELDS - set(body.keys())
    res.check("VER-08 response schema complete", status == 200 and not missing,
              detail=f"status={status} missing={sorted(missing)} body={raw[:300]}")
    res.check("VER-09 response success flag", body.get("success") is True,
              detail=f"success={body.get('success')}")
    res.check("VER-10 counters populated", body.get("passed") == 1
              and body.get("total") == 1,
              detail=f"passed={body.get('passed')} total={body.get('total')}")

    out_cases = [
        ("OUT-01 exact match", "print('hi')", "hi", True),
        ("OUT-02 trailing newline", "print('hi')", "hi\n", True),
        ("OUT-03 leading whitespace", "print('hi')", "   hi", True),
        ("OUT-04 trailing whitespace", "print('hi')", "hi   ", True),
        ("OUT-05 multiple spaces", "print('hi')", "h i", False),
        ("OUT-06 empty expected", "print('')", "", True),
        ("OUT-07 whitespace-only expected", "print('')", "   ", True),
        ("OUT-08 empty actual", "pass", "x", False),
        ("OUT-09 wrong capitalization", "print('Hi')", "hi", False),
        ("OUT-10 wrong multiline order", "print('a'); print('b')", "b\na", False),
        ("OUT-11 missing line", "print('a')", "a\nb", False),
        ("OUT-12 extra line", "print('a'); print('b')", "a", False),
        ("OUT-13 special characters", "print('a\\tb\\n\\u0000')", "a\tb\n\u0000", True),
        ("OUT-14 unicode", "print('café ☕')", "café ☕", True),
        ("OUT-15 large output", "print('y' * 100000)", "y" * 100000, True),
    ]
    for name, src, expected, should_pass in out_cases:
        status, body, raw, _ = post(conf, judge(src, expected))
        verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
        want = "ACCEPTED" if should_pass else "WRONG_ANSWER"
        res.check(name, status == 200 and verdict == want,
                  detail=f"status={status} verdict={verdict} body={raw[:200]}")

    status, body, raw, _ = post(conf, judge(
        "import sys\nsys.stderr.write('e' * (1024 * 1024 * 2))\nprint('ok')", "ok"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("OUT-16 large stderr contained",
              status == 200 and verdict in ("OUTPUT_LIMIT_EXCEEDED", "ACCEPTED"),
              detail=f"status={status} verdict={verdict}")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Verdict tests: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  FAIL", f)

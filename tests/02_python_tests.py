"""02 - Python execution regression tests."""
import os

from common import Config, Result, run_payload, check_ok, post, load_config

ACCEPTED = "ACCEPTED"
CONTAINED = ("RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED",
             "MEMORY_LIMIT_EXCEEDED", "OUTPUT_LIMIT_EXCEEDED")


def run(conf: Config) -> Result:
    res = Result()

    cases = [
        ("PY-01 hello world", "print('hello world')", ""),
        ("PY-02 arithmetic", "print(2 + 3 * 4)", ""),
        ("PY-03 variables", "x = 10\ny = 20\nprint(x + y)", ""),
        ("PY-04 functions", "def add(a, b):\n    return a + b\nprint(add(2, 3))", ""),
        ("PY-05 loops", "s = 0\nfor i in range(1, 11):\n    s += i\nprint(s)", ""),
        ("PY-06 lists", "print([1, 2, 3][1])", ""),
        ("PY-07 dictionaries", "print({'a': 1, 'b': 2}['b'])", ""),
        ("PY-08 classes", "class A:\n    def __init__(self):\n        self.x = 5\na = A()\nprint(a.x)", ""),
        ("PY-09 stdlib", "import math\nprint(math.sqrt(16))", ""),
        ("PY-10 input", "print(input())", "42"),
        ("PY-11 multiple inputs", "a = int(input())\nb = int(input())\nprint(a + b)", "1\n2"),
        ("PY-12 multiline input", "lines = []\nfor _ in range(3):\n    lines.append(input())\nprint(len(lines))", "a\nb\nc"),
        ("PY-13 unicode input", "print(len(input()))", "héllo"),
        ("PY-14 unicode output", "print('café ☕')", ""),
        ("PY-15 special chars", "print('a\\tb\\n\\u0000')", ""),
        ("PY-16 empty input", "print('ok')", ""),
        ("PY-17 large input", "print(len(input()))", "x" * 100_000),
        ("PY-18 large output", "print('y' * 100_000)", ""),
    ]
    for name, src, inp in cases:
        check_ok(conf, res, name, run_payload(src, "PYTHON", inp), ACCEPTED)

    check_ok(conf, res, "PY-19 runtime exception",
             run_payload("raise ValueError('boom')", "PYTHON"), "RUNTIME_ERROR")
    check_ok(conf, res, "PY-20 zero division",
             run_payload("print(1 // 0)", "PYTHON"), "RUNTIME_ERROR")
    check_ok(conf, res, "PY-21 infinite loop",
             run_payload("while True: pass", "PYTHON"), "TIME_LIMIT_EXCEEDED")
    check_ok(conf, res, "PY-22 memory exhaustion",
             run_payload("x = 'a' * (1024 * 1024 * 1024)", "PYTHON"),
             "MEMORY_LIMIT_EXCEEDED")

    status, body, raw, _ = post(conf, run_payload(
        "import subprocess\nwhile True: subprocess.Popen(['true'])", "PYTHON"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("PY-23 process spawn contained",
              status == 200 and verdict in CONTAINED,
              detail=f"status={status} verdict={verdict} body={raw[:200]}")

    status, body, raw, _ = post(conf, run_payload(
        "import subprocess\nsubprocess.run(['ls'], check=True)", "PYTHON"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    stdout = (body or {}).get("stdout", "") if isinstance(body, dict) else ""
    res.check("PY-24 no escape via subprocess",
              status == 200 and verdict in (ACCEPTED, *CONTAINED),
              detail=f"status={status} verdict={verdict}")

    status, body, raw, _ = post(conf, run_payload(
        "open('/etc/evil.txt', 'w').write('x')", "PYTHON"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("PY-25 read-only rootfs blocks write",
              status == 200 and verdict in CONTAINED,
              detail=f"status={status} verdict={verdict} body={raw[:200]}")

    status, body, raw, _ = post(conf, run_payload(
        "import os\nprint(os.environ)", "PYTHON"))
    stdout = (body or {}).get("stdout", "") if isinstance(body, dict) else ""
    leaked = [k for k in ("RUNNER_TOKEN", "DB_PASSWORD", "DB_USERNAME", "DB_URL")
              if k in stdout]
    res.check("PY-26 env does not leak secrets",
              not leaked,
              detail=f"leaked={leaked} stdout={stdout[:200]}")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Python tests: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  FAIL", f)

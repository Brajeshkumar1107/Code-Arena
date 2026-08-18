"""10 - Recovery and container-lifecycle tests."""
import subprocess

from common import Config, Result, run_payload, check_ok, load_config


def docker_ids():
    try:
        out = subprocess.run(
            ["docker", "ps", "-a", "--no-trunc", "--format",
             "{{.ID}} {{.Image}}"],
            capture_output=True, text=True, timeout=30,
            check=True)
        return {line.split()[0] for line in out.stdout.splitlines()}
    except Exception:
        return None


def run(conf: Config) -> Result:
    res = Result()

    failures = [
        ("REC-01 after normal", run_payload("print('ok')", "PYTHON", expected="ok"),
         "ACCEPTED"),
        ("REC-02 after runtime error",
         run_payload("raise ValueError()", "PYTHON"), "RUNTIME_ERROR"),
        ("REC-03 after compile error",
         run_payload("def f(:", "PYTHON"), "COMPILATION_ERROR"),
        ("REC-04 after timeout",
         run_payload("import time; time.sleep(5)", "PYTHON"),
         "TIME_LIMIT_EXCEEDED"),
        ("REC-05 after OOM",
         run_payload("x = 'a' * (1024 * 1024 * 1024)", "PYTHON"),
         "MEMORY_LIMIT_EXCEEDED"),
        ("REC-06 after output limit",
         run_payload("print('x' * (1024 * 1024 * 2))", "PYTHON"),
         "OUTPUT_LIMIT_EXCEEDED"),
    ]
    for name, payload, expected in failures:
        check_ok(conf, res, name, payload, expected)
        check_ok(conf, res, name.replace("after", "recovers after"),
                 run_payload("print('ok')", "PYTHON", expected="ok"), "ACCEPTED")

    before = docker_ids()
    if before is None:
        res.skip("REC-07 container leak check", "docker CLI unavailable")
    else:
        for _ in range(5):
            check_ok(conf, res, f"REC-07.0 container normal batch #{_}",
                     run_payload("print('ok')", "PYTHON", expected="ok"),
                     "ACCEPTED")
        after = docker_ids()
        leaked = after - before if after is not None else None
        res.check("REC-07 no leaked containers after batch",
                  leaked is not None and not leaked,
                  detail=f"leaked={leaked}")

    res.skip("REC-08 docker restart recovery", "manual")
    res.skip("REC-09 db failure recovery", "manual")
    res.skip("REC-10 app restart recovery", "manual")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Recovery tests: {r.passed} passed, {r.failed} failed, {r.skipped} skipped")
    for f in r.failures:
        print("  ", f)

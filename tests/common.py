"""Shared helpers for the CodeArena release test suite (stdlib only).

Environment variables (all optional):
    BASE_URL          base URL of the runner, default http://localhost:8081
    RUNNER_TOKEN      bearer token for authenticated requests
    REQUEST_TIMEOUT   per-request timeout in seconds, default 120
    SKIP_SLOW_TESTS   set to 0 to run slow tests (window-wait), default 1
"""
import json
import os
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from typing import Any, Optional

AUTO = object()


@dataclass
class Config:
    base_url: str
    token: str
    timeout: float
    skip_slow: bool


def load_config() -> Config:
    return Config(
        base_url=os.environ.get("BASE_URL", "http://localhost:8081").rstrip("/"),
        token=os.environ.get("RUNNER_TOKEN", ""),
        timeout=float(os.environ.get("REQUEST_TIMEOUT", "120")),
        skip_slow=os.environ.get("SKIP_SLOW_TESTS", "1")
        not in ("0", "false", "no"),
    )


def post(
    config: Config,
    payload: dict,
    token: Optional[str] = AUTO,
    timeout: Optional[float] = None,
    extra_headers: Optional[dict] = None,
):
    if token is AUTO:
        token = config.token or None
    body = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token is not None:
        headers["Authorization"] = f"Bearer {token}"
    if extra_headers:
        headers.update(extra_headers)
    req = urllib.request.Request(
        config.base_url + "/api/v1/runner/execute",
        data=body,
        headers=headers,
        method="POST",
    )
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout or config.timeout) as resp:
            raw = resp.read()
            elapsed = time.perf_counter() - t0
            parsed = _parse(raw)
            return resp.status, parsed, raw.decode("utf-8", "replace"), elapsed
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        elapsed = time.perf_counter() - t0
        return exc.code, _parse(raw), raw.decode("utf-8", "replace"), elapsed
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        return None, None, str(exc), time.perf_counter() - t0


def get(config: Config, path: str, token: Optional[str] = AUTO,
        timeout: Optional[float] = None):
    if token is AUTO:
        token = config.token or None
    headers = {}
    if token is not None:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(config.base_url + path, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout or config.timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            return resp.status, _parse(raw.encode()), raw
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", "replace")
        return exc.code, _parse(raw.encode()), raw
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        return None, None, str(exc)


def _parse(raw: bytes) -> Any:
    try:
        return json.loads(raw)
    except Exception:
        return None


@dataclass
class Result:
    passed: int = 0
    failed: int = 0
    skipped: int = 0
    failures: list = field(default_factory=list)

    def check(self, name: str, ok: bool, detail: str = "") -> None:
        if ok:
            self.passed += 1
        else:
            self.failed += 1
            self.failures.append(f"{name} :: {detail}")

    def skip(self, name: str, detail: str = "") -> None:
        self.skipped += 1
        self.failures.append(f"{name} :: SKIPPED {detail}")

    def merge(self, other: "Result") -> None:
        self.passed += other.passed
        self.failed += other.failed
        self.skipped += other.skipped
        self.failures.extend(other.failures)


def run_payload(source: str, language: str, inp: str = "",
                expected: str = "") -> dict:
    return {
        "mode": "JUDGE",
        "language": language,
        "sourceCode": source,
        "testCases": [{"input": inp, "expectedOutput": expected}],
    }


def hello(language: str) -> str:
    return {
        "PYTHON": "print('hello')",
        "JAVA": "public class Main { public static void main(String[] a) "
                "{ System.out.println(\"hello\"); } }",
        "CPP": "#include <iostream>\nint main() { std::cout << \"hello\""
               " << std::endl; return 0; }",
        "JAVASCRIPT": "console.log('hello');",
    }[language]


def check_ok(conf: Config, res: Result, name: str, payload: dict,
             expect_verdict: str, token: Optional[str] = AUTO) -> dict:
    if token is AUTO:
        token = conf.token or None
    status, body, raw, _ = post(conf, payload, token=token)
    got = (body or {}).get("verdict") if isinstance(body, dict) else None
    ok = status == 200 and got == expect_verdict
    res.check(name, ok,
              detail=f"status={status} verdict={got} expected={expect_verdict} "
                     f"body={raw[:300]}")
    return body or {}


def load_run(conf: Config, total: int, label: str) -> Result:
    """Fire `total` concurrent authenticated executions.

    Returns a Result plus metrics attached to the Result (result.metrics).
    """
    import threading
    from concurrent.futures import ThreadPoolExecutor

    payload = run_payload("print('ok')", "PYTHON", expected="ok")
    lock = threading.Lock()
    counts = {}
    latencies = []
    timeouts = 0
    errors = 0
    failures = []

    def one(_):
        nonlocal timeouts, errors
        status, body, raw, elapsed = post(
            conf, payload, token=conf.token or None, timeout=conf.timeout)
        with lock:
            counts[status] = counts.get(status, 0) + 1
            if elapsed is not None and status is not None:
                latencies.append(elapsed * 1000.0)
            if status is None:
                timeouts += 1
                if "timed out" in raw.lower() or "timed out" in str(raw).lower():
                    pass
            if isinstance(body, dict) and body.get("verdict") != "ACCEPTED":
                failures.append(raw[:200])
            return status

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=min(total, 200)) as pool:
        list(pool.map(one, range(total)))
    duration = time.perf_counter() - t0

    latencies.sort()
    def pct(p):
        if not latencies:
            return 0.0
        idx = min(len(latencies) - 1, int(len(latencies) * p))
        return round(latencies[idx], 1)

    ok = counts.get(200, 0)
    res = Result(passed=ok, failed=total - ok)
    if failures:
        res.failures = [f"{label} non-ACCEPTED: {f}" for f in failures[:5]]
    res.metrics = {
        "label": label,
        "total": total,
        "ok": ok,
        "timeouts": timeouts,
        "statuses": counts,
        "avg_ms": round(sum(latencies) / len(latencies), 1) if latencies else 0.0,
        "p95_ms": pct(0.95),
        "p99_ms": pct(0.99),
        "duration_s": round(duration, 1),
    }
    return res

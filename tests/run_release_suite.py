#!/usr/bin/env python3
"""CodeArena release test suite runner.

Usage:
    python3 run_release_suite.py
    python3 run_release_suite.py --only 01 05          # subset by module number
    python3 run_release_suite.py --skip-load            # skip 11/12/13

Requires a running runner service (default http://localhost:8081).
Set RUNNER_TOKEN so functional suites bypass the anonymous rate limiter.
"""
import importlib
import importlib.util
import os
import sys

TESTS_DIR = os.path.dirname(os.path.abspath(__file__))


def load_module(num: str, module_name: str):
    """Load a test module from its file (modules are prefixed by number)."""
    path = os.path.join(TESTS_DIR, f"{num}_{module_name}.py")
    spec = importlib.util.spec_from_file_location(f"suite_{num}_{module_name}", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


SUITES = [
    ("01", "api_tests", "API validation", False),
    ("02", "python_tests", "Python execution", False),
    ("03", "java_tests", "Java execution", False),
    ("04", "cpp_tests", "C++ execution", False),
    ("05", "verdict_tests", "Verdict handling", False),
    ("06", "limits_tests", "Resource limits", False),
    ("07", "docker_security_tests", "Docker security", False),
    ("08", "auth_tests", "Authentication", False),
    ("09", "rate_limit_tests", "Rate limiting", False),
    ("10", "recovery_tests", "Recovery", False),
    ("11", "load_100", "100 concurrent", True),
    ("12", "load_500", "500 concurrent", True),
    ("13", "load_1000", "1000 concurrent", True),
]


def main():
    args = sys.argv[1:]
    only = set()
    skip_load = False
    i = 0
    while i < len(args):
        if args[i] == "--only":
            only.update(args[i + 1].split(","))
            i += 2
        elif args[i] == "--skip-load":
            skip_load = True
            i += 1
        else:
            print(f"Unknown argument: {args[i]}")
            return 2
    conf = importlib.import_module("common").load_config()

    rows = []
    blockers_failed = 0
    for num, module_name, label, is_load in SUITES:
        if only and num not in only:
            continue
        if skip_load and is_load:
            continue
        module = load_module(num, module_name)
        result = module.run(conf)
        rows.append((num, label, result, is_load))
        if not is_load:
            blockers_failed += result.failed

    width = 64
    print("=" * width)
    print(f"{'CODEARENA RELEASE TEST':^{width}}")
    print(f"base: {conf.base_url}" + (f" token: set" if conf.token else " token: UNSET (set RUNNER_TOKEN)"))
    print("=" * width)

    total_p = total_f = total_s = 0
    print(f"{'Suite':<26} {'Result':>12} {'Notes':<24}")
    print("-" * width)
    for num, label, result, is_load in rows:
        total_p += result.passed
        total_f += result.failed
        total_s += result.skipped
        status = f"{result.passed}/{result.passed + result.failed}"
        if result.failed:
            status += "  FAIL"
        else:
            status += "  PASS"
        note = f"{result.skipped} skipped" if result.skipped else ""
        print(f"{num} {label:<22} {status:>12} {note:<24}")
    print("-" * width)
    print(f"{'TOTAL':<26} {'':>12}")
    print(f"  passed: {total_p}   failed: {total_f}   skipped: {total_s}")
    print()

    metrics = [r.metrics for _, _, r, is_load in rows if is_load and hasattr(r, "metrics")]
    if metrics:
        print(f"{'LOAD / CAPACITY':^{width}}")
        for m in metrics:
            verdict = "PASS" if m["ok"] == m["total"] else "WARN"
            print(f"  {m['label']:<16} {m['ok']}/{m['total']:<6} "
                  f"timeouts={m['timeouts']:<5} avg={m['avg_ms']}ms "
                  f"p95={m['p95_ms']}ms p99={m['p99_ms']}ms "
                  f"dur={m['duration_s']}s  {verdict}")
        print()

    for num, label, result, is_load in rows:
        for failure in result.failures:
            print(f"[{num}] {failure}")
    print()

    if blockers_failed:
        print(f"RELEASE BLOCKERS: {blockers_failed} FAILURES")
        print("RELEASE: NOT READY")
        return 1
    print("RELEASE BLOCKERS: 0")
    print("CODEARENA: READY FOR RELEASE (capacity warnings above are optional)")
    return 0


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    raise SystemExit(main())

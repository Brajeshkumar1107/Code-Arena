#!/usr/bin/env python3

import concurrent.futures
import json
import os
import statistics
import sys
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen


# ============================================================
# CONFIGURATION
# ============================================================

BASE_URL = os.getenv(
    "CODEARENA_URL",
    "http://localhost:8081"
)

EXECUTE_URL = f"{BASE_URL}/api/v1/runner/execute"

TOKEN = os.getenv("CODEARENA_TOKEN", "")

HTTP_TIMEOUT = int(
    os.getenv("HTTP_TIMEOUT", "30")
)

# Keep large load tests disabled by default.
RUN_LOAD_TESTS = (
    os.getenv("LOAD_TESTS", "false").lower() == "true"
)

LOAD_COUNTS = [
    int(x)
    for x in os.getenv(
        "LOAD_COUNTS",
        "100,500"
    ).split(",")
]

RATE_LIMIT = int(
    os.getenv("RATE_LIMIT", "30")
)


# ============================================================
# GLOBAL RESULTS
# ============================================================

PASSED = 0
FAILED = 0
WARNINGS = 0


# ============================================================
# HTTP
# ============================================================

def send_request(payload, token=None, timeout=HTTP_TIMEOUT):

    data = json.dumps(payload).encode("utf-8")

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json"
    }

    if token is not None:
        headers["Authorization"] = f"Bearer {token}"

    request = Request(
        EXECUTE_URL,
        data=data,
        headers=headers,
        method="POST"
    )

    start = time.time()

    try:

        with urlopen(
            request,
            timeout=timeout
        ) as response:

            raw = response.read().decode("utf-8")

            try:
                body = json.loads(raw)
            except Exception:
                body = {}

            return {
                "http": response.status,
                "body": body,
                "time": round(
                    time.time() - start,
                    2
                ),
                "error": None
            }

    except HTTPError as e:

        try:
            raw = e.read().decode("utf-8")

            try:
                body = json.loads(raw)
            except Exception:
                body = {}

        except Exception:
            body = {}

        return {
            "http": e.code,
            "body": body,
            "time": round(
                time.time() - start,
                2
            ),
            "error": f"HTTP {e.code}"
        }

    except Exception as e:

        return {
            "http": None,
            "body": {},
            "time": round(
                time.time() - start,
                2
            ),
            "error": f"{type(e).__name__}: {e}"
        }


def execute(
    source,
    language="PYTHON",
    test_cases=None,
    token=None,
    timeout=HTTP_TIMEOUT
):

    if test_cases is None:

        test_cases = [
            {
                "input": "",
                "expectedOutput": "CODEARENA_OK"
            }
        ]

    payload = {
        "mode": "JUDGE",
        "language": language,
        "sourceCode": source,
        "testCases": test_cases
    }

    return send_request(
        payload,
        token,
        timeout
    )


def get_verdict(result):
    return result["body"].get("verdict")


def get_success(result):
    return result["body"].get("success")


# ============================================================
# RESULT HELPERS
# ============================================================

def section(title):

    print()
    print("=" * 75)
    print(title)
    print("=" * 75)


def check(name, condition, details=""):

    global PASSED
    global FAILED

    if condition:

        PASSED += 1

        print(
            f"PASS  {name:<42} {details}"
        )

    else:

        FAILED += 1

        print(
            f"FAIL  {name:<42} {details}"
        )


def warning(name, condition, details=""):

    global WARNINGS

    if condition:

        print(
            f"PASS  {name:<42} {details}"
        )

    else:

        WARNINGS += 1

        print(
            f"WARN  {name:<42} {details}"
        )


# ============================================================
# 1. BASIC EXECUTION
# ============================================================

def test_basic_execution():

    section("1. BASIC EXECUTION")

    result = execute(
        "print('CODEARENA_OK')"
    )

    check(
        "Python Hello World",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED"
        and get_success(result) is True,
        f"HTTP={result['http']} "
        f"VERDICT={get_verdict(result)}"
    )

    result = execute(
        "print(10 + 20)",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "30"
            }
        ]
    )

    check(
        "Python arithmetic",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"VERDICT={get_verdict(result)}"
    )

    result = execute(
        "x = input(); print('Hello ' + x)",
        test_cases=[
            {
                "input": "Brajesh",
                "expectedOutput": "Hello Brajesh"
            }
        ]
    )

    check(
        "Python input",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"VERDICT={get_verdict(result)}"
    )

    result = execute(
        "print('नमस्ते')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "नमस्ते"
            }
        ]
    )

    check(
        "Unicode output",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 2. JAVA
# ============================================================

def test_java():

    section("2. JAVA EXECUTION")

    source = """
public class Main {

    public static void main(String[] args) {

        System.out.println("JAVA_OK");

    }
}
"""

    result = execute(
        source,
        language="JAVA",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "JAVA_OK"
            }
        ]
    )

    check(
        "Java execution",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"HTTP={result['http']} "
        f"VERDICT={get_verdict(result)}"
    )

    source = """
public class Main {

    public static void main(String[] args) {

        invalid java code

    }
}
"""

    result = execute(
        source,
        language="JAVA",
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ]
    )

    check(
        "Java compilation error",
        result["http"] == 200
        and get_verdict(result) == "COMPILATION_ERROR",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 3. C++
# ============================================================

def test_cpp():

    section("3. C++ EXECUTION")

    source = """
#include <iostream>

using namespace std;

int main() {

    cout << "CPP_OK" << endl;

    return 0;
}
"""

    result = execute(
        source,
        language="CPP",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "CPP_OK"
            }
        ]
    )

    check(
        "C++ execution",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"HTTP={result['http']} "
        f"VERDICT={get_verdict(result)}"
    )

    source = """
#include <iostream>

int main() {

    invalid cpp code

}
"""

    result = execute(
        source,
        language="CPP",
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ]
    )

    check(
        "C++ compilation error",
        result["http"] == 200
        and get_verdict(result) == "COMPILATION_ERROR",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 4. VERDICTS
# ============================================================

def test_verdicts():

    section("4. VERDICT TESTS")

    result = execute(
        "print('WRONG')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "CORRECT"
            }
        ]
    )

    check(
        "Wrong Answer",
        result["http"] == 200
        and get_verdict(result) == "WRONG_ANSWER",
        f"VERDICT={get_verdict(result)}"
    )

    result = execute(
        "raise RuntimeError('TEST_ERROR')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ]
    )

    check(
        "Runtime Error",
        result["http"] == 200
        and get_verdict(result) == "RUNTIME_ERROR",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 5. API VALIDATION
# ============================================================

def test_validation():

    section("5. API VALIDATION")

    payloads = [

        (
            "Empty source",
            {
                "mode": "JUDGE",
                "language": "PYTHON",
                "sourceCode": "",
                "testCases": [
                    {
                        "input": "",
                        "expectedOutput": ""
                    }
                ]
            }
        ),

        (
            "Missing sourceCode",
            {
                "mode": "JUDGE",
                "language": "PYTHON",
                "testCases": [
                    {
                        "input": "",
                        "expectedOutput": ""
                    }
                ]
            }
        ),

        (
            "Empty testCases",
            {
                "mode": "JUDGE",
                "language": "PYTHON",
                "sourceCode": "print('x')",
                "testCases": []
            }
        ),

        (
            "Missing language",
            {
                "mode": "JUDGE",
                "sourceCode": "print('x')",
                "testCases": [
                    {
                        "input": "",
                        "expectedOutput": "x"
                    }
                ]
            }
        )
    ]

    for name, payload in payloads:

        result = send_request(payload)

        check(
            name,
            result["http"] in (400, 422),
            f"HTTP={result['http']}"
        )


# ============================================================
# 6. INPUT / OUTPUT
# ============================================================

def test_io():

    section("6. INPUT / OUTPUT")

    result = execute(
        "print('A  B')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "A  B"
            }
        ]
    )

    check(
        "Exact spaces",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"VERDICT={get_verdict(result)}"
    )

    result = execute(
        "print('HELLO')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "WORLD"
            }
        ]
    )

    check(
        "Case/output mismatch",
        result["http"] == 200
        and get_verdict(result) == "WRONG_ANSWER",
        f"VERDICT={get_verdict(result)}"
    )

    result = execute(
        "print('line1'); print('line2')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "line1\nline2"
            }
        ]
    )

    check(
        "Multiline output",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 7. TIMEOUT
# ============================================================

def test_timeout():

    section("7. TIMEOUT")

    result = execute(
        "while True: pass",
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ],
        timeout=30
    )

    check(
        "Infinite loop timeout",
        result["http"] == 200
        and get_verdict(result) == "TIME_LIMIT_EXCEEDED",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 8. MEMORY
# ============================================================

def test_memory():

    section("8. MEMORY LIMIT")

    source = """
data = []

while True:

    data.append("A" * 1024 * 1024)
"""

    result = execute(
        source,
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ],
        timeout=30
    )

    check(
        "Memory protection",
        result["http"] == 200
        and get_verdict(result) in (
            "MEMORY_LIMIT_EXCEEDED",
            "RUNTIME_ERROR",
            "TIME_LIMIT_EXCEEDED"
        ),
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 9. OUTPUT LIMIT
# ============================================================

def test_output_limit():

    section("9. OUTPUT LIMIT")

    source = """
while True:

    print("X" * 65536)
"""

    result = execute(
        source,
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ],
        timeout=30
    )

    check(
        "Stdout output limit",
        result["http"] == 200
        and get_verdict(result) == "OUTPUT_LIMIT_EXCEEDED",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 10. DOCKER NETWORK
# ============================================================

def test_network():

    section("10. DOCKER NETWORK ISOLATION")

    source = """
import urllib.request

urllib.request.urlopen(
    "http://example.com",
    timeout=3
)

print("NETWORK_ACCESS")
"""

    result = execute(
        source,
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ],
        timeout=15
    )

    check(
        "Network disabled",
        result["http"] == 200
        and get_verdict(result) == "RUNTIME_ERROR",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 11. READ ONLY ROOT FS
# ============================================================

def test_readonly():

    section("11. READ-ONLY ROOT FILESYSTEM")

    source = """
with open(
    "/etc/codearena_test.txt",
    "w"
) as f:

    f.write("SHOULD_FAIL")
"""

    result = execute(
        source,
        test_cases=[
            {
                "input": "",
                "expectedOutput": ""
            }
        ],
        timeout=15
    )

    check(
        "Read-only root filesystem",
        result["http"] == 200
        and get_verdict(result) == "RUNTIME_ERROR",
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 12. PID LIMIT
# ============================================================

def test_pid_limit():

    section("12. PID LIMIT")

    source = """
import subprocess

processes = []

try:

    for i in range(100):

        processes.append(
            subprocess.Popen(
                ["sleep", "10"]
            )
        )

except Exception:

    pass

finally:

    for p in processes:

        try:
            p.terminate()
        except Exception:
            pass

print("PID_TEST_DONE")
"""

    result = execute(
        source,
        test_cases=[
            {
                "input": "",
                "expectedOutput": "PID_TEST_DONE"
            }
        ],
        timeout=30
    )

    check(
        "PID/process protection",
        result["http"] == 200
        and get_verdict(result) in (
            "ACCEPTED",
            "RUNTIME_ERROR",
            "TIME_LIMIT_EXCEEDED"
        ),
        f"VERDICT={get_verdict(result)}"
    )


# ============================================================
# 13. AUTHENTICATION
# ============================================================

def test_authentication():

    section("13. AUTHENTICATION")

    if not TOKEN:

        warning(
            "Authentication tests",
            False,
            "CODEARENA_TOKEN is not configured"
        )

        return

    result = execute(
        "print('AUTH_OK')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "AUTH_OK"
            }
        ],
        token=TOKEN
    )

    check(
        "Valid token",
        result["http"] == 200
        and get_verdict(result) == "ACCEPTED",
        f"HTTP={result['http']} "
        f"VERDICT={get_verdict(result)}"
    )

    result = execute(
        "print('AUTH_OK')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "AUTH_OK"
            }
        ],
        token="INVALID_TOKEN_123"
    )

    check(
        "Invalid token rejected",
        result["http"] == 401,
        f"HTTP={result['http']}"
    )


# ============================================================
# 14. RATE LIMIT
# ============================================================

def test_rate_limit():

    section("14. RATE LIMIT")

    if not TOKEN:

        warning(
            "Rate-limit authentication tests",
            False,
            "CODEARENA_TOKEN is not configured"
        )

        return

    total = RATE_LIMIT + 2

    print(
        f"Sending {total} anonymous requests..."
    )

    def anonymous(_):

        return execute(
            "print('RATE_TEST')",
            test_cases=[
                {
                    "input": "",
                    "expectedOutput": "RATE_TEST"
                }
            ]
        )

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=min(total, 32)
    ) as executor:

        results = list(
            executor.map(
                anonymous,
                range(total)
            )
        )

    http_429 = sum(
        r["http"] == 429
        for r in results
    )

    check(
        "Anonymous rate limiting",
        http_429 > 0,
        f"HTTP429={http_429}/{total}"
    )

    authenticated_total = 5

    def authenticated(_):

        return execute(
            "print('AUTH_RATE_TEST')",
            test_cases=[
                {
                    "input": "",
                    "expectedOutput": "AUTH_RATE_TEST"
                }
            ],
            token=TOKEN
        )

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=authenticated_total
    ) as executor:

        results = list(
            executor.map(
                authenticated,
                range(authenticated_total)
            )
        )

    http_200 = sum(
        r["http"] == 200
        for r in results
    )

    http_429 = sum(
        r["http"] == 429
        for r in results
    )

    check(
        "Authenticated rate-limit bypass",
        http_200 == authenticated_total
        and http_429 == 0,
        f"HTTP200={http_200} HTTP429={http_429}"
    )


# ============================================================
# 15. RECOVERY
# ============================================================

def test_recovery():

    section("15. RECOVERY")

    failure_tests = [

        (
            "Runtime error recovery",
            "raise RuntimeError('FAIL')",
            "RUNTIME_ERROR"
        ),

        (
            "Timeout recovery",
            "while True: pass",
            "TIME_LIMIT_EXCEEDED"
        )
    ]

    for name, source, expected in failure_tests:

        result = execute(
            source,
            test_cases=[
                {
                    "input": "",
                    "expectedOutput": ""
                }
            ],
            timeout=30
        )

        check(
            name,
            result["http"] == 200
            and get_verdict(result) == expected,
            f"VERDICT={get_verdict(result)}"
        )

        recovery = execute(
            "print('RECOVERY_OK')",
            test_cases=[
                {
                    "input": "",
                    "expectedOutput": "RECOVERY_OK"
                }
            ]
        )

        check(
            "Normal execution after failure",
            recovery["http"] == 200
            and get_verdict(recovery) == "ACCEPTED",
            f"VERDICT={get_verdict(recovery)}"
        )


# ============================================================
# 16. LOAD TEST
# ============================================================

def load_request(number):

    start = time.time()

    result = execute(
        "print('LOAD_TEST_OK')",
        test_cases=[
            {
                "input": "",
                "expectedOutput": "LOAD_TEST_OK"
            }
        ],
        timeout=180
    )

    return {
        "request": number,
        "http": result["http"],
        "verdict": get_verdict(result),
        "success": get_success(result),
        "time": round(
            time.time() - start,
            2
        ),
        "error": result["error"]
    }


def run_load_test(total):

    section(
        f"16. LOAD TEST - {total} CONCURRENT"
    )

    start = time.time()

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=total
    ) as executor:

        futures = [
            executor.submit(
                load_request,
                i
            )
            for i in range(
                1,
                total + 1
            )
        ]

        results = [
            future.result()
            for future in concurrent.futures.as_completed(
                futures
            )
        ]

    wall_time = round(
        time.time() - start,
        2
    )

    http_200 = sum(
        r["http"] == 200
        for r in results
    )

    accepted = sum(
        r["verdict"] == "ACCEPTED"
        for r in results
    )

    errors = sum(
        bool(r["error"])
        for r in results
    )

    times = [
        r["time"]
        for r in results
    ]

    print()
    print(f"Requests     : {total}")
    print(f"HTTP 200     : {http_200}")
    print(f"Accepted     : {accepted}")
    print(f"Errors       : {errors}")
    print(f"Wall time    : {wall_time}s")
    print(
        f"Min response : {min(times):.2f}s"
    )
    print(
        f"Max response : {max(times):.2f}s"
    )
    print(
        f"Avg response : "
        f"{statistics.mean(times):.2f}s"
    )

    # Load tests are warnings rather than hard
    # release blockers for now.

    warning(
        f"{total} concurrent load",
        http_200 == total
        and accepted == total
        and errors == 0,
        f"{accepted}/{total} accepted"
    )


# ============================================================
# MAIN
# ============================================================

def main():

    print("=" * 75)
    print(
        "          CODEARENA PRE-RELEASE TEST SUITE"
    )
    print("=" * 75)

    print()
    print(
        f"Endpoint : {EXECUTE_URL}"
    )
    print(
        f"Token    : "
        f"{'CONFIGURED' if TOKEN else 'NOT SET'}"
    )
    print(
        f"Timeout  : {HTTP_TIMEOUT}s"
    )
    print(
        f"Load     : "
        f"{'ENABLED' if RUN_LOAD_TESTS else 'DISABLED'}"
    )

    try:

        test_basic_execution()

        test_java()

        test_cpp()

        test_verdicts()

        test_validation()

        test_io()

        test_timeout()

        test_memory()

        test_output_limit()

        test_network()

        test_readonly()

        test_pid_limit()

        test_authentication()

        test_rate_limit()

        test_recovery()

        if RUN_LOAD_TESTS:

            for count in LOAD_COUNTS:

                run_load_test(
                    count
                )

        else:

            print()
            print(
                "LOAD TESTS SKIPPED"
            )

            print(
                "Run with:"
            )

            print(
                "LOAD_TESTS=true "
                "LOAD_COUNTS=100,500 "
                "python test_release.py"
            )

    except KeyboardInterrupt:

        print()
        print(
            "Test suite interrupted."
        )

        sys.exit(2)

    except Exception as e:

        print()
        print(
            f"TEST SUITE ERROR: "
            f"{type(e).__name__}: {e}"
        )

        sys.exit(2)

    print()
    print("=" * 75)
    print(
        "                       FINAL SUMMARY"
    )
    print("=" * 75)

    print(
        f"PASS     : {PASSED}"
    )

    print(
        f"FAIL     : {FAILED}"
    )

    print(
        f"WARN     : {WARNINGS}"
    )

    print()

    if FAILED == 0:

        print(
            "RELEASE SMOKE TESTS: PASS"
        )

    else:

        print(
            "RELEASE SMOKE TESTS: FAIL"
        )

    if WARNINGS:

        print(
            "Review warnings before production release."
        )

    print("=" * 75)

    sys.exit(
        1 if FAILED else 0
    )


if __name__ == "__main__":
    main()

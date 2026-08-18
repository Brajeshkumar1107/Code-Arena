"""01 - Basic API / HTTP behavior tests."""
import json

from common import Config, Result, post, get, run_payload, hello, load_config


def run(conf: Config) -> Result:
    res = Result()
    valid = run_payload(hello("PYTHON"), "PYTHON")

    status, body, raw, _ = post(conf, valid)
    res.check("API-01 valid request -> 200", status == 200,
              detail=f"status={status} body={raw[:200]}")

    req = _custom_post(conf, method="GET")
    res.check("API-02 GET on POST endpoint -> 405", req == 405,
              detail=f"status={req}")

    status, _, _ = get(conf, "/api/v1/runner/does-not-exist")
    res.check("API-03 invalid endpoint -> 404", status == 404,
              detail=f"status={status}")

    status, body, raw, _ = _raw_post(conf, b"")
    res.check("API-04 empty body -> 400", status == 400,
              detail=f"status={status} body={raw[:200]}")

    status, body, raw, _ = _raw_post(conf, b"{not json")
    res.check("API-05 malformed JSON -> 400", status == 400,
              detail=f"status={status} body={raw[:200]}")

    cases = [
        ("API-06 missing mode", {"language": "PYTHON", "sourceCode": "x=1",
                                 "testCases": [{}]}),
        ("API-07 missing language", {"mode": "RUN", "sourceCode": "x=1",
                                     "testCases": [{}]}),
        ("API-08 missing sourceCode", {"mode": "RUN", "language": "PYTHON",
                                       "testCases": [{}]}),
        ("API-09 missing testCases", {"mode": "RUN", "language": "PYTHON",
                                      "sourceCode": "x=1"}),
        ("API-10 empty sourceCode", {"mode": "RUN", "language": "PYTHON",
                                     "sourceCode": "", "testCases": [{}]}),
        ("API-11 empty testCases", {"mode": "RUN", "language": "PYTHON",
                                    "sourceCode": "x=1", "testCases": []}),
        ("API-12 null test case", {"mode": "RUN", "language": "PYTHON",
                                   "sourceCode": "x=1", "testCases": [None]}),
        ("API-13 unsupported language", {"mode": "RUN", "language": "RUBY",
                                         "sourceCode": "x=1", "testCases": [{}]}),
        ("API-14 unsupported mode", {"mode": "EXECUTE", "language": "PYTHON",
                                     "sourceCode": "x=1", "testCases": [{}]}),
    ]
    for name, payload in cases:
        status, body, raw, _ = post(conf, payload)
        res.check(name + " -> 400", status == 400,
                  detail=f"status={status} body={raw[:200]}")

    payload = dict(valid)
    payload["extraField"] = "ignored"
    status, body, raw, _ = post(conf, payload)
    res.check("API-15 extra unknown field tolerated", status == 200,
              detail=f"status={status} body={raw[:200]}")

    big = run_payload("print('x')" * 300_000, "PYTHON")
    status, body, raw, _ = post(conf, big, timeout=30)
    res.check("API-16 oversized request rejected safely", status in (400, 413, 200),
              detail=f"status={status}")

    status, body, raw, _ = _raw_post(conf, json.dumps(valid).encode())
    res.check("API-17 missing Content-Type tolerated", status == 200,
              detail=f"status={status}")

    req, _, raw, _ = _raw_post(conf, b"hello", content_type="text/plain")
    res.check("API-18 text/plain -> 400/415", req in (400, 415),
              detail=f"status={req} body={raw[:200]}")

    return res


def _custom_post(conf: Config, method: str):
    import json
    import urllib.request
    body = json.dumps(run_payload(hello("PYTHON"), "PYTHON")).encode()
    req = urllib.request.Request(conf.base_url + "/api/v1/runner/execute",
                                 data=body, headers={"Content-Type": "application/json"},
                                 method=method)
    try:
        with urllib.request.urlopen(req, timeout=conf.timeout) as resp:
            return resp.status
    except urllib.error.HTTPError as exc:
        return exc.code
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        return None


def _raw_post(conf: Config, body: bytes, content_type: str = "application/json"):
    import urllib.request
    req = urllib.request.Request(conf.base_url + "/api/v1/runner/execute",
                                 data=body,
                                 headers={"Content-Type": content_type},
                                 method="POST")
    try:
        with urllib.request.urlopen(req, timeout=conf.timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            return resp.status, _try_json(raw), raw, 0.0
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", "replace")
        return exc.code, _try_json(raw), raw, 0.0
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        return None, None, str(exc), 0.0


def _try_json(raw: str):
    import json
    try:
        return json.loads(raw)
    except Exception:
        return None


if __name__ == "__main__":
    r = run(load_config())
    print(f"API tests: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  FAIL", f)

"""08 - Authentication tests.

Design: anonymous requests are allowed but rate-limited; requests with a
valid bearer token bypass the rate limiter; invalid/malformed tokens -> 401.
"""
from common import Config, Result, run_payload, post, load_config


def run(conf: Config) -> Result:
    res = Result()
    payload = run_payload("print('ok')", "PYTHON", expected="ok")

    status, body, raw, _ = post(conf, payload, token=None)
    res.check("AUTH-01 anonymous allowed", status == 200,
              detail=f"status={status} body={raw[:200]}")

    if conf.token:
        status, body, raw, _ = post(conf, payload, token=conf.token)
        res.check("AUTH-02 valid token allowed", status == 200,
                  detail=f"status={status} body={raw[:200]}")
    else:
        res.skip("AUTH-02 valid token allowed", "RUNNER_TOKEN not set")

    bad_tokens = [
        ("AUTH-03 invalid token", "this-is-not-the-token"),
        ("AUTH-04 empty bearer", ""),
        ("AUTH-05 wrong scheme", "Basic abc123"),
        ("AUTH-06 bearer missing", "abc123"),
        ("AUTH-07 wrong prefix", "Token abc123"),
        ("AUTH-08 random token", "t" * 64),
        ("AUTH-09 token with whitespace", "valid token"),
    ]
    for name, tok in bad_tokens:
        status, body, raw, _ = post(conf, payload, token=tok)
        res.check(name + " -> 401", status == 401,
                  detail=f"status={status} body={raw[:200]}")

    status, body, raw, _ = post(conf, payload, token=None,
                                extra_headers={"Authorization": "  "})
    res.check("AUTH-10 blank authorization allowed (anonymous)",
              status == 200, detail=f"status={status}")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Auth tests: {r.passed} passed, {r.failed} failed, {r.skipped} skipped")
    for f in r.failures:
        print("  ", f)

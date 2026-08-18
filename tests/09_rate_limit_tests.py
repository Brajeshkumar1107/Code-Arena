"""09 - Rate limit tests.

The limiter is a fixed window per client IP (max-requests=30 / 60s) and
runs before validation, so earlier suites' anonymous requests also consume
slots. We therefore assert on observed outcomes (>=1 x 429, >=20 x 200)
rather than exact positions. For fully deterministic numbers set
SKIP_SLOW_TESTS=0 and run this suite against a fresh instance.
"""
import time
from concurrent.futures import ThreadPoolExecutor

from common import Config, Result, run_payload, post, load_config


def run(conf: Config) -> Result:
    res = Result()
    payload = run_payload("print('ok')", "PYTHON", expected="ok")
    window = 60

    statuses = []
    for _ in range(40):
        status, body, raw, _ = post(conf, payload, token=None)
        statuses.append(status)
    ok = [s for s in statuses if s == 200]
    limited = [s for s in statuses if s == 429]
    other = [s for s in statuses if s not in (200, 429)]
    res.check("RATE-01 burst produces 200s and 429s",
              len(ok) >= 20 and len(limited) >= 1 and not other,
              detail=f"200={len(ok)} 429={len(limited)} other={other}")

    statuses = []
    for _ in range(5):
        status, body, raw, _ = post(conf, payload, token=conf.token or None)
        statuses.append(status)
    res.check("RATE-02 valid token bypasses limiter",
              statuses == [200] * 5,
              detail=f"statuses={statuses}")

    def one(_):
        status, body, raw, _ = post(conf, payload, token=None)
        return status

    with ThreadPoolExecutor(max_workers=40) as pool:
        statuses = list(pool.map(one, range(40)))
    ok = [s for s in statuses if s == 200]
    limited = [s for s in statuses if s == 429]
    other = [s for s in statuses if s not in (200, 429)]
    res.check("RATE-03 concurrent anonymous requests",
              len(ok) >= 1 and len(limited) >= 1 and not other,
              detail=f"200={len(ok)} 429={len(limited)} other={other}")

    if conf.skip_slow:
        res.skip("RATE-04 window recovery", "set SKIP_SLOW_TESTS=0")
    else:
        time.sleep(window + 5)
        status, body, raw, _ = post(conf, payload, token=None)
        res.check("RATE-04 window recovery -> 200", status == 200,
                  detail=f"status={status}")

    res.skip("RATE-05 different client IP",
             "needs LB / X-Forwarded-For support (manual)")
    res.skip("RATE-06 multiple clients",
             "needs multiple source IPs (manual)")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Rate tests: {r.passed} passed, {r.failed} failed, {r.skipped} skipped")
    for f in r.failures:
        print("  ", f)

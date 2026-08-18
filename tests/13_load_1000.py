"""13 - Load test: 1000 concurrent requests (capacity/optimization gate).

Historical baseline: ~778/1000 accepted, ~222 client timeouts.
"""
from common import Config, Result, load_run, load_config


def run(conf: Config) -> Result:
    return load_run(conf, 1000, "1000 concurrent")


if __name__ == "__main__":
    r = run(load_config())
    print(r.metrics)
    print(f"Load-1000: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  ", f)

"""12 - Load test: 500 concurrent requests (scalability gate)."""
from common import Config, Result, load_run, load_config


def run(conf: Config) -> Result:
    return load_run(conf, 500, "500 concurrent")


if __name__ == "__main__":
    r = run(load_config())
    print(r.metrics)
    print(f"Load-500: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  ", f)

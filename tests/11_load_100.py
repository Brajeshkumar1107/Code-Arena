"""11 - Load test: 100 concurrent requests (scalability gate)."""
from common import Config, Result, load_run, load_config


def run(conf: Config) -> Result:
    return load_run(conf, 100, "100 concurrent")


if __name__ == "__main__":
    r = run(load_config())
    print(r.metrics)
    print(f"Load-100: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  ", f)

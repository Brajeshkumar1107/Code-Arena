"""07 - Docker isolation and security tests (release blockers).

Each abuse attempt must be contained: the runner must return a finite
verdict and must never give the code a way to reach the host.
"""
from common import Config, Result, run_payload, post, load_config

CONTAINED = ("RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED",
             "MEMORY_LIMIT_EXCEEDED", "OUTPUT_LIMIT_EXCEEDED")


def _sec(conf, res, name, src, must_contain=True):
    status, body, raw, _ = post(conf, run_payload(src, "PYTHON"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    if must_contain:
        ok = status == 200 and verdict in CONTAINED
    else:
        ok = status == 200 and verdict == "ACCEPTED"
    res.check(name, ok,
              detail=f"status={status} verdict={verdict} body={raw[:200]}")


def run(conf: Config) -> Result:
    res = Result()

    # Filesystem
    _sec(conf, res, "SEC-01 write /etc",
         "open('/etc/evil.txt', 'w').write('x')")
    _sec(conf, res, "SEC-02 write /root",
         "open('/root/evil.txt', 'w').write('x')")
    _sec(conf, res, "SEC-03 write outside workspace",
         "open('/opt/evil.txt', 'w').write('x')")
    _sec(conf, res, "SEC-04 write inside workspace",
         "open('in.txt', 'w').write('x')\nprint(open('in.txt').read())",
         must_contain=False)
    _sec(conf, res, "SEC-05 read protected filesystem",
         "print(open('/etc/shadow').read())")
    _sec(conf, res, "SEC-06 delete protected filesystem",
         "import os\nos.remove('/etc/hostname')")

    # Network
    _sec(conf, res, "SEC-07 internet access",
         "import urllib.request\n"
         "print(urllib.request.urlopen('http://example.com', timeout=3).status)")
    _sec(conf, res, "SEC-08 http request",
         "import urllib.request\n"
         "print(urllib.request.urlopen('http://93.184.216.34', timeout=3).status)")
    _sec(conf, res, "SEC-09 dns lookup",
         "import socket\nprint(socket.gethostbyname('example.com'))")
    _sec(conf, res, "SEC-10 localhost access",
         "import socket\n"
         "s = socket.create_connection(('127.0.0.1', 8081), 3)\nprint('ok')")
    _sec(conf, res, "SEC-11 host gateway access",
         "import socket\n"
         "s = socket.create_connection(('172.17.0.1', 80), 3)\nprint('ok')")
    _sec(conf, res, "SEC-12 connect to MySQL",
         "import socket\n"
         "s = socket.create_connection(('127.0.0.1', 3306), 3)\nprint('ok')")
    _sec(conf, res, "SEC-13 connect to Docker",
         "import socket\n"
         "s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)\n"
         "s.connect('/var/run/docker.sock')\nprint('ok')")

    # Privileges
    _sec(conf, res, "SEC-14 docker socket access",
         "import os\nprint(os.path.exists('/var/run/docker.sock'))")
    _sec(conf, res, "SEC-15 create docker container",
         "import subprocess\nsubprocess.run(['docker', 'ps'], check=True)")
    _sec(conf, res, "SEC-16 mount host filesystem",
         "import subprocess\nsubprocess.run(['mount', '-t', 'proc', 'proc', '/mnt'], check=True)")
    _sec(conf, res, "SEC-17 privileged operations (mknod)",
         "import os\nos.mknod('/dev/evil')")
    _sec(conf, res, "SEC-18 load kernel modules",
         "import subprocess\nsubprocess.run(['insmod', '/tmp/x.ko'], check=True)")
    _sec(conf, res, "SEC-19 access host devices",
         "print(open('/dev/sda').read(10))")
    _sec(conf, res, "SEC-20 change container capabilities",
         "import subprocess\nsubprocess.run(['setcap'], check=True)")

    # Proc/sys inspection stays inside the namespace
    _sec(conf, res, "SEC-21 /proc inspection",
         "import os\nprint(sorted(os.listdir('/proc'))[:5])",
         must_contain=False)
    _sec(conf, res, "SEC-22 /sys inspection",
         "import os\nprint(sorted(os.listdir('/sys/class'))[:3])",
         must_contain=False)

    # Process isolation
    _sec(conf, res, "SEC-23 spawn many threads",
         "import threading\n"
         "ts = [threading.Thread(target=lambda: None) for _ in range(500)]\n"
         "[t.start() for t in ts]\nprint('ok')")

    # Verify workspace is shared and cleaned between executions
    status, body, raw, _ = post(conf, run_payload(
        "print('marker-secret')", "PYTHON", expected="marker-secret"))
    res.check("SEC-24 normal execution after abuse",
              status == 200 and isinstance(body, dict) and body.get("verdict") == "ACCEPTED",
              detail=f"status={status} body={raw[:200]}")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Security tests: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  FAIL", f)

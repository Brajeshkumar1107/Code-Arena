"""04 - C++ execution regression tests."""
from common import Config, Result, run_payload, check_ok, post, load_config

ACCEPTED = "ACCEPTED"
CONTAINED = ("RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED",
             "MEMORY_LIMIT_EXCEEDED", "OUTPUT_LIMIT_EXCEEDED")


def run(conf: Config) -> Result:
    res = Result()

    cases = [
        ("CPP-01 hello world",
         "#include <iostream>\nint main() { std::cout << \"hello world\" "
         "<< std::endl; return 0; }"),
        ("CPP-02 arithmetic",
         "#include <iostream>\nint main() { std::cout << (2 + 3 * 4) "
         "<< std::endl; return 0; }"),
        ("CPP-03 cin input",
         "#include <iostream>\n#include <string>\nint main() { "
         "std::string s; std::getline(std::cin, s); std::cout << s; return 0; }"),
        ("CPP-04 multiple inputs",
         "#include <iostream>\nint main() { int a, b; std::cin >> a >> b; "
         "std::cout << (a + b); return 0; }"),
        ("CPP-05 multiline input",
         "#include <iostream>\n#include <string>\nint main() { "
         "std::string l; int n = 0; while (std::getline(std::cin, l)) n++; "
         "std::cout << n; return 0; }"),
        ("CPP-06 unicode",
         "#include <iostream>\nint main() { std::cout << \"café ☕\"; return 0; }"),
        ("CPP-07 stl",
         "#include <iostream>\n#include <algorithm>\n#include <vector>\n"
         "int main() { std::vector<int> v{3,1,2}; std::sort(v.begin(), v.end()); "
         "std::cout << v[0] << v[1] << v[2]; return 0; }"),
        ("CPP-08 vector",
         "#include <iostream>\n#include <vector>\nint main() { "
         "std::vector<int> v(10, 7); std::cout << v.size() << v[3]; return 0; }"),
        ("CPP-09 map",
         "#include <iostream>\n#include <map>\nint main() { "
         "std::map<std::string,int> m{{ \"a\", 1 }, { \"b\", 2 }}; "
         "std::cout << m[\"b\"]; return 0; }"),
        ("CPP-10 large calculation",
         "#include <iostream>\nint main() { long long s = 0; "
         "for (int i = 0; i < 1000000; i++) s += i; std::cout << s; return 0; }"),
    ]
    for name, src in cases:
        inp = "3 4" if "multiple inputs" in name else ""
        check_ok(conf, res, name, run_payload(src, "CPP", inp), ACCEPTED)

    check_ok(conf, res, "CPP-11 compile error",
             run_payload("int main() { undefined_function(); }", "CPP"),
             "COMPILATION_ERROR")
    check_ok(conf, res, "CPP-12 missing semicolon",
             run_payload("int main() { std::cout << 1 return 0; }", "CPP"),
             "COMPILATION_ERROR")
    check_ok(conf, res, "CPP-13 runtime error",
             run_payload("#include <iostream>\nint main() { "
                         "throw 42; return 0; }", "CPP"), "RUNTIME_ERROR")
    check_ok(conf, res, "CPP-14 segmentation fault",
             run_payload("int main() { int* p = nullptr; *p = 1; return 0; }",
                         "CPP"), "RUNTIME_ERROR")
    check_ok(conf, res, "CPP-15 infinite loop",
             run_payload("int main() { while (true) {} return 0; }", "CPP"),
             "TIME_LIMIT_EXCEEDED")
    check_ok(conf, res, "CPP-16 memory exhaustion",
             run_payload("#include <vector>\nint main() { "
                         "std::vector<int> v; while (true) v.push_back(1); "
                         "return 0; }", "CPP"), "MEMORY_LIMIT_EXCEEDED")

    status, body, raw, _ = post(conf, run_payload(
        "#include <cstdlib>\nint main() { while (true) system(\"true\"); }", "CPP"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("CPP-17 process spawn contained",
              status == 200 and verdict in CONTAINED,
              detail=f"status={status} verdict={verdict}")

    status, body, raw, _ = post(conf, run_payload(
        "#include <fstream>\nint main() { "
        "std::ofstream f(\"/etc/evil.txt\"); f << \"x\"; return 0; }", "CPP"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("CPP-18 read-only rootfs blocks write",
              status == 200 and verdict in CONTAINED,
              detail=f"status={status} verdict={verdict}")

    status, body, raw, _ = post(conf, run_payload(
        "int main() { "
        "for (int i = 0; i < 100; i++) { "
        "for (int j = 0; j < 100; j++) { } } return 0; }", "CPP"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("CPP-19 normal exec after failures",
              status == 200 and verdict == ACCEPTED,
              detail=f"status={status} verdict={verdict}")

    status, body, raw, _ = post(conf, {
        "mode": "JUDGE", "language": "CPP",
        "sourceCode": "#include <iostream>\nint main() { int x; "
                      "std::cin >> x; std::cout << (x * 2); return 0; }",
        "testCases": [{"input": "1", "expectedOutput": "2"},
                      {"input": "5", "expectedOutput": "10"},
                      {"input": "0", "expectedOutput": "0"}]})
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    passed = (body or {}).get("passed") if isinstance(body, dict) else None
    total = (body or {}).get("total") if isinstance(body, dict) else None
    res.check("CPP-20 multiple test cases",
              status == 200 and verdict == ACCEPTED and passed == 3 and total == 3,
              detail=f"status={status} verdict={verdict} passed={passed} total={total}")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"C++ tests: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  FAIL", f)

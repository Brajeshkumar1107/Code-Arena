"""03 - Java execution regression tests."""
from common import Config, Result, run_payload, check_ok, post, load_config

ACCEPTED = "ACCEPTED"
CONTAINED = ("RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED",
             "MEMORY_LIMIT_EXCEEDED", "OUTPUT_LIMIT_EXCEEDED")


def run(conf: Config) -> Result:
    res = Result()

    cases = [
        ("JAVA-01 hello world",
         "public class Main { public static void main(String[] a) { "
         "System.out.println(\"hello world\"); } }"),
        ("JAVA-02 arithmetic",
         "public class Main { public static void main(String[] a) { "
         "System.out.println(2 + 3 * 4); } }"),
        ("JAVA-03 scanner input",
         "import java.util.Scanner;\npublic class Main { public static void main(String[] a) { "
         "System.out.println(new Scanner(System.in).nextLine()); } }"),
        ("JAVA-04 buffered reader",
         "import java.io.*;\npublic class Main { public static void main(String[] a) "
         "throws Exception { System.out.println(new BufferedReader(new InputStreamReader(System.in)).readLine()); } }"),
        ("JAVA-05 multiple inputs",
         "import java.util.Scanner;\npublic class Main { public static void main(String[] a) { "
         "Scanner s = new Scanner(System.in); System.out.println(s.nextInt() + s.nextInt()); } }"),
        ("JAVA-06 multiline output",
         "public class Main { public static void main(String[] a) { "
         "for (int i = 1; i <= 3; i++) System.out.println(i); } }"),
        ("JAVA-07 unicode",
         "public class Main { public static void main(String[] a) { "
         "System.out.println(\"café ☕\"); } }"),
        ("JAVA-08 collections",
         "import java.util.*;\npublic class Main { public static void main(String[] a) { "
         "List<Integer> l = new ArrayList<>(); l.add(3); l.add(1); l.add(2); "
         "Collections.sort(l); System.out.println(l); } }"),
        ("JAVA-09 streams",
         "import java.util.*;\npublic class Main { public static void main(String[] a) { "
         "System.out.println(Arrays.stream(new int[]{1,2,3}).sum()); } }"),
        ("JAVA-10 large calculation",
         "public class Main { public static void main(String[] a) { "
         "long s = 0; for (int i = 0; i < 1_000_000; i++) s += i; "
         "System.out.println(s); } }"),
    ]
    for name, src in cases:
        inp = "3\n4" if "multiple inputs" in name else ""
        check_ok(conf, res, name, run_payload(src, "JAVA", inp), ACCEPTED)

    check_ok(conf, res, "JAVA-11 compile error",
             run_payload("public class Main { void main() { } }", "JAVA"),
             "COMPILATION_ERROR")
    check_ok(conf, res, "JAVA-12 syntax error",
             run_payload("public class Main { public static void main(", "JAVA"),
             "COMPILATION_ERROR")
    check_ok(conf, res, "JAVA-13 missing class",
             run_payload("class NotMain { }", "JAVA"), "COMPILATION_ERROR")
    check_ok(conf, res, "JAVA-14 runtime exception",
             run_payload("public class Main { public static void main(String[] a) "
                         "{ int[] x = {}; System.out.println(x[5]); } }", "JAVA"),
             "RUNTIME_ERROR")
    check_ok(conf, res, "JAVA-15 infinite loop",
             run_payload("public class Main { public static void main(String[] a) "
                         "{ while (true) {} } }", "JAVA"), "TIME_LIMIT_EXCEEDED")
    check_ok(conf, res, "JAVA-16 memory exhaustion",
             run_payload("public class Main { public static void main(String[] a) "
                         "{ int[][] m = new int[5000][5000]; } }", "JAVA"),
             "MEMORY_LIMIT_EXCEEDED")

    status, body, raw, _ = post(conf, run_payload(
        "public class Main { public static void main(String[] a) throws Exception { "
        "while (true) { Runtime.getRuntime().exec(\"true\"); } } }", "JAVA"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("JAVA-17 process spawn contained",
              status == 200 and verdict in CONTAINED,
              detail=f"status={status} verdict={verdict} body={raw[:200]}")

    status, body, raw, _ = post(conf, run_payload(
        "public class Main { public static void main(String[] a) throws Exception { "
        "java.nio.file.Files.writeString(java.nio.file.Path.of(\"/etc/evil.txt\"), \"x\"); } }",
        "JAVA"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("JAVA-18 read-only rootfs blocks write",
              status == 200 and verdict in CONTAINED,
              detail=f"status={status} verdict={verdict}")

    status, body, raw, _ = post(conf, run_payload(
        "public class Main { public static void main(String[] a) { "
        "System.out.println(System.getenv()); } }", "JAVA"))
    stdout = (body or {}).get("stdout", "") if isinstance(body, dict) else ""
    leaked = [k for k in ("RUNNER_TOKEN", "DB_PASSWORD") if k in stdout]
    res.check("JAVA-19 env does not leak secrets",
              not leaked, detail=f"leaked={leaked}")

    status, body, raw, _ = post(conf, run_payload(
        "public class Main { public static void main(String[] a) throws Exception { "
        "java.nio.file.Files.createTempFile(\"t\", \".txt\"); "
        "System.out.println(\"ok\"); } }", "JAVA"))
    verdict = (body or {}).get("verdict") if isinstance(body, dict) else None
    res.check("JAVA-20 tmpfs scratch space works",
              status == 200 and verdict == ACCEPTED,
              detail=f"status={status} verdict={verdict}")

    return res


if __name__ == "__main__":
    r = run(load_config())
    print(f"Java tests: {r.passed} passed, {r.failed} failed")
    for f in r.failures:
        print("  FAIL", f)

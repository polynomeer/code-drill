import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * 공개 테스트를 로컬에서 돌린다 — 채점기와 같은 하네스다. 프로젝트 폴더(이 파일이 든 .codedrill 의 부모)에서:
 *
 *   java .codedrill/Run.java
 *
 * 채점기처럼 src·tests 의 .java 전부와 하네스를 한 번에 컴파일하고, 하네스로 테스트를 돌려 리포트를 읽는다.
 * 숨은 테스트는 키트에 없다 — 채점은 서버가 한다. JDK 17 이상이면 된다.
 */
public class Run {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path kit = root.resolve(".codedrill");
        if (!Files.isRegularFile(kit.resolve("project.json"))) {
            System.out.println("프로젝트 폴더에서 실행하세요: java .codedrill/Run.java");
            System.exit(2);
        }
        Map<String, Object> meta = Json.object(Files.readString(kit.resolve("project.json")));
        @SuppressWarnings("unchecked") Map<String, Object> limits = (Map<String, Object>) meta.get("limits");
        @SuppressWarnings("unchecked") List<Object> publicTests = (List<Object>) meta.get("publicTests");
        System.out.println(meta.get("title") + " · v" + ((Number) meta.get("version")).intValue() + " · Java");

        Path classes = kit.resolve("out").resolve("classes");
        deleteTree(kit.resolve("out"));
        Files.createDirectories(classes);

        List<String> sources = new ArrayList<>();
        for (String dir : List.of("src", "tests", ".codedrill/harness")) sources.addAll(javaFiles(root.resolve(dir)));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            System.out.println("javac 가 없습니다 — JRE 가 아니라 JDK 로 실행하세요");
            System.exit(2);
        }
        List<String> options = new ArrayList<>(List.of("-d", classes.toString(), "-encoding", "UTF-8"));
        options.addAll(sources);
        if (javac.run(null, null, null, options.toArray(String[]::new)) != 0) {
            System.out.println("빌드 ✕ 컴파일 오류");
            System.exit(1);
        }
        System.out.println("빌드 ✓");

        Path tmp = Files.createTempDirectory("codedrill");
        Path nonce = tmp.resolve("nonce");
        Path report = tmp.resolve("report.json");
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        Files.writeString(nonce, HexFormat.of().formatHex(bytes));
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(
            java, "-Xmx" + ((Number) limits.get("memoryMb")).intValue() + "m", "-Dfile.encoding=UTF-8",
            "-cp", classes.toString(), "codedrill.CodedrillHarness",
            classes.toString(), report.toString(), nonce.toString()
        ).directory(root.toFile()).inheritIO().start();
        process.waitFor();
        if (!Files.isRegularFile(report)) {
            System.out.println("테스트를 돌리지 못했습니다 — 하네스가 리포트를 남기지 않았습니다");
            System.exit(1);
        }
        System.exit(print(Json.object(Files.readString(report)), publicTests));
    }

    @SuppressWarnings("unchecked")
    static int print(Map<String, Object> report, List<Object> publicTests) {
        if (report.get("tampered") != null) {
            System.out.println("테스트 기반이 바뀌었습니다 — 채점에서는 전부 실패입니다: " + report.get("tampered"));
            return 1;
        }
        List<Object> tests = (List<Object>) report.get("tests");
        int passed = 0;
        System.out.println("공개 테스트");
        for (Object item : tests) {
            Map<String, Object> test = (Map<String, Object>) item;
            boolean ok = Boolean.TRUE.equals(test.get("passed"));
            if (ok) passed++;
            String mine = publicTests.contains(test.get("module")) ? "" : "  (내가 쓴 테스트)";
            System.out.println("  " + (ok ? "✓" : "✕") + " " + test.get("module") + "." + test.get("name") + mine);
            if (!ok && test.get("message") != null) System.out.println("      " + test.get("message"));
        }
        System.out.println(passed + " / " + tests.size() + " 통과 — 숨은 테스트는 제출하면 서버에서 돕니다");
        return !tests.isEmpty() && passed == tests.size() ? 0 : 1;
    }

    static List<String> javaFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> p.toString().endsWith(".java")).map(Path::toString).sorted().toList();
        }
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted((a, b) -> b.compareTo(a)).toList()) Files.delete(p);
        }
    }

    /** 리포트와 project.json 만 읽는 작은 JSON 파서. 의존성을 두지 않으려고 직접 쓴다. */
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        @SuppressWarnings("unchecked")
        static Map<String, Object> object(String text) { return (Map<String, Object>) new Json(text).value(); }

        private Object value() {
            skip();
            char c = s.charAt(i);
            if (c == '{') {
                Map<String, Object> map = new LinkedHashMap<>();
                i++;
                skip();
                if (s.charAt(i) == '}') { i++; return map; }
                while (true) {
                    skip();
                    String key = string();
                    skip();
                    i++; // :
                    map.put(key, value());
                    skip();
                    if (s.charAt(i++) == '}') return map;
                }
            }
            if (c == '[') {
                List<Object> list = new ArrayList<>();
                i++;
                skip();
                if (s.charAt(i) == ']') { i++; return list; }
                while (true) {
                    list.add(value());
                    skip();
                    if (s.charAt(i++) == ']') return list;
                }
            }
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return true; }
            if (s.startsWith("false", i)) { i += 5; return false; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(start, i));
        }

        private String string() {
            StringBuilder out = new StringBuilder();
            i++; // "
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'r' -> out.append('\r');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> { out.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> out.append(e);
                }
            }
        }

        private void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}

package codedrill;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 프로젝트형 문제의 Java 테스트 하네스 (ProjectEngine 이 워크스페이스와 함께 컴파일한다).
 *
 *   java -cp <classes> codedrill.CodedrillHarness <클래스 디렉터리> <리포트 파일> <nonce 파일>
 *
 * Kotlin 하네스와 같은 약속이다: 이름이 `Test` 로 끝나는 클래스의, 인자 없는 공개 메서드 중
 * 이름이 `test` 로 시작하는 것이 테스트이고, 예외가 나면 실패다. `module` 은 클래스의 완전한
 * 이름이라 숨은 테스트 파일 `tests/HiddenAccountTest.java` 와 맞아떨어진다. 리포트의 nonce 는
 * 사용자 클래스를 들이기 전에 읽고 지운 파일에서 온다. 단언은 [Assertions] 에 있다.
 */
public final class CodedrillHarness {

    private record Row(String module, String name, boolean passed, String message) {}

    private static String json(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t") + "\"";
    }

    public static void main(String[] args) throws Exception {
        File classes = new File(args[0]);
        File report = new File(args[1]);
        File nonceFile = new File(args[2]);
        String nonce = Files.readString(nonceFile.toPath()).trim();
        nonceFile.delete();

        List<Row> rows = new ArrayList<>();
        String tampered = null;

        boolean canaryFailed = false;
        try {
            Assertions.assertEquals(1, 2);
        } catch (AssertionError e) {
            canaryFailed = true;
        }
        if (!canaryFailed) tampered = "실패해야 하는 카나리 단언이 통과했다";

        List<String> names = new ArrayList<>();
        try (var walk = Files.walk(classes.toPath())) {
            walk.filter(p -> p.toString().endsWith("Test.class"))
                .forEach(p -> names.add(classes.toPath().relativize(p).toString().replace(".class", "").replace(File.separatorChar, '.')));
        }
        names.sort(null);

        for (String className : names) {
            Class<?> type;
            try {
                type = Class.forName(className, true, ClassLoader.getSystemClassLoader());
            } catch (Throwable e) {
                rows.add(new Row(className, "<load>", false, cut(e.getClass().getSimpleName() + ": " + e.getMessage())));
                continue;
            }
            Method[] methods = type.getMethods();
            Arrays.sort(methods, (a, b) -> a.getName().compareTo(b.getName()));
            for (Method method : methods) {
                if (!method.getName().startsWith("test") || method.getParameterCount() != 0) continue;
                Object instance;
                try {
                    instance = type.getDeclaredConstructor().newInstance();
                } catch (Throwable e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    rows.add(new Row(className, method.getName(), false, cut("인스턴스를 만들지 못했다: " + cause.getMessage())));
                    continue;
                }
                try {
                    method.invoke(instance);
                    rows.add(new Row(className, method.getName(), true, null));
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    rows.add(new Row(className, method.getName(), false, cut(cause.getClass().getSimpleName() + ": " + cause.getMessage())));
                } catch (Throwable e) {
                    rows.add(new Row(className, method.getName(), false, cut(e.getClass().getSimpleName() + ": " + e.getMessage())));
                }
            }
        }

        StringBuilder body = new StringBuilder();
        body.append("{\"nonce\":").append(json(nonce));
        body.append(",\"tampered\":").append(json(tampered));
        body.append(",\"loadError\":null,\"tests\":[");
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (i > 0) body.append(',');
            body.append("{\"module\":").append(json(row.module()));
            body.append(",\"name\":").append(json(row.name()));
            body.append(",\"passed\":").append(row.passed());
            body.append(",\"message\":").append(json(row.message())).append('}');
        }
        body.append("]}");
        Files.writeString(report.toPath(), body.toString());
    }

    private static String cut(String text) {
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}

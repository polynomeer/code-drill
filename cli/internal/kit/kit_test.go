package kit

import (
	"archive/zip"
	"bytes"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestSkip(t *testing.T) {
	skipped := []string{".codedrill/harness/Harness.kt", ".codedrill/project.json", "CODEDRILL.md", "build.gradle.kts",
		".idea/workspace.xml", "build/classes/A.class", "src/__pycache__/a.pyc", "src/A.class", ".gitignore"}
	for _, p := range skipped {
		if !Skip(p) {
			t.Errorf("%s 는 빼야 한다", p)
		}
	}
	kept := []string{"src/queue/JobQueue.kt", "tests/PublicJobQueueTest.kt", "src/build.py", "docs/CODEDRILL.md"}
	for _, p := range kept {
		if Skip(p) {
			t.Errorf("%s 는 올려야 한다", p)
		}
	}
}

func write(t *testing.T, root, rel, content string) {
	t.Helper()
	p := filepath.Join(root, filepath.FromSlash(rel))
	if err := os.MkdirAll(filepath.Dir(p), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(p, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

func TestCollectAndFind(t *testing.T) {
	root := t.TempDir()
	write(t, root, ".codedrill/project.json", `{"kit":1,"id":"job-queue","version":3,"language":"KOTLIN","limits":{"maxFiles":2,"maxTotalBytes":100}}`)
	write(t, root, ".codedrill/harness/Harness.kt", "object Harness")
	write(t, root, "build.gradle.kts", "plugins {}")
	write(t, root, "build/out.txt", "x")
	write(t, root, "src/JobQueue.kt", "class JobQueue")
	write(t, root, "tests/MyTest.kt", "class MyTest")

	found, info, err := Find(filepath.Join(root, "src"))
	if err != nil || found != root || info.ID != "job-queue" || info.Version != 3 {
		t.Fatalf("하위 폴더에서도 키트를 찾아야 한다: %v %v %v", found, info, err)
	}
	files, err := Collect(root, info)
	if err != nil {
		t.Fatal(err)
	}
	if len(files) != 2 || files["src/JobQueue.kt"] != "class JobQueue" || files["tests/MyTest.kt"] == "" {
		t.Fatalf("소스와 테스트만 모아야 한다: %v", files)
	}

	write(t, root, "src/Extra.kt", "class Extra")
	if _, err := Collect(root, info); err == nil || !strings.Contains(err.Error(), "한도 2 개") {
		t.Fatalf("파일 수 한도를 넘으면 보내기 전에 멈춰야 한다: %v", err)
	}
}

func TestCollectRejectsBinary(t *testing.T) {
	root := t.TempDir()
	write(t, root, "src/a.bin", "a\x00b")
	if _, err := Collect(root, &Info{}); err == nil {
		t.Fatal("바이너리는 거절해야 한다")
	}
}

func zipOf(t *testing.T, files map[string]string) []byte {
	t.Helper()
	var buf bytes.Buffer
	w := zip.NewWriter(&buf)
	for name, content := range files {
		f, err := w.Create(name)
		if err != nil {
			t.Fatal(err)
		}
		f.Write([]byte(content))
	}
	w.Close()
	return buf.Bytes()
}

func TestExtract(t *testing.T) {
	dest := t.TempDir()
	written, err := Extract(zipOf(t, map[string]string{"job-queue/src/A.kt": "a", "job-queue/.codedrill/project.json": "{}"}), dest)
	if err != nil || len(written) != 2 {
		t.Fatalf("%v %v", written, err)
	}
	if data, _ := os.ReadFile(filepath.Join(dest, "job-queue", "src", "A.kt")); string(data) != "a" {
		t.Fatal("내용이 그대로 풀려야 한다")
	}
	if _, err := Extract(zipOf(t, map[string]string{"job-queue/src/A.kt": "b"}), dest); err == nil {
		t.Fatal("풀던 코드를 덮으면 안 된다")
	}
	if data, _ := os.ReadFile(filepath.Join(dest, "job-queue", "src", "A.kt")); string(data) != "a" {
		t.Fatal("거절했으면 그대로여야 한다")
	}
}

func TestExtractRejectsEscape(t *testing.T) {
	for _, name := range []string{"../evil", "/etc/evil", "a/../../evil", "C:/evil"} {
		if _, err := Extract(zipOf(t, map[string]string{name: "x"}), t.TempDir()); err == nil {
			t.Errorf("%s 는 거절해야 한다", name)
		}
	}
}

// Package kit 은 받은 키트를 풀고, 제출할 파일을 고른다.
//
// 고르는 규칙은 서버의 `ProjectKit.KIT_PATHS` 와 웹의 `kitFiles.ts` 와 같다 — 키트 파일(특히
// `.codedrill/harness/`)을 올리면 Java·Kotlin 은 채점기의 하네스와 같은 클래스가 두 번 생겨 컴파일이 깨진다.
package kit

import (
	"archive/zip"
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path"
	"path/filepath"
	"strings"
	"unicode/utf8"
)

// Info 는 키트의 `.codedrill/project.json`.
type Info struct {
	Kit           int    `json:"kit"`
	ID            string `json:"id"`
	Version       int    `json:"version"`
	Title         string `json:"title"`
	Language      string `json:"language"`
	PackageDigest string `json:"packageDigest"`
	Limits        struct {
		BuildSeconds  int `json:"buildSeconds"`
		TestSeconds   int `json:"testSeconds"`
		MemoryMb      int `json:"memoryMb"`
		MaxFiles      int `json:"maxFiles"`
		MaxTotalBytes int `json:"maxTotalBytes"`
	} `json:"limits"`
	PublicTests []string `json:"publicTests"`
}

// Find 는 dir 에서 위로 올라가며 `.codedrill/project.json` 을 찾는다 — 하위 폴더에서 불러도 된다.
func Find(dir string) (root string, info *Info, err error) {
	dir, err = filepath.Abs(dir)
	if err != nil {
		return "", nil, err
	}
	for {
		data, err := os.ReadFile(filepath.Join(dir, ".codedrill", "project.json"))
		if err == nil {
			var info Info
			if err := json.Unmarshal(data, &info); err != nil {
				return "", nil, fmt.Errorf(".codedrill/project.json 을 읽지 못했습니다: %w", err)
			}
			return dir, &info, nil
		}
		parent := filepath.Dir(dir)
		if parent == dir {
			return "", nil, errors.New("키트 폴더가 아닙니다 — codedrill get <문제> 로 받은 폴더 안에서 하세요")
		}
		dir = parent
	}
}

var kitFiles = map[string]bool{"CODEDRILL.md": true, "build.gradle.kts": true, "settings.gradle.kts": true}
var buildDirs = map[string]bool{"build": true, "out": true, "__pycache__": true, "node_modules": true}

// Skip 은 올리지 않을 경로인가 — 키트 파일, `.` 으로 시작하는 것(.codedrill·.idea·.gradle·.git), 빌드 산출물.
func Skip(rel string) bool {
	parts := strings.Split(rel, "/")
	for _, part := range parts {
		if strings.HasPrefix(part, ".") {
			return true
		}
	}
	if len(parts) == 1 && kitFiles[rel] {
		return true
	}
	for _, dir := range parts[:len(parts)-1] {
		if buildDirs[dir] {
			return true
		}
	}
	return strings.HasSuffix(rel, ".class")
}

// Collect 는 제출할 파일을 모은다. 한도(파일 수·바이트)를 넘으면 서버에 보내기 전에 멈춘다.
func Collect(root string, info *Info) (map[string]string, error) {
	files := map[string]string{}
	total := 0
	err := filepath.WalkDir(root, func(p string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		rel, _ := filepath.Rel(root, p)
		rel = filepath.ToSlash(rel)
		if rel == "." {
			return nil
		}
		if d.IsDir() {
			if Skip(rel + "/x") {
				return filepath.SkipDir
			}
			return nil
		}
		if !d.Type().IsRegular() || Skip(rel) {
			return nil
		}
		data, err := os.ReadFile(p)
		if err != nil {
			return err
		}
		if !utf8.Valid(data) || bytes.IndexByte(data, 0) >= 0 {
			return fmt.Errorf("%s 는 텍스트 파일이 아닙니다 — 제출에는 소스와 테스트만 넣습니다", rel)
		}
		files[rel] = string(data)
		total += len(data)
		return nil
	})
	if err != nil {
		return nil, err
	}
	if len(files) == 0 {
		return nil, errors.New("올릴 파일이 없습니다")
	}
	if max := info.Limits.MaxFiles; max > 0 && len(files) > max {
		return nil, fmt.Errorf("파일이 %d 개라 한도 %d 개를 넘습니다", len(files), max)
	}
	if max := info.Limits.MaxTotalBytes; max > 0 && total > max {
		return nil, fmt.Errorf("파일이 모두 %d 바이트라 한도 %d 바이트를 넘습니다", total, max)
	}
	return files, nil
}

// Extract 는 키트 ZIP 을 dest 아래에 푼다. ZIP 안의 경로는 `<id>/...` 다. 이미 있는 파일은 덮지 않는다
// — 풀던 코드를 잃지 않게. 바깥으로 나가는 경로(zip slip)는 거절한다.
func Extract(data []byte, dest string) (written []string, err error) {
	reader, err := zip.NewReader(bytes.NewReader(data), int64(len(data)))
	if err != nil {
		return nil, fmt.Errorf("키트를 열지 못했습니다: %w", err)
	}
	for _, file := range reader.File {
		name := path.Clean(file.Name)
		if path.IsAbs(name) || name == ".." || strings.HasPrefix(name, "../") || strings.Contains(name, ":") {
			return nil, fmt.Errorf("키트에 이상한 경로가 있습니다: %s", file.Name)
		}
		if file.FileInfo().IsDir() {
			continue
		}
		target := filepath.Join(dest, filepath.FromSlash(name))
		if _, err := os.Stat(target); err == nil {
			return nil, fmt.Errorf("%s 가 이미 있습니다 — 다른 폴더에서 받거나 지우고 다시 하세요", target)
		}
		if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
			return nil, err
		}
		in, err := file.Open()
		if err != nil {
			return nil, err
		}
		out, err := os.OpenFile(target, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0o644)
		if err != nil {
			in.Close()
			return nil, err
		}
		_, err = io.Copy(out, in)
		in.Close()
		if cerr := out.Close(); err == nil {
			err = cerr
		}
		if err != nil {
			return nil, err
		}
		written = append(written, name)
	}
	return written, nil
}

// RunCommand 는 키트의 로컬 실행 명령 — 채점기와 같은 하네스로 공개 테스트를 돌린다.
func RunCommand(language string) ([]string, error) {
	switch language {
	case "PYTHON":
		return []string{"python3", ".codedrill/run.py"}, nil
	case "JAVA":
		return []string{"java", ".codedrill/Run.java"}, nil
	case "KOTLIN":
		return []string{"gradle", "-q", "codedrillTest"}, nil
	}
	return nil, fmt.Errorf("이 CLI 가 모르는 언어입니다: %s — codedrill 을 새 판으로 올리세요", language)
}

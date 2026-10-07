// codedrill — 프로젝트형 문제를 내 IDE 에서 푸는 CLI (feature-roadmap 11단계 이어서, 2단계).
//
//	codedrill login            브라우저에서 승인해 이 기기를 연결한다 (RFC 8628)
//	codedrill get <문제>       시작 저장소·공개 테스트·하네스(키트)를 받는다
//	codedrill test             채점기와 같은 하네스로 공개 테스트를 돌린다
//	codedrill submit           제출하고 판정을 기다린다
//
// 판정은 서버가 한다. 숨은 테스트는 키트에 없고, 이 CLI 도 볼 수 없다.
package main

import (
	"errors"
	"flag"
	"fmt"
	"io"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"github.com/polynomeer/code-drill/cli/internal/api"
	"github.com/polynomeer/code-drill/cli/internal/auth"
	"github.com/polynomeer/code-drill/cli/internal/kit"
)

// version 은 릴리스 빌드가 -ldflags "-X main.version=..." 로 넣는다.
var version = "dev"

const usage = `codedrill — 프로젝트형 문제를 내 IDE 에서 푼다

사용법:
  codedrill login [--server URL] [--no-browser]
  codedrill logout
  codedrill whoami
  codedrill get <문제 id> [폴더]
  codedrill test
  codedrill submit [--no-wait]
  codedrill version

서버 주소는 --server 나 CODEDRILL_SERVER (기본 %s). 승인 화면이 다른 주소면 CODEDRILL_WEB.
`

const defaultServer = "http://localhost:8080"

func main() {
	if err := run(os.Args[1:], os.Stdout); err != nil {
		fmt.Fprintln(os.Stderr, "codedrill:", err)
		os.Exit(1)
	}
}

func run(args []string, out io.Writer) error {
	if len(args) == 0 {
		fmt.Fprintf(out, usage, defaultServer)
		return nil
	}
	command, rest := args[0], args[1:]
	switch command {
	case "login":
		return login(rest, out)
	case "logout":
		return logout(out)
	case "whoami":
		return whoami(out)
	case "get":
		return get(rest, out)
	case "test":
		return test(out)
	case "submit":
		return submit(rest, out)
	case "version", "--version", "-v":
		fmt.Fprintf(out, "codedrill %s (%s/%s)\n", version, runtime.GOOS, runtime.GOARCH)
		return nil
	case "help", "--help", "-h":
		fmt.Fprintf(out, usage, defaultServer)
		return nil
	}
	return fmt.Errorf("모르는 명령입니다: %s — codedrill help", command)
}

func userAgent() string {
	return fmt.Sprintf("codedrill/%s (%s; %s)", version, runtime.GOOS, runtime.GOARCH)
}

func serverFromEnv() string {
	if server := os.Getenv("CODEDRILL_SERVER"); server != "" {
		return server
	}
	return defaultServer
}

// signedIn 은 저장된 세션으로 클라이언트를 만든다. 갱신된 토큰은 바로 저장한다.
func signedIn() (*api.Client, error) {
	stored, err := auth.Load()
	if err != nil {
		return nil, err
	}
	if stored == nil || stored.Session == nil {
		return nil, errors.New("로그인하지 않았습니다. codedrill login 을 먼저 하세요")
	}
	client := api.New(stored.Server, userAgent())
	client.Session = stored.Session
	client.OnRefresh = func(session *api.Session) error {
		return auth.Save(&auth.Stored{Server: stored.Server, Session: session})
	}
	return client, nil
}

func deviceName() string {
	host, err := os.Hostname()
	if err != nil || host == "" {
		host = "unknown"
	}
	host = strings.TrimSuffix(host, ".local")
	if shell := filepath.Base(os.Getenv("SHELL")); shell != "." && shell != "" && runtime.GOOS != "windows" {
		return fmt.Sprintf("%s (%s)", host, shell)
	}
	return host
}

func osName() string {
	switch runtime.GOOS {
	case "darwin":
		return "macOS"
	case "windows":
		return "Windows"
	case "linux":
		return "Linux"
	}
	return runtime.GOOS
}

func login(args []string, out io.Writer) error {
	flags := flag.NewFlagSet("login", flag.ContinueOnError)
	server := flags.String("server", serverFromEnv(), "서버 주소")
	noBrowser := flags.Bool("no-browser", false, "브라우저를 열지 않는다")
	if err := flags.Parse(args); err != nil {
		return err
	}
	client := api.New(*server, userAgent())
	code, err := client.StartDevice(deviceName(), fmt.Sprintf("codedrill %s · %s", version, osName()))
	if err != nil {
		return err
	}
	web := os.Getenv("CODEDRILL_WEB")
	if web == "" {
		web = client.Server
	}
	link := strings.TrimRight(web, "/") + code.VerificationURI + "?code=" + url.QueryEscape(code.UserCode)

	fmt.Fprintf(out, "\n  브라우저에서 이 코드를 확인하고 승인하세요:  %s\n\n  %s\n\n", code.UserCode, link)
	if !*noBrowser {
		openBrowser(link, out)
	}
	fmt.Fprintln(out, "  승인을 기다리는 중… (Ctrl+C 로 그만둔다)")
	session, err := waitForApproval(client, code, time.Sleep, time.Now)
	if err != nil {
		return err
	}
	if err := auth.Save(&auth.Stored{Server: client.Server, Session: session}); err != nil {
		return fmt.Errorf("승인됐지만 저장하지 못했습니다: %w", err)
	}
	fmt.Fprintf(out, "\n  연결됐습니다 — %s. 웹의 계정 설정 → 연결된 기기에서 끊을 수 있습니다.\n", session.DisplayName)
	return nil
}

func logout(out io.Writer) error {
	client, err := signedIn()
	if err != nil {
		return err
	}
	if err := client.Logout(); err != nil && !errors.Is(err, api.ErrSignedOut) {
		fmt.Fprintln(out, "서버에 알리지 못했습니다 — 웹의 연결된 기기에서 끊으세요:", err)
	}
	if err := auth.Clear(); err != nil {
		return err
	}
	fmt.Fprintln(out, "이 기기의 연결을 지웠습니다.")
	return nil
}

func whoami(out io.Writer) error {
	client, err := signedIn()
	if err != nil {
		return err
	}
	me, err := client.Me()
	if err != nil {
		return err
	}
	fmt.Fprintf(out, "%s · %s\n", me.DisplayName, client.Server)
	return nil
}

func get(args []string, out io.Writer) error {
	if len(args) == 0 {
		return errors.New("문제 id 를 주세요 — codedrill get job-queue")
	}
	id := args[0]
	dest := "."
	if len(args) > 1 {
		dest = args[1]
	}
	client, err := signedIn()
	if err != nil {
		// 키트는 공개다 — 로그인 전에도 받아 볼 수 있다
		client = api.New(serverFromEnv(), userAgent())
	}
	data, err := client.Kit(id)
	if err != nil {
		return err
	}
	written, err := kit.Extract(data, dest)
	if err != nil {
		return err
	}
	folder := filepath.Join(dest, id)
	_, info, err := kit.Find(folder)
	if err != nil {
		return err
	}
	command, _ := kit.RunCommand(info.Language)
	fmt.Fprintf(out, "%s (v%d, %s) — 파일 %d 개를 %s 에 받았습니다.\n\n", info.Title, info.Version, info.Language, len(written), folder)
	fmt.Fprintf(out, "  cd %s\n  codedrill test      # 공개 테스트 (%s)\n  codedrill submit    # 제출 — 숨은 테스트로 채점\n", folder, strings.Join(command, " "))
	return nil
}

func test(out io.Writer) error {
	root, info, err := kit.Find(".")
	if err != nil {
		return err
	}
	command, err := kit.RunCommand(info.Language)
	if err != nil {
		return err
	}
	if _, err := exec.LookPath(command[0]); err != nil {
		return fmt.Errorf("%s 가 없습니다 — CODEDRILL.md 의 준비물을 보세요", command[0])
	}
	cmd := exec.Command(command[0], command[1:]...)
	cmd.Dir = root
	cmd.Stdout, cmd.Stderr, cmd.Stdin = out, os.Stderr, os.Stdin
	if err := cmd.Run(); err != nil {
		var exit *exec.ExitError
		if errors.As(err, &exit) {
			return fmt.Errorf("공개 테스트가 통과하지 않았습니다 (종료 코드 %d)", exit.ExitCode())
		}
		return err
	}
	return nil
}

func submit(args []string, out io.Writer) error {
	flags := flag.NewFlagSet("submit", flag.ContinueOnError)
	noWait := flags.Bool("no-wait", false, "판정을 기다리지 않는다")
	if err := flags.Parse(args); err != nil {
		return err
	}
	root, info, err := kit.Find(".")
	if err != nil {
		return err
	}
	files, err := kit.Collect(root, info)
	if err != nil {
		return err
	}
	client, err := signedIn()
	if err != nil {
		return err
	}
	submission, err := client.Submit(info.ID, info.Version, files)
	if api.Code(err) == "PROBLEM_VERSION_STALE" {
		return fmt.Errorf("문제가 새 판으로 바뀌었습니다 (받은 판 v%d). 다른 폴더에 codedrill get %s 로 새로 받아 코드를 옮기세요", info.Version, info.ID)
	}
	if err != nil {
		return err
	}
	fmt.Fprintf(out, "제출했습니다 — 파일 %d 개, v%d.\n", len(files), info.Version)
	if *noWait {
		return nil
	}
	submission, err = waitForVerdict(client, submission, out, time.Sleep)
	if err != nil {
		return err
	}
	printVerdict(submission, out)
	if submission.Verdict == nil || *submission.Verdict != "ACCEPTED" {
		return errors.New("통과하지 못했습니다")
	}
	return nil
}

func waitForVerdict(client *api.Client, submission *api.Submission, out io.Writer, sleep func(time.Duration)) (*api.Submission, error) {
	last := ""
	for deadline := 0; submission.Status != "COMPLETED"; deadline++ {
		if deadline > 600 {
			return nil, errors.New("판정이 너무 오래 걸립니다 — 웹의 작업 공간에서 결과를 보세요")
		}
		if step := stepOf(submission.Status); step != last {
			fmt.Fprintln(out, "  "+step)
			last = step
		}
		sleep(time.Second)
		next, err := client.Submission(submission.ID)
		if err != nil {
			return nil, err
		}
		submission = next
	}
	return submission, nil
}

// stepOf 는 웹의 judgingStep 과 같은 세 단계로 말한다 — 서버가 아는 상태가 셋이다.
func stepOf(status string) string {
	switch status {
	case "QUEUED":
		return "대기 중…"
	default:
		return "빌드와 테스트 중…"
	}
}

var verdictNames = map[string]string{
	"ACCEPTED":      "맞았습니다",
	"WRONG_ANSWER":  "틀렸습니다",
	"COMPILE_ERROR": "컴파일 오류",
	"RUNTIME_ERROR": "런타임 오류",
	"TIME_LIMIT":    "시간 초과",
	"MEMORY_LIMIT":  "메모리 초과",
	"OUTPUT_LIMIT":  "출력 초과",
	"SYSTEM_ERROR":  "채점 오류",
}

func printVerdict(s *api.Submission, out io.Writer) {
	verdict := "판정 없음"
	if s.Verdict != nil {
		verdict = *s.Verdict
		if name, ok := verdictNames[verdict]; ok {
			verdict = name
		}
	}
	score := ""
	if s.Score != nil {
		score = fmt.Sprintf(" · %d점", *s.Score)
	}
	fmt.Fprintf(out, "\n%s%s\n", verdict, score)
	passed := 0
	for _, t := range s.Tests {
		if t.Passed {
			passed++
		}
	}
	if len(s.Tests) > 0 {
		fmt.Fprintf(out, "  보이는 테스트  %d / %d\n", passed, len(s.Tests))
	}
	if s.HiddenPassed != nil && s.HiddenTotal != nil {
		fmt.Fprintf(out, "  숨은 테스트    %d / %d\n", *s.HiddenPassed, *s.HiddenTotal)
	}
	if s.Probe != nil && len(s.Probe.Killed)+len(s.Probe.Survived) > 0 {
		fmt.Fprintf(out, "  테스트 점검    오답 %d 개 중 %d 개를 잡았습니다\n", len(s.Probe.Killed)+len(s.Probe.Survived), len(s.Probe.Killed))
	}
	for _, t := range s.Tests {
		if !t.Passed {
			why := ""
			if t.Message != nil {
				why = " — " + *t.Message
			}
			fmt.Fprintf(out, "  ✕ %s.%s%s\n", t.Module, t.Name, why)
		}
	}
	if s.Log != nil && *s.Log != "" && (s.Verdict == nil || *s.Verdict == "COMPILE_ERROR") {
		fmt.Fprintf(out, "\n%s\n", *s.Log)
	}
}

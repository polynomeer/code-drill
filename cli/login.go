package main

import (
	"errors"
	"fmt"
	"io"
	"os/exec"
	"runtime"
	"time"

	"github.com/polynomeer/code-drill/cli/internal/api"
)

// waitForApproval 는 RFC 8628 §3.5 대로 묻는다 — 기다리라면 간격대로, 너무 잦다면 5초 늘려서,
// 거절·만료면 멈춘다. sleep 은 시험에서 바꾼다.
func waitForApproval(client *api.Client, code *api.DeviceCode, sleep func(time.Duration), now func() time.Time) (*api.Session, error) {
	interval := time.Duration(code.Interval) * time.Second
	if interval <= 0 {
		interval = 5 * time.Second
	}
	for {
		if !code.ExpiresAt.IsZero() && now().After(code.ExpiresAt) {
			return nil, errors.New("코드가 만료됐습니다. codedrill login 을 다시 하세요")
		}
		sleep(interval)
		session, err := client.PollDevice(code.DeviceCode)
		if err == nil {
			return session, nil
		}
		switch api.Code(err) {
		case "AUTHORIZATION_PENDING":
		case "SLOW_DOWN":
			interval += 5 * time.Second
		case "ACCESS_DENIED":
			return nil, errors.New("웹에서 거절했습니다")
		case "DEVICE_CODE_EXPIRED":
			return nil, errors.New("코드가 만료됐습니다. codedrill login 을 다시 하세요")
		default:
			return nil, err
		}
	}
}

// openBrowser 는 실패해도 괜찮다 — 주소는 이미 화면에 찍었다.
func openBrowser(url string, out io.Writer) {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("open", url)
	case "windows":
		cmd = exec.Command("rundll32", "url.dll,FileProtocolHandler", url)
	default:
		cmd = exec.Command("xdg-open", url)
	}
	if err := cmd.Start(); err != nil {
		fmt.Fprintln(out, "  (브라우저를 열지 못했습니다 — 위 주소를 직접 여세요)")
	}
}

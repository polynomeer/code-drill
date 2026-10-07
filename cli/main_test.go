package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/polynomeer/code-drill/cli/internal/api"
)

// 서버가 기다리라 → 너무 잦다 → 승인 순으로 답할 때, 간격을 늘리고 세션을 받는다.
func TestWaitForApprovalBacksOffThenSucceeds(t *testing.T) {
	answers := []string{"AUTHORIZATION_PENDING", "SLOW_DOWN", ""}
	calls := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/v1/auth/device/token" {
			t.Fatalf("다른 곳을 불렀다: %s", r.URL.Path)
		}
		answer := answers[calls]
		calls++
		if answer != "" {
			w.WriteHeader(400)
			json.NewEncoder(w).Encode(map[string]string{"errorCode": answer, "message": "기다림"})
			return
		}
		json.NewEncoder(w).Encode(api.Session{AccessToken: "a", RefreshToken: "r", DisplayName: "시험"})
	}))
	defer server.Close()

	var slept []time.Duration
	session, err := waitForApproval(api.New(server.URL, "test"), &api.DeviceCode{DeviceCode: "d", Interval: 5},
		func(d time.Duration) { slept = append(slept, d) }, time.Now)
	if err != nil || session.DisplayName != "시험" {
		t.Fatalf("%v %v", session, err)
	}
	if want := []time.Duration{5 * time.Second, 5 * time.Second, 10 * time.Second}; !equal(slept, want) {
		t.Fatalf("간격 %v, 기대 %v", slept, want)
	}
}

func TestWaitForApprovalStopsWhenDenied(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(400)
		json.NewEncoder(w).Encode(map[string]string{"errorCode": "ACCESS_DENIED"})
	}))
	defer server.Close()
	_, err := waitForApproval(api.New(server.URL, "test"), &api.DeviceCode{DeviceCode: "d", Interval: 1}, func(time.Duration) {}, time.Now)
	if err == nil || !strings.Contains(err.Error(), "거절") {
		t.Fatalf("거절이면 멈춰야 한다: %v", err)
	}
}

// 접근 토큰이 끝나면 한 번 갱신하고, 새 세션을 저장하게 알린 뒤 다시 묻는다.
func TestClientRefreshesOnce(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch {
		case r.URL.Path == "/api/v1/auth/refresh":
			json.NewEncoder(w).Encode(api.Session{AccessToken: "new", RefreshToken: "r2"})
		case r.Header.Get("Authorization") == "Bearer new":
			json.NewEncoder(w).Encode(api.Me{DisplayName: "시험"})
		default:
			w.WriteHeader(401)
		}
	}))
	defer server.Close()
	client := api.New(server.URL, "test")
	client.Session = &api.Session{AccessToken: "old", RefreshToken: "r"}
	var saved *api.Session
	client.OnRefresh = func(s *api.Session) error { saved = s; return nil }
	me, err := client.Me()
	if err != nil || me.DisplayName != "시험" || saved == nil || saved.RefreshToken != "r2" {
		t.Fatalf("%v %v %v", me, err, saved)
	}
}

func TestPrintVerdictShowsCountsNotHiddenNames(t *testing.T) {
	verdict, score, hp, ht := "WRONG_ANSWER", 70, 9, 12
	why := "expected <1> but was <0>"
	var out bytes.Buffer
	printVerdict(&api.Submission{
		Status: "COMPLETED", Verdict: &verdict, Score: &score, HiddenPassed: &hp, HiddenTotal: &ht,
		Tests: []api.TestResult{{Module: "tests.T", Name: "a", Passed: true}, {Module: "tests.T", Name: "b", Message: &why}},
		Probe: &api.Probe{Killed: []string{"x", "y", "z"}, Survived: []string{"w"}},
	}, &out)
	for _, want := range []string{"틀렸습니다 · 70점", "보이는 테스트  1 / 2", "숨은 테스트    9 / 12", "오답 4 개 중 3 개", "✕ tests.T.b — expected"} {
		if !strings.Contains(out.String(), want) {
			t.Errorf("%q 가 없다:\n%s", want, out.String())
		}
	}
}

func equal(a, b []time.Duration) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

// 제출은 Idempotency-Key 를 싣고, 판을 함께 보낸다.
func TestSubmitSendsKeyAndVersion(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			ProjectVersion int `json:"projectVersion"`
		}
		json.NewDecoder(r.Body).Decode(&body)
		if len(r.Header.Get("Idempotency-Key")) != 36 || body.ProjectVersion != 3 {
			w.WriteHeader(400)
			return
		}
		json.NewEncoder(w).Encode(api.Submission{ID: "ps-1", Status: "QUEUED"})
	}))
	defer server.Close()
	client := api.New(server.URL, "test")
	client.Session = &api.Session{AccessToken: "a"}
	if s, err := client.Submit("job-queue", 3, map[string]string{"a": "b"}); err != nil || s.ID != "ps-1" {
		t.Fatalf("%v %v", s, err)
	}
}

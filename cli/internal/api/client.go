// Package api 는 CodeDrill 서버의 프로젝트형·기기 승인 API 를 부른다. 표준 라이브러리만 쓴다 —
// 단일 실행 파일이 의존성 없이 빌드되어야 배포 채널(Homebrew·Scoop·스크립트)이 단순하다.
package api

import (
	"bytes"
	"crypto/rand"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

// Session 은 웹 로그인과 같은 모양이다 (SessionResponse).
type Session struct {
	AccessToken      string    `json:"accessToken"`
	RefreshToken     string    `json:"refreshToken"`
	AccessExpiresAt  time.Time `json:"accessExpiresAt"`
	RefreshExpiresAt time.Time `json:"refreshExpiresAt"`
	UserID           string    `json:"userId"`
	DisplayName      string    `json:"displayName"`
}

// Error 는 서버의 ApiError 다. 오류 코드로 갈라 처리한다.
type Error struct {
	Status    int    `json:"-"`
	ErrorCode string `json:"errorCode"`
	Message   string `json:"message"`
}

func (e *Error) Error() string {
	if e.ErrorCode == "" {
		if e.Message != "" {
			return fmt.Sprintf("%s (%d)", e.Message, e.Status)
		}
		return fmt.Sprintf("서버가 %d 로 답했습니다", e.Status)
	}
	return fmt.Sprintf("%s (%s)", e.Message, e.ErrorCode)
}

// Code 는 err 가 서버 오류면 그 코드, 아니면 빈 문자열.
func Code(err error) string {
	var apiErr *Error
	if errors.As(err, &apiErr) {
		return apiErr.ErrorCode
	}
	return ""
}

// Client 는 토큰이 만료되면 한 번 갱신하고 다시 묻는다. 갱신된 세션은 OnRefresh 로 알려 저장하게 한다.
type Client struct {
	Server    string
	UserAgent string
	Session   *Session
	OnRefresh func(*Session) error
	HTTP      *http.Client
}

func New(server, userAgent string) *Client {
	return &Client{Server: strings.TrimRight(server, "/"), UserAgent: userAgent, HTTP: &http.Client{Timeout: 60 * time.Second}}
}

func (c *Client) do(method, path string, body any, authed bool, headers ...string) (*http.Response, error) {
	var payload []byte
	if body != nil {
		var err error
		if payload, err = json.Marshal(body); err != nil {
			return nil, err
		}
	}
	send := func() (*http.Response, error) {
		req, err := http.NewRequest(method, c.Server+"/api/v1"+path, bytes.NewReader(payload))
		if err != nil {
			return nil, err
		}
		req.Header.Set("User-Agent", c.UserAgent)
		req.Header.Set("Accept", "application/json")
		if body != nil {
			req.Header.Set("Content-Type", "application/json")
		}
		if authed && c.Session != nil {
			req.Header.Set("Authorization", "Bearer "+c.Session.AccessToken)
		}
		for i := 0; i+1 < len(headers); i += 2 {
			req.Header.Set(headers[i], headers[i+1])
		}
		return c.HTTP.Do(req)
	}
	res, err := send()
	if err != nil {
		return nil, fmt.Errorf("%s 에 닿지 못했습니다: %w", c.Server, err)
	}
	if res.StatusCode == http.StatusUnauthorized && authed && c.Session != nil {
		res.Body.Close()
		if err := c.refresh(); err != nil {
			return nil, err
		}
		return send()
	}
	return res, nil
}

func (c *Client) refresh() error {
	var fresh Session
	err := c.call(http.MethodPost, "/auth/refresh", map[string]string{"refreshToken": c.Session.RefreshToken}, false, &fresh)
	if err != nil {
		return ErrSignedOut
	}
	c.Session = &fresh
	if c.OnRefresh != nil {
		return c.OnRefresh(&fresh)
	}
	return nil
}

// ErrSignedOut — 갱신 토큰도 끝났거나 웹에서 연결을 끊었다.
var ErrSignedOut = errors.New("로그인이 끝났습니다. codedrill login 을 다시 하세요")

func (c *Client) call(method, path string, body any, authed bool, out any, headers ...string) error {
	res, err := c.do(method, path, body, authed, headers...)
	if err != nil {
		return err
	}
	defer res.Body.Close()
	return decode(res, out)
}

func decode(res *http.Response, out any) error {
	data, err := io.ReadAll(res.Body)
	if err != nil {
		return err
	}
	if res.StatusCode >= 400 {
		apiErr := &Error{Status: res.StatusCode}
		_ = json.Unmarshal(data, apiErr)
		return apiErr
	}
	if out == nil || len(data) == 0 {
		return nil
	}
	return json.Unmarshal(data, out)
}

// ─── 기기 승인 (RFC 8628) ───

type DeviceCode struct {
	DeviceCode      string    `json:"deviceCode"`
	UserCode        string    `json:"userCode"`
	VerificationURI string    `json:"verificationUri"`
	ExpiresAt       time.Time `json:"expiresAt"`
	Interval        int       `json:"interval"`
}

func (c *Client) StartDevice(deviceName, client string) (*DeviceCode, error) {
	var out DeviceCode
	err := c.call(http.MethodPost, "/auth/device/code", map[string]string{"deviceName": deviceName, "client": client}, false, &out)
	return &out, err
}

// PollDevice 는 승인됐으면 세션, 아니면 서버 오류(AUTHORIZATION_PENDING·SLOW_DOWN·ACCESS_DENIED·DEVICE_CODE_EXPIRED).
func (c *Client) PollDevice(deviceCode string) (*Session, error) {
	var out Session
	if err := c.call(http.MethodPost, "/auth/device/token", map[string]string{"deviceCode": deviceCode}, false, &out); err != nil {
		return nil, err
	}
	return &out, nil
}

type Me struct {
	ID          string `json:"id"`
	DisplayName string `json:"displayName"`
}

func (c *Client) Me() (*Me, error) {
	var out Me
	return &out, c.call(http.MethodGet, "/auth/me", nil, true, &out)
}

func (c *Client) Logout() error {
	return c.call(http.MethodPost, "/auth/logout", nil, true, nil)
}

// ─── 프로젝트형 ───

type Project struct {
	ID       string `json:"id"`
	Version  int    `json:"version"`
	Title    string `json:"title"`
	Language string `json:"language"`
}

func (c *Client) Project(id string) (*Project, error) {
	var out Project
	return &out, c.call(http.MethodGet, "/projects/"+id, nil, true, &out)
}

// Kit 은 키트 ZIP 의 바이트. 공개 API 지만 로그인돼 있으면 토큰을 싣는다 — 서버 기록에 남게.
func (c *Client) Kit(id string) ([]byte, error) {
	res, err := c.do(http.MethodGet, "/projects/"+id+"/kit", nil, true)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()
	if res.StatusCode >= 400 {
		return nil, decode(res, nil)
	}
	return io.ReadAll(res.Body)
}

type TestResult struct {
	Module  string  `json:"module"`
	Name    string  `json:"name"`
	Passed  bool    `json:"passed"`
	Message *string `json:"message"`
}

type Probe struct {
	ReferencePassed bool     `json:"referencePassed"`
	Killed          []string `json:"killed"`
	Survived        []string `json:"survived"`
}

type Submission struct {
	ID             string       `json:"id"`
	ProjectID      string       `json:"projectId"`
	ProjectVersion int          `json:"projectVersion"`
	Status         string       `json:"status"`
	Verdict        *string      `json:"verdict"`
	Score          *int         `json:"score"`
	Log            *string      `json:"log"`
	Tests          []TestResult `json:"tests"`
	HiddenPassed   *int         `json:"hiddenPassed"`
	HiddenTotal    *int         `json:"hiddenTotal"`
	Probe          *Probe       `json:"probe"`
	Source         string       `json:"source"`
	Device         *string      `json:"device"`
}

// Submit 은 키 하나로 한 번 낸다 — 토큰을 갱신하고 다시 보내도 같은 키라 제출이 둘이 되지 않는다.
func (c *Client) Submit(id string, version int, files map[string]string) (*Submission, error) {
	var out Submission
	body := map[string]any{"files": files, "projectVersion": version}
	return &out, c.call(http.MethodPost, "/projects/"+id+"/submissions", body, true, &out, "Idempotency-Key", newKey())
}

func newKey() string {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		panic(err)
	}
	b[6], b[8] = b[6]&0x0f|0x40, b[8]&0x3f|0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}

func (c *Client) Submission(id string) (*Submission, error) {
	var out Submission
	return &out, c.call(http.MethodGet, "/projects/submissions/"+id, nil, true, &out)
}

// Package auth 는 기기 세션을 사용자 설정 폴더에 저장한다 (권한 0600).
package auth

import (
	"encoding/json"
	"errors"
	"os"
	"path/filepath"

	"github.com/polynomeer/code-drill/cli/internal/api"
)

// Stored 는 서버 주소와 그 서버의 세션. 서버를 바꾸면 다시 로그인한다.
type Stored struct {
	Server  string       `json:"server"`
	Session *api.Session `json:"session"`
}

func path() (string, error) {
	if dir := os.Getenv("CODEDRILL_CONFIG_DIR"); dir != "" {
		return filepath.Join(dir, "credentials.json"), nil
	}
	base, err := os.UserConfigDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(base, "codedrill", "credentials.json"), nil
}

func Load() (*Stored, error) {
	p, err := path()
	if err != nil {
		return nil, err
	}
	data, err := os.ReadFile(p)
	if errors.Is(err, os.ErrNotExist) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	var stored Stored
	if err := json.Unmarshal(data, &stored); err != nil {
		return nil, nil
	}
	return &stored, nil
}

func Save(stored *Stored) error {
	p, err := path()
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(p), 0o700); err != nil {
		return err
	}
	data, err := json.MarshalIndent(stored, "", "  ")
	if err != nil {
		return err
	}
	tmp := p + ".tmp"
	if err := os.WriteFile(tmp, data, 0o600); err != nil {
		return err
	}
	return os.Rename(tmp, p)
}

func Clear() error {
	p, err := path()
	if err != nil {
		return err
	}
	if err := os.Remove(p); err != nil && !errors.Is(err, os.ErrNotExist) {
		return err
	}
	return nil
}

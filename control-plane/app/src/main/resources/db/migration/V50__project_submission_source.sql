/*
 * 프로젝트형 제출의 출처 — 웹에서 냈나, CLI 로 어느 기기에서 냈나 (feature-roadmap 11단계 이어서 — 2단계).
 *
 * 작업 공간이 기록에 "CLI · MacBook-Pro" 를 보이고, CLI 로 낸 파일이 웹 초안과 다르면 알려 주는 데 쓴다.
 * 판정·증거와는 상관없다 — 어디서 냈든 같은 판정기가 같은 규칙으로 채점한다.
 */
ALTER TABLE project_submission
    ADD COLUMN source      TEXT NOT NULL DEFAULT 'WEB' CHECK (source IN ('WEB', 'CLI')),
    ADD COLUMN device_name TEXT;

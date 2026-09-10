-- 최초 분기 진단 (PRD FR-805).
--
-- > 리플레이 중 다음 상태 예측과 최초 분기 진단을 지원합니다. 선택·근거·결과가
-- > 코드·입력·이벤트 시점과 연결됩니다.

-- 참조 풀이의 트레이스 지문.
--
-- **이벤트 본문을 담지 않는다.** 담는 것은 비교에 필요한 정규화 키뿐이다 — 종류·대상·
-- 결과값을 이은 문자열. 참조 풀이가 무엇을 했는지 전부 저장해 두면 언젠가 그것을 읽는
-- 조회가 생기고, 그러면 정답 알고리즘이 통째로 나간다 (§8.3).
--
-- 문제 버전과 케이스의 함수라 한 번만 계산한다. 문제를 고치면 새 버전이 되고, 이 표에는
-- 새 행이 생긴다 — 옛 행은 옛 판정을 되짚을 때 여전히 맞다.
CREATE TABLE reference_trace (
    problem_version_id TEXT        NOT NULL,
    case_id            TEXT        NOT NULL,

    -- 정규화 키 배열. 길이가 곧 참조 풀이의 이벤트 수다.
    keys               JSONB       NOT NULL,

    -- 계측 호출이 없어 비었는지, 돌지 않았는지. 빈 지문과 실패를 가려야 화면이
    -- "참조와 비교할 수 없다"의 이유를 말할 수 있다.
    status             TEXT        NOT NULL,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (problem_version_id, case_id)
);

-- 제출 하나의 분기 진단.
--
-- 트레이스와 나눈다. 트레이스는 30일 보존이고(§7.4) 이것은 훨씬 작아 오래 남길 수 있다 —
-- "무엇을 반복해서 틀리는가"는 원본 이벤트가 사라진 뒤에도 답할 수 있어야 한다.
CREATE TABLE submission_divergence (
    submission_id   UUID        PRIMARY KEY,
    case_id         TEXT        NOT NULL,

    -- SAME / DIVERGED / DIFFERENT_APPROACH / NO_REFERENCE / PENDING
    outcome         TEXT        NOT NULL,

    -- 갈라지기 전까지 같았던 이벤트 수. 0 이면 처음부터 다른 길이다.
    shared_prefix   INT,

    -- 갈라진 지점의 사용자 이벤트 seq 와 코드 줄. 사용자 자신의 것이라 그대로 보여준다.
    diverged_at_seq BIGINT,
    source_line     INT,

    -- 그 시점에 참조 풀이가 한 일, 한 줄. **딱 한 이벤트만** 담는다.
    expected_step   TEXT,
    actual_step     TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE reference_trace IS
    '참조 풀이 트레이스의 정규화 지문. 이벤트 본문은 담지 않는다 (FR-805).';
COMMENT ON TABLE submission_divergence IS
    '제출이 참조 풀이와 처음 갈라진 지점. 참조의 한 이벤트만 노출한다 (FR-805).';

#!/usr/bin/env python3
"""문제 패키지 생성기 (기술 설계서 §6.1).

**패키지가 산출물이고 이 스크립트는 저작 도구다.** 공개된 패키지는 digest 로 고정되므로
(§6.1), 진실의 원천은 언제나 `content/problems/<id>/` 아래의 파일이다. 이 스크립트는
그 파일을 처음 만들 때와, 성능 케이스처럼 사람이 손으로 쓸 수 없는 데이터를 다시 뽑을
때 쓴다.

기대 출력은 여기 있는 **파이썬 참조 구현**이 계산하고, 채점은 패키지의 **코틀린 참조
구현**이 통과하는지로 검증한다. 두 구현이 어긋나면 `validateContent` 의
`official-solution` 검사가 실패한다 — 서로 다른 두 사람이 같은 문제를 푼 셈이라, 한쪽의
착각이 조용히 정답으로 굳는 일이 없다.

    python3 content/tools/author.py            # 전체 생성
    python3 content/tools/author.py two-sum    # 하나만
"""

from __future__ import annotations

import json
import pathlib
import random
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONTENT = ROOT / "content" / "problems"

# 케이스 데이터는 매번 같아야 한다. 다시 생성했을 때 digest 가 흔들리면 "무엇을 채점한
# 패키지인가"를 말할 수 없다 (§12.1 재현성).
SEED = 20260907


class Problem:
    def __init__(
        self,
        id,
        title,
        summary,
        signature,
        groups,
        reference,
        cases,
        kotlin,
        mutants,
        constraints,
        notes="",
        drill_doc="",
        limits=None,
    ):
        self.id = id
        self.title = title
        self.summary = summary
        self.signature = signature
        self.groups = groups
        self.reference = reference
        self.cases = cases
        self.kotlin = kotlin
        self.mutants = mutants
        self.constraints = constraints
        self.notes = notes
        self.drill_doc = drill_doc
        self.limits = limits or {"timeMillis": 2000, "memoryMb": 256, "outputBytes": 65536}


# 값 타입 → 코틀린 타입. 서버의 ValueType.kotlinType() 과 같아야 한다.
KOTLIN_TYPES = {
    "INT": "Int",
    "INT_ARRAY": "IntArray",
    "STRING": "String",
    "STRING_ARRAY": "Array<String>",
    "INT_MATRIX": "Array<IntArray>",
}


def kotlin_signature(problem) -> str:
    params = ", ".join(
        f"{name}: {KOTLIN_TYPES[kind]}" for name, kind in problem.signature["parameters"]
    )
    return f"fun {problem.signature['name']}({params}): {KOTLIN_TYPES[problem.signature['returns']]}"


def write_manifest(problem, directory):
    params = "\n".join(
        f"    - name: {name}\n      type: {kind}"
        for name, kind in problem.signature["parameters"]
    )
    groups = "\n".join(
        f"  - id: {g['id']}\n"
        f"    weight: {g['weight']}\n"
        f"    visibility: {g['visibility']}\n"
        f"    aggregation: {g['aggregation']}\n"
        f"    stopPolicy: {g['stopPolicy']}"
        + (
            f"\n    limitMultiplier:\n      time: {g['timeMultiplier']}"
            if g.get("timeMultiplier")
            else ""
        )
        for g in problem.groups
    )
    (directory / "manifest.yaml").write_text(
        f"""# Problem Package manifest (기술 설계서 §6.1).
# 이 파일과 tests/ 의 내용이 problem_version_id 를 결정한다. 공개 후에는 불변이다.

id: {problem.id}
version: 1
title: {problem.title}
statement: statement.md

limits:
  timeMillis: {problem.limits['timeMillis']}
  memoryMb: {problem.limits['memoryMb']}
  outputBytes: {problem.limits['outputBytes']}

# 함수형 풀이. Runner 가 이 시그니처로 하네스를 생성한다 (§5.3 prepare).
signature:
  name: {problem.signature['name']}
  parameters:
{params}
  returns: {problem.signature['returns']}

# 테스트 그룹 정책 (§6.2). 총점은 weight 합이 100 이어야 한다.
groups:
{groups}
"""
    )


def write_statement(problem, directory):
    drill = ""
    if problem.drill_doc:
        drill = f"""
## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
{problem.drill_doc.strip()}
```
"""
    notes = f"\n{problem.notes.strip()}\n" if problem.notes else ""
    (directory / "statement.md").write_text(
        f"""# {problem.title}

{problem.summary.strip()}

```kotlin
{kotlin_signature(problem)}
```
{notes}{drill}
## 제약

{problem.constraints.strip()}
"""
    )


INT_MIN, INT_MAX = -(2 ** 31), 2 ** 31 - 1


def _leaves(value):
    """중첩을 풀어 스칼라만 남긴다. 격자는 배열의 배열이라 한 겹으로는 부족하다."""
    if isinstance(value, list):
        for item in value:
            yield from _leaves(item)
    else:
        yield value


def check_expected(problem, group, name, expected):
    """기대 출력이 하네스와 제한을 지나갈 수 있는지 (§5.2 출력, §5.3 check).

    두 가지를 여기서 잡는다. 이 둘은 채점기가 알려 주기는 하지만, 그때는 이미 케이스
    데이터를 다 만든 뒤라 원인을 되짚기가 번거롭다.

    - **Int 범위.** 파이썬은 큰 정수를 그냥 계산하지만 코틀린 참조 구현은 넘친다.
      기대 출력이 범위를 넘으면 두 구현이 다른 답을 내는 것이 정상이 된다.
    - **출력 한도.** 배열을 돌려주는 문제는 원소 수가 곧 출력 크기다. 성능 케이스를
      키우다 보면 정답 풀이가 OUTPUT_LIMIT 으로 떨어진다.
    """
    values = list(_leaves(expected))
    for value in values:
        if isinstance(value, str):
            continue
        if not (INT_MIN <= value <= INT_MAX):
            raise SystemExit(
                f"{problem.id} {group}/{name}: 기대 출력이 Int 범위를 넘는다 ({value}). "
                "입력을 줄이거나 문제의 제약을 바꾼다"
            )

    # 하네스는 쉼표로 이어 한 줄로 내보낸다. 문자열은 Base64 라 4/3 배로 늘어난다.
    size = sum(
        (len(v.encode("utf-8")) * 4 // 3 + 4) if isinstance(v, str) else (len(str(v)) + 1)
        for v in values
    )
    if size > problem.limits["outputBytes"]:
        raise SystemExit(
            f"{problem.id} {group}/{name}: 출력 {size}바이트가 한도 "
            f"{problem.limits['outputBytes']}를 넘는다. limits.outputBytes 를 올리거나 "
            "케이스를 줄인다"
        )


def write_cases(problem, directory):
    for group, cases in problem.cases.items():
        group_dir = directory / "tests" / group
        group_dir.mkdir(parents=True, exist_ok=True)
        for name, args in cases:
            expected = problem.reference(*[list(a) if isinstance(a, list) else a for a in args])
            check_expected(problem, group, name, expected)
            payload = {"args": args, "expected": expected}
            (group_dir / f"{name}.json").write_text(json.dumps(payload) + "\n")


def write_sources(problem, directory):
    (directory / "solutions").mkdir(parents=True, exist_ok=True)
    (directory / "solutions" / "reference.kt").write_text(problem.kotlin.strip() + "\n")

    mutant_dir = directory / "mutants"
    mutant_dir.mkdir(parents=True, exist_ok=True)
    for name, kind, why, source in problem.mutants:
        (mutant_dir / f"{name}.kt").write_text(
            f"// kind: {kind}\n// {why}\n{source.strip()}\n"
        )


def emit(problem):
    directory = CONTENT / problem.id
    directory.mkdir(parents=True, exist_ok=True)
    write_manifest(problem, directory)
    write_statement(problem, directory)
    write_cases(problem, directory)
    write_sources(problem, directory)
    total = sum(len(v) for v in problem.cases.values())
    print(f"{problem.id:<28} 케이스 {total:>3}개, 오답 {len(problem.mutants)}개")


# --- 그룹 정책 조합 -----------------------------------------------------------

def standard_groups(boundary=40, hidden=60):
    """공개 샘플 + 경계 + 숨은. 대부분의 문제가 이 모양이다."""
    return [
        dict(id="sample", weight=0, visibility="PUBLIC",
             aggregation="ALL_OR_NOTHING", stopPolicy="FAIL_FAST"),
        dict(id="boundary", weight=boundary, visibility="HIDDEN",
             aggregation="ALL_OR_NOTHING", stopPolicy="CONTINUE"),
        dict(id="hidden", weight=hidden, visibility="HIDDEN",
             aggregation="ALL_OR_NOTHING", stopPolicy="CONTINUE"),
    ]


def perf_groups(boundary=20, hidden=40, performance=40):
    """성능 그룹이 있는 문제. SUM 이라 부분 점수가 나온다 (§6.2)."""
    return [
        dict(id="sample", weight=0, visibility="PUBLIC",
             aggregation="ALL_OR_NOTHING", stopPolicy="FAIL_FAST"),
        dict(id="boundary", weight=boundary, visibility="HIDDEN",
             aggregation="ALL_OR_NOTHING", stopPolicy="CONTINUE"),
        dict(id="hidden", weight=hidden, visibility="HIDDEN",
             aggregation="ALL_OR_NOTHING", stopPolicy="CONTINUE"),
        # 느린 풀이도 작은 입력은 통과한다. 어디서 무너지는지가 점수로 드러난다.
        dict(id="performance", weight=performance, visibility="HIDDEN",
             aggregation="SUM", stopPolicy="CONTINUE"),
    ]


def randoms(count, low, high, salt=0):
    """재현 가능한 난수 배열.

    호출마다 새 [random.Random] 을 만든다 — 하나를 공유하면 문제를 추가하거나 순서를
    바꿀 때 기존 문제의 데이터까지 달라지고, 그러면 패키지 digest 가 흔들려 이미 공개한
    버전과 어긋난다 (§6.1 공개 후 불변).
    """
    source = random.Random(SEED + salt)
    return [source.randint(low, high) for _ in range(count)]


def shuffled(values, salt=0):
    out = list(values)
    random.Random(SEED + salt).shuffle(out)
    return out


def catalog() -> list[Problem]:
    """문제 목록.

    import 를 함수 안에서 한다. 카탈로그 모듈이 이 파일의 [Problem] 을 쓰므로, 위에서
    바로 import 하면 순환이 된다.
    """
    import catalog_arrays
    import catalog_dp
    import catalog_graph
    import catalog_math
    import catalog_search
    import catalog_strings
    import catalog_stack

    problems: list[Problem] = []
    for module in (
        catalog_arrays, catalog_dp, catalog_graph,
        catalog_math, catalog_search, catalog_stack, catalog_strings,
    ):
        problems += module.PROBLEMS

    ids = [p.id for p in problems]
    duplicated = {i for i in ids if ids.count(i) > 1}
    assert not duplicated, f"문제 id 가 겹친다: {duplicated}"
    return problems


def main() -> int:
    wanted = sys.argv[1:]
    problems = catalog()
    chosen = [p for p in problems if not wanted or p.id in wanted]
    if not chosen:
        print(f"그런 문제가 없다: {wanted}")
        return 1
    for problem in chosen:
        emit(problem)
    print(f"\n{len(chosen)}개 생성. 이제 검증한다:")
    print("  ./gradlew :judge:runner-agent:validateContent")
    return 0


if __name__ == "__main__":
    sys.exit(main())

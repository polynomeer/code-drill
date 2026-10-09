# kind: WRONG_BRANCH
# 조각 하나의 숨은 규칙은 지키지만 `**` 는 숨은 조각도 건너뛴다. `**/config` 가 `.git/config` 와 맞는다.
"""경로 글롭 매처. 패턴을 `/` 로 나눠 조각끼리 맞추고, `**` 조각만 경로 조각 여럿을 건너뛴다."""


def _parse_class(segment, start):
    """`segment[start]` 의 `[` 에서 시작하는 문자 클래스. 닫히지 않으면 None — 그때 `[` 는 그냥 글자다."""
    j = start + 1
    negate = False
    if j < len(segment) and segment[j] == "!":
        negate = True
        j += 1
    ranges = []
    first = True
    while j < len(segment):
        c = segment[j]
        if c == "]" and not first:
            return ("class", negate, ranges), j + 1
        if c == "\\" and j + 1 < len(segment):
            j += 1
            c = segment[j]
        if j + 2 < len(segment) and segment[j + 1] == "-" and segment[j + 2] != "]":
            ranges.append((c, segment[j + 2]))
            j += 3
        else:
            ranges.append((c, c))
            j += 1
        first = False
    return None


def _tokens(segment):
    out = []
    i = 0
    while i < len(segment):
        c = segment[i]
        if c == "\\":
            if i + 1 < len(segment):
                out.append(("lit", segment[i + 1]))
                i += 2
            else:
                out.append(("lit", "\\"))
                i += 1
        elif c == "?":
            out.append(("one",))
            i += 1
        elif c == "*":
            # 이어진 별은 하나와 같다 — 조각 안의 `**` 도 `*` 다.
            if not out or out[-1] != ("star",):
                out.append(("star",))
            i += 1
        elif c == "[":
            parsed = _parse_class(segment, i)
            if parsed is None:
                out.append(("lit", "["))
                i += 1
            else:
                token, i = parsed
                out.append(token)
        else:
            out.append(("lit", c))
            i += 1
    return out


def _one(token, c):
    if token[0] == "lit":
        return token[1] == c
    if token[0] == "one":
        return True
    _, negate, ranges = token
    return any(low <= c <= high for low, high in ranges) != negate


def _match_segment(tokens, text):
    # 숨은 조각(`.` 으로 시작)은 패턴도 글자 그대로의 `.` 으로 시작해야 한다.
    if text.startswith(".") and (not tokens or tokens[0] != ("lit", ".")):
        return False
    n = len(text)
    # reach[k]: 지금까지의 토큰으로 text[:k] 를 정확히 덮을 수 있다. 별이 여럿이어도 토큰 × 글자다.
    reach = [False] * (n + 1)
    reach[0] = True
    for token in tokens:
        nxt = [False] * (n + 1)
        if token == ("star",):
            running = False
            for k in range(n + 1):
                running = running or reach[k]
                nxt[k] = running
        else:
            for k in range(n):
                if reach[k] and _one(token, text[k]):
                    nxt[k + 1] = True
        reach = nxt
    return reach[n]


def match(pattern: str, path: str) -> bool:
    parts = path.split("/")
    m = len(parts)
    reach = [False] * (m + 1)
    reach[0] = True
    for segment in pattern.split("/"):
        nxt = [False] * (m + 1)
        if segment == "**":
            # 0 개 이상의 조각을 건너뛴다. 숨은 조각은 건너뛰지 않는다.
            running = False
            for k in range(m + 1):
                running = reach[k] or running
                nxt[k] = running
        else:
            tokens = _tokens(segment)
            for k in range(m):
                if reach[k] and _match_segment(tokens, parts[k]):
                    nxt[k + 1] = True
        reach = nxt
    return reach[m]

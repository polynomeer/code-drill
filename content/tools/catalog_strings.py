"""문자열 (역량: 문자 단위로 훑고 비교하기).

네 문제가 문자열이 드나드는 **네 가지 방향**을 모두 지난다. 하나만 있으면 인코딩의
한쪽 방향만 확인되고, 반대쪽은 처음 쓰는 사람이 발견하게 된다.

| 문제 | 입력 → 출력 |
|---|---|
| is-palindrome | STRING → INT |
| longest-common-prefix | STRING_ARRAY → STRING |
| reverse-words | STRING_ARRAY → STRING_ARRAY |
| count-distinct-chars | STRING → INT_ARRAY |
"""

from author import Problem, standard_groups, perf_groups, randoms

PROBLEMS = []


# --- 31. 회문 판별 -----------------------------------------------------------

def _palindrome(text):
    kept = [c.lower() for c in text if c.isalnum()]
    return 1 if kept == kept[::-1] else 0


PROBLEMS.append(Problem(
    id="is-palindrome",
    title="회문인가",
    summary="""
문자열 `text` 가 회문이면 `1`, 아니면 `0` 을 반환한다.

- **영문자와 숫자만** 본다. 공백과 문장 부호는 무시한다.
- 대소문자는 구분하지 않는다.
""",
    notes="""
빈 문자열과, 볼 글자가 하나도 없는 문자열은 회문이다.
""",
    drill_doc="""
Drill.pointer("left", i)   // 왼쪽에서 볼 글자를 찾았다
Drill.pointer("right", j)  // 오른쪽에서 볼 글자를 찾았다
Drill.compare(i, j)        // 두 글자를 견줬다
""",
    constraints="""
- `0 <= text.length <= 200_000`
- 임의의 유니코드 문자가 올 수 있다
""",
    signature=dict(name="isPalindrome", parameters=[("text", "STRING")], returns="INT"),
    groups=standard_groups(),
    reference=_palindrome,
    cases={
        "sample": [
            ("01", ["A man, a plan, a canal: Panama"]),
            ("02", ["race a car"]),
        ],
        "boundary": [
            ("01-empty", [""]),
            # 볼 글자가 하나도 없다.
            ("02-punctuation-only", [".,;: !?"]),
            ("03-single-char", ["a"]),
            ("04-case-differs", ["Aa"]),
            ("05-digits", ["12321"]),
            ("06-digits-not-palindrome", ["12345"]),
            # 탭과 쉼표. 프로토콜의 구분자가 값 안에 들어 있어도 안전해야 한다.
            ("07-contains-separators", ["ab\tc,cb\ta"]),
            # 비ASCII. UTF-8 이 왕복하는지 본다.
            ("08-non-ascii", ["다들 잠 좀 자다"]),
        ],
        "hidden": [
            ("01-long-palindrome", ["ab" * 500 + "ba" * 500]),
            ("02-almost", ["abcdefghij" * 50 + "x" + "jihgfedcba" * 50]),
            ("03-mixed-noise", ["No 'x' in Nixon"]),
            ("04-spaces", ["  a  b  a  "]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 양쪽에서 좁혀 온다.
//
// 볼 글자만 골라 새 문자열을 만들어도 되지만, 그러면 입력만큼의 메모리를 더 쓴다.
// 양쪽 포인터가 각자 다음 글자를 찾아 건너뛰면 추가 메모리 없이 끝난다.
fun isPalindrome(text: String): Int {
    var left = 0
    var right = text.length - 1

    while (left < right) {
        while (left < right && !text[left].isLetterOrDigit()) left += 1
        while (left < right && !text[right].isLetterOrDigit()) right -= 1
        if (left >= right) break

        Drill.pointer("left", left)
        Drill.pointer("right", right)
        Drill.compare(left, right)

        if (text[left].lowercaseChar() != text[right].lowercaseChar()) return 0
        left += 1
        right -= 1
    }
    return 1
}
""",
    mutants=[
        ("case-sensitive--rejects-mixed", "WRONG_BRANCH",
         "대소문자를 구분해 'Aa' 를 회문이 아니라고 본다.",
         """
fun isPalindrome(text: String): Int {
    var left = 0
    var right = text.length - 1
    while (left < right) {
        while (left < right && !text[left].isLetterOrDigit()) left += 1
        while (left < right && !text[right].isLetterOrDigit()) right -= 1
        if (left >= right) break
        if (text[left] != text[right]) return 0
        left += 1
        right -= 1
    }
    return 1
}
"""),
        ("keeps-punctuation--compares-all", "MISSING_EDGE_CASE",
         "문장 부호와 공백까지 비교한다.",
         """
fun isPalindrome(text: String): Int {
    var left = 0
    var right = text.length - 1
    while (left < right) {
        if (text[left].lowercaseChar() != text[right].lowercaseChar()) return 0
        left += 1
        right -= 1
    }
    return 1
}
"""),
        ("letters-only--drops-digits", "WRONG_BRANCH",
         "숫자를 볼 글자로 치지 않는다.",
         """
fun isPalindrome(text: String): Int {
    val kept = text.filter { it.isLetter() }.lowercase()
    return if (kept == kept.reversed()) 1 else 0
}
"""),
    ],
))


# --- 32. 공통 접두사 ---------------------------------------------------------

def _common_prefix(words):
    if not words:
        return ""
    shortest = min(words, key=len)
    for i, ch in enumerate(shortest):
        for word in words:
            if word[i] != ch:
                return shortest[:i]
    return shortest


PROBLEMS.append(Problem(
    id="longest-common-prefix",
    title="가장 긴 공통 접두사",
    summary="""
문자열 배열 `words` 가 주어진다. 모든 문자열이 공통으로 갖는 가장 긴 **접두사**를
반환한다. 없으면 빈 문자열이다.
""",
    notes="""
빈 문자열이 하나라도 있으면 공통 접두사도 빈 문자열이다.
""",
    drill_doc="""
Drill.visit(index, 0)   // 몇 번째 글자를 보고 있는지
Drill.compare(a, b)     // 두 단어의 같은 자리를 견줬다
""",
    constraints="""
- `1 <= words.size <= 10_000`
- `0 <= words[i].length <= 1_000`
""",
    signature=dict(name="commonPrefix", parameters=[("words", "STRING_ARRAY")],
                   returns="STRING"),
    groups=standard_groups(),
    reference=_common_prefix,
    cases={
        "sample": [
            ("01", [["flower", "flow", "flight"]]),
            ("02", [["dog", "racecar", "car"]]),
        ],
        "boundary": [
            ("01-single-word", [["alone"]]),
            # 빈 문자열이 섞여 있다. 답은 반드시 빈 문자열이다.
            ("02-empty-member", [["abc", "", "abd"]]),
            ("03-all-empty", [["", "", ""]]),
            # 하나가 다른 것의 접두사다.
            ("04-one-is-prefix", [["ab", "abc", "abcd"]]),
            ("05-all-same", [["same", "same", "same"]]),
            ("06-no-common", [["a", "b"]]),
            # 구분자가 값 안에 들어 있다.
            ("07-contains-separators", [["a,b\tc", "a,b\td"]]),
            ("08-non-ascii", [["문제해결", "문제집", "문자열"]]),
        ],
        "hidden": [
            ("01-long-prefix", [["x" * 500 + "a", "x" * 500 + "b"]]),
            ("02-many-words", [[f"prefix-{i}" for i in range(200)]]),
            ("03-differ-at-last", [["abcde", "abcdf"]]),
            ("04-shortest-is-answer", [["ab", "abcdef", "abzz"]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/).
//
// 가장 짧은 단어보다 긴 답은 나올 수 없다. 그것을 후보로 두고 글자마다 전부 견주면,
// 어긋나는 첫 자리가 곧 답의 끝이다.
fun commonPrefix(words: Array<String>): String {
    var shortest = words[0]
    for (word in words) if (word.length < shortest.length) shortest = word

    for (index in shortest.indices) {
        Drill.visit(index, 0)
        for (word in words) {
            Drill.compare(index, index)
            if (word[index] != shortest[index]) return shortest.substring(0, index)
        }
    }
    return shortest
}
""",
    mutants=[
        ("first-word-only--ignores-rest", "MISSING_EDGE_CASE",
         "첫 단어만 보고 답으로 삼는다.",
         """
fun commonPrefix(words: Array<String>): String = words[0]
"""),
        ("compares-first-two--ignores-others", "MISSING_EDGE_CASE",
         "앞의 두 단어만 견준다.",
         """
fun commonPrefix(words: Array<String>): String {
    if (words.size == 1) return words[0]
    val a = words[0]
    val b = words[1]
    var index = 0
    while (index < a.length && index < b.length && a[index] == b[index]) index += 1
    return a.substring(0, index)
}
"""),
        ("off-by-one--includes-mismatch", "OFF_BY_ONE",
         "어긋난 글자까지 답에 넣는다.",
         """
fun commonPrefix(words: Array<String>): String {
    var shortest = words[0]
    for (word in words) if (word.length < shortest.length) shortest = word
    for (index in shortest.indices) {
        for (word in words) {
            if (word[index] != shortest[index]) return shortest.substring(0, index + 1)
        }
    }
    return shortest
}
"""),
    ],
))


# --- 33. 단어 순서 뒤집기 ----------------------------------------------------

def _reverse_words(words):
    return [w[::-1] for w in reversed(words)]


PROBLEMS.append(Problem(
    id="reverse-words",
    title="단어 순서와 글자 뒤집기",
    summary="""
문자열 배열 `words` 가 주어진다. **배열의 순서를 뒤집고, 각 단어의 글자도 뒤집어**
반환한다.
""",
    notes="""
두 가지를 모두 뒤집어야 한다. 하나만 뒤집은 답은 짧은 예제에서 우연히 맞을 수 있다.
""",
    drill_doc="""
Drill.visit(index, 0)      // 단어를 봤다
Drill.swap(left, right)    // 자리를 바꿨다
""",
    constraints="""
- `1 <= words.size <= 10_000`
- `0 <= words[i].length <= 1_000`
""",
    signature=dict(name="reverseWords", parameters=[("words", "STRING_ARRAY")],
                   returns="STRING_ARRAY"),
    groups=standard_groups(),
    reference=_reverse_words,
    cases={
        "sample": [
            ("01", [["abc", "de", "f"]]),
            ("02", [["hello", "world"]]),
        ],
        "boundary": [
            ("01-single", [["solo"]]),
            # 배열 순서만 뒤집은 답과 글자만 뒤집은 답이 같아지는 함정을 피한다.
            ("02-single-palindrome", [["aba"]]),
            ("03-empty-string", [["", "ab"]]),
            ("04-all-empty", [["", ""]]),
            ("05-one-char-each", [["a", "b", "c"]]),
            ("06-contains-separators", [["a,b", "c\td"]]),
            ("07-non-ascii", [["문제", "해결"]]),
        ],
        "hidden": [
            ("01-long-words", [["x" * 200, "y" * 200]]),
            ("02-many-words", [[f"w{i}" for i in range(300)]]),
            ("03-mixed-lengths", [["a", "bb", "ccc", "dddd"]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/).
//
// 배열 순서와 글자 순서는 서로 다른 뒤집기다. 하나만 하면 원소가 하나이거나 단어가
// 전부 회문일 때만 우연히 맞는다.
fun reverseWords(words: Array<String>): Array<String> {
    val out = Array(words.size) { "" }
    for (index in words.indices) {
        Drill.visit(index, 0)
        val target = words.size - 1 - index
        Drill.swap(index, target)
        out[target] = words[index].reversed()
    }
    return out
}
""",
    mutants=[
        ("order-only--keeps-letters", "MISSING_EDGE_CASE",
         "배열 순서만 뒤집고 글자는 그대로 둔다.",
         """
fun reverseWords(words: Array<String>): Array<String> = words.reversedArray()
"""),
        ("letters-only--keeps-order", "MISSING_EDGE_CASE",
         "글자만 뒤집고 배열 순서는 그대로 둔다.",
         """
fun reverseWords(words: Array<String>): Array<String> =
    Array(words.size) { words[it].reversed() }
"""),
        ("off-by-one--drops-first", "OFF_BY_ONE",
         "첫 단어를 빠뜨린다.",
         """
fun reverseWords(words: Array<String>): Array<String> {
    val out = Array(words.size) { "" }
    for (index in 1 until words.size) {
        out[words.size - 1 - index] = words[index].reversed()
    }
    return out
}
"""),
    ],
))


# --- 34. 글자별 등장 횟수 ----------------------------------------------------

def _char_counts(text):
    counts = [0] * 26
    for ch in text.lower():
        if "a" <= ch <= "z":
            counts[ord(ch) - ord("a")] += 1
    return counts


PROBLEMS.append(Problem(
    id="count-letters",
    title="알파벳별 등장 횟수",
    summary="""
문자열 `text` 가 주어진다. `a` 부터 `z` 까지 각 글자가 몇 번 나오는지를 담은 **길이 26
배열**을 반환한다. 대소문자는 구분하지 않고, 알파벳이 아닌 문자는 세지 않는다.
""",
    drill_doc="""
Drill.visit(index, 0)      // 글자를 봤다
Drill.write(slot, count)   // 그 글자의 개수를 올렸다
""",
    constraints="""
- `0 <= text.length <= 200_000`
- 임의의 유니코드 문자가 올 수 있다
""",
    signature=dict(name="countLetters", parameters=[("text", "STRING")], returns="INT_ARRAY"),
    groups=standard_groups(),
    reference=_char_counts,
    cases={
        "sample": [
            ("01", ["abca"]),
            ("02", ["Hello, World!"]),
        ],
        "boundary": [
            ("01-empty", [""]),
            ("02-no-letters", ["12345 !?"]),
            ("03-all-upper", ["ABC"]),
            ("04-mixed-case", ["aAbBcC"]),
            # 알파벳이 아닌 문자가 섞여 있다. 인덱스를 그대로 쓰면 범위를 벗어난다.
            ("05-non-ascii", ["가나다abc라마바"]),
            ("06-contains-separators", ["a,b\tc"]),
            ("07-every-letter", ["abcdefghijklmnopqrstuvwxyz"]),
        ],
        "hidden": [
            ("01-repeated", ["ab" * 1000]),
            ("02-long-mixed", ["The quick brown fox jumps over the lazy dog" * 20]),
            ("03-only-z", ["z" * 500]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/).
//
// 알파벳인지 먼저 보고 나서 인덱스를 만든다. 순서를 바꾸면 한글이나 기호에서
// 배열 범위를 벗어난다 — 입력에 무엇이 올지는 우리가 정하지 않는다.
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)

    for (index in text.indices) {
        Drill.visit(index, 0)
        val lower = text[index].lowercaseChar()
        if (lower < 'a' || lower > 'z') continue

        val slot = lower - 'a'
        counts[slot] += 1
        Drill.write(slot, counts[slot])
    }
    return counts
}
""",
    mutants=[
        ("case-sensitive--misses-upper", "MISSING_EDGE_CASE",
         "소문자만 센다.",
         """
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)
    for (ch in text) {
        if (ch < 'a' || ch > 'z') continue
        counts[ch - 'a'] += 1
    }
    return counts
}
"""),
        ("counts-everything--wrong-slot", "WRONG_BRANCH",
         "알파벳이 아닌 문자도 26으로 나눈 나머지 자리에 센다.",
         """
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)
    for (ch in text) {
        val slot = (ch.lowercaseChar() - 'a')
        counts[((slot % 26) + 26) % 26] += 1
    }
    return counts
}
"""),
        ("wrong-size--returns-short", "MISSING_EDGE_CASE",
         "실제로 나온 글자만 담아 길이가 26이 아니다.",
         """
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)
    for (ch in text) {
        val lower = ch.lowercaseChar()
        if (lower < 'a' || lower > 'z') continue
        counts[lower - 'a'] += 1
    }
    return counts.filter { it > 0 }.toIntArray()
}
"""),
    ],
))


# --- 57. 같은 모양의 두 문자열 ------------------------------------------------------

def _isomorphic(first, second):
    if len(first) != len(second):
        return 0
    forward = {}
    backward = {}
    for a, b in zip(first, second):
        if forward.get(a, b) != b or backward.get(b, a) != a:
            return 0
        forward[a] = b
        backward[b] = a
    return 1


PROBLEMS.append(Problem(
    id="isomorphic-strings",
    title="같은 모양의 두 문자열",
    summary="""
두 문자열 `first` 와 `second` 가 주어진다. `first` 의 각 문자를 어떤 문자로 바꾸어
`second` 를 만들 수 있으면 두 문자열은 **같은 모양**이다. 단, 같은 문자는 늘 같은 문자로
바뀌어야 하고, **서로 다른 문자가 같은 문자로 바뀌어서는 안 된다.** 문자는 자기 자신으로
바뀔 수 있다.

같은 모양이면 `1`, 아니면 `0` 을 반환한다.

- `"egg"` 와 `"add"` 는 같은 모양이다 (e→a, g→d).
- `"foo"` 와 `"bar"` 는 아니다 (o 가 a 와 r 둘로 바뀌어야 한다).
- `"badc"` 와 `"baba"` 는 아니다 (d 와 c 가 둘 다 a 로 바뀐다).
""",
    notes="""
한 방향의 대응만 기억하면 "서로 다른 문자가 같은 문자로"를 놓친다. 두 방향을 다 기억하거나,
각 자리에서 "그 문자가 처음 나온 위치"가 두 문자열에서 같은지를 본다.
""",
    drill_doc="""
Drill.visit(i, 0)             // i 번째 자리를 봤다
Drill.match(i, 1)             // 대응이 맞았다
""",
    constraints="""
- `0 <= first.length, second.length <= 100_000`
- 임의의 유니코드 문자가 올 수 있다
""",
    signature=dict(name="isomorphic", parameters=[("first", "STRING"), ("second", "STRING")],
                   returns="INT"),
    groups=standard_groups(),
    reference=_isomorphic,
    cases={
        "sample": [
            ("01", ["egg", "add"]),
            ("02", ["foo", "bar"]),
        ],
        "boundary": [
            ("01-both-empty", ["", ""]),
            # 길이가 다르면 모양이 같을 수 없다.
            ("02-different-length", ["ab", "abc"]),
            ("03-same-string", ["paper", "paper"]),
            # 서로 다른 문자가 같은 문자로. 한 방향만 보면 놓친다.
            ("04-two-to-one", ["badc", "baba"]),
            ("05-one-to-two", ["baba", "badc"]),
            ("06-single-char", ["a", "z"]),
            # 탭과 쉼표. 프로토콜의 구분자가 값 안에 들어 있어도 안전해야 한다.
            ("07-tab-and-comma", ["a\tb,c", "x\ty,z"]),
            # 비ASCII. 문자 단위로 대응해야 한다.
            ("08-non-ascii", ["한글한", "abca"]),
            ("09-non-ascii-match", ["한글한", "aba"]),
        ],
        "hidden": [
            ("01-long-match", ["abcabcabc" * 100, "xyzxyzxyz" * 100]),
            ("02-long-mismatch-late", ["ab" * 500 + "a", "cd" * 500 + "d"]),
            ("03-identity-mapping", ["abcdefg", "abcdefg"]),
            ("04-swap", ["abab", "baba"]),
            ("05-digits-and-letters", ["a1b2", "x9y8"]),
            ("06-two-to-one-late", ["abcdefghij", "abcdefghii"]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 두 방향의 대응을 함께 기억한다.
fun isomorphic(first: String, second: String): Int {
    if (first.length != second.length) return 0
    val forward = HashMap<Char, Char>()
    val backward = HashMap<Char, Char>()
    for (i in first.indices) {
        val a = first[i]
        val b = second[i]
        Drill.visit(i, 0)
        val f = forward[a]
        val g = backward[b]
        if ((f != null && f != b) || (g != null && g != a)) return 0
        forward[a] = b
        backward[b] = a
        Drill.match(i, 1)
    }
    return 1
}
""",
    mutants=[
        ("one-direction-only", "MISSING_EDGE_CASE",
         "first 에서 second 로의 대응만 기억한다. 서로 다른 문자가 같은 문자로 바뀌는 것을 놓친다.",
         """
fun isomorphic(first: String, second: String): Int {
    if (first.length != second.length) return 0
    val forward = HashMap<Char, Char>()
    for (i in first.indices) {
        val f = forward[first[i]]
        if (f != null && f != second[i]) return 0
        forward[first[i]] = second[i]
    }
    return 1
}
"""),
        ("ignores-length", "MISSING_EDGE_CASE",
         "길이가 다른 것을 확인하지 않는다. 짧은 쪽까지만 맞으면 같다고 본다.",
         """
fun isomorphic(first: String, second: String): Int {
    val forward = HashMap<Char, Char>()
    val backward = HashMap<Char, Char>()
    for (i in 0 until minOf(first.length, second.length)) {
        val a = first[i]; val b = second[i]
        val f = forward[a]; val g = backward[b]
        if ((f != null && f != b) || (g != null && g != a)) return 0
        forward[a] = b; backward[b] = a
    }
    return 1
}
"""),
        ("distinct-count--not-structure", "WRONG_ALGORITHM",
         "서로 다른 문자의 개수만 비교한다. 개수가 같아도 자리가 다르면 다른 모양이다.",
         """
fun isomorphic(first: String, second: String): Int {
    if (first.length != second.length) return 0
    return if (first.toSet().size == second.toSet().size) 1 else 0
}
"""),
    ],
))


# --- 65. 같은 글자가 없는 가장 긴 부분 문자열 ----------------------------------------------

def _longest_unique(text):
    last = {}
    start = 0
    best = 0
    for i, ch in enumerate(text):
        if ch in last and last[ch] >= start:
            start = last[ch] + 1
        last[ch] = i
        best = max(best, i - start + 1)
    return best


def _distinct_run(count):
    """서로 다른 BMP 글자 count 개. 대리쌍 앞에서 멈춘다 — 코틀린의 Char 와 파이썬의 글자가 같아야 한다."""
    assert count <= 0xD7FF - 0x100
    return "".join(chr(0x100 + i) for i in range(count))


def _text_of(count, alphabet, salt):
    picks = randoms(count, 0, len(alphabet) - 1, salt=salt)
    return "".join(alphabet[p] for p in picks)


PROBLEMS.append(Problem(
    id="longest-unique-substring",
    title="같은 글자가 없는 가장 긴 부분 문자열",
    summary="""
문자열 `text` 가 주어진다. **같은 글자가 두 번 나오지 않는** 연속 부분 문자열 중 가장 긴
것의 길이를 반환한다. 빈 문자열의 답은 `0` 이다.

예: `"abcabcbb"` 는 `"abc"` 로 `3`, `"bbbbb"` 는 `1`, `"pwwkew"` 는 `"wke"` 로 `3` 이다.
""",
    notes="""
창의 왼쪽 끝과 오른쪽 끝을 둔다. 오른쪽 글자가 창 안에 이미 있으면 왼쪽 끝을 그 글자의
**다음 자리**까지 당긴다 — 당기는 것이지 되돌리는 것이 아니다. 글자가 마지막으로 나온
자리를 기억해 두면 창 안에 있는지가 비교 한 번이다.
""",
    drill_doc="""
Drill.pointer("start", start) // 창의 왼쪽 끝
Drill.visit(i, best)          // 오른쪽 끝을 옮기고 본 최선
""",
    constraints="""
- `0 <= text.length <= 100_000`
- 임의의 유니코드 문자가 올 수 있다 (기본 다국어 평면 안)
""",
    signature=dict(name="longestUnique", parameters=[("text", "STRING")], returns="INT"),
    groups=perf_groups(),
    reference=_longest_unique,
    cases={
        "sample": [
            ("01", ["abcabcbb"]),
            ("02", ["pwwkew"]),
        ],
        "boundary": [
            ("01-empty", [""]),
            ("02-single", ["a"]),
            ("03-all-same", ["bbbbb"]),
            ("04-all-unique", ["abcdef"]),
            # 창 밖에 있던 글자를 다시 만난다. 왼쪽 끝을 뒤로 되돌리면 안 된다.
            ("05-stale-last-seen", ["abba"]),
            # 탭과 쉼표가 글자다.
            ("06-tab-and-comma", ["a\tb,a\t"]),
            ("07-non-ascii", ["한글한글글"]),
            ("08-space", ["a b a"]),
        ],
        "hidden": [
            ("01-random-small", [_text_of(40, "abcd", salt=2501)]),
            ("02-random-medium", [_text_of(3000, "abcdefgh", salt=2502)]),
            ("03-long-unique-tail", ["aaaa" + "bcdefghijklmnop"]),
            ("04-periodic", ["abcde" * 100]),
        ],
        "performance": [
            # 서로 다른 글자가 길게 이어져야 시작점마다 끝까지 훑는 풀이가 진다. 알파벳이
            # 작으면 창이 알파벳 크기에서 멈춰 O(n²) 도 빠르다.
            ("01-small", [_distinct_run(5000)]),
            ("02-medium", [_distinct_run(20000)]),
            ("03-large", [_distinct_run(55000)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 슬라이딩 윈도우 + 마지막 위치.
fun longestUnique(text: String): Int {
    val last = HashMap<Char, Int>()
    var start = 0
    var best = 0
    for (i in text.indices) {
        val seen = last[text[i]]
        if (seen != null && seen >= start) {
            start = seen + 1
            Drill.pointer("start", start)
        }
        last[text[i]] = i
        best = maxOf(best, i - start + 1)
        Drill.visit(i, best)
    }
    return best
}
""",
    mutants=[
        ("moves-start-backward", "WRONG_BRANCH",
         "창 밖에 있던 글자를 다시 만나도 왼쪽 끝을 그 자리로 되돌린다. 창이 다시 넓어진다.",
         """
fun longestUnique(text: String): Int {
    val last = HashMap<Char, Int>()
    var start = 0
    var best = 0
    for (i in text.indices) {
        val seen = last[text[i]]
        if (seen != null) start = seen + 1
        last[text[i]] = i
        best = maxOf(best, i - start + 1)
    }
    return best
}
"""),
        ("start-at-repeat--off-by-one", "OFF_BY_ONE",
         "왼쪽 끝을 반복된 글자의 다음이 아니라 그 자리로 당긴다. 같은 글자 둘이 창에 남는다.",
         """
fun longestUnique(text: String): Int {
    val last = HashMap<Char, Int>()
    var start = 0
    var best = 0
    for (i in text.indices) {
        val seen = last[text[i]]
        if (seen != null && seen >= start) start = seen
        last[text[i]] = i
        best = maxOf(best, i - start + 1)
    }
    return best
}
"""),
        ("distinct-count--not-window", "WRONG_ALGORITHM",
         "서로 다른 글자의 수를 답한다. 연속이라는 조건을 잊었다.",
         """
fun longestUnique(text: String): Int = text.toSet().size
"""),
        ("all-substrings--quadratic", "PERFORMANCE",
         "시작점마다 집합을 새로 만들어 늘려 간다. O(n²).",
         """
fun longestUnique(text: String): Int {
    var best = 0
    for (i in text.indices) {
        val seen = HashSet<Char>()
        var j = i
        while (j < text.length && seen.add(text[j])) { Drill.compare(i, j); j += 1 }
        best = maxOf(best, j - i)
    }
    return best
}
"""),
    ],
))


# --- 78. 애너그램 묶음의 수 ----------------------------------------------------------

def _anagram_groups(words):
    return len({"".join(sorted(word)) for word in words})


PROBLEMS.append(Problem(
    id="anagram-groups",
    title="애너그램 묶음의 수",
    summary="""
단어 배열 `words` 가 주어진다. 글자를 재배열해 서로 같아지는 단어들은 **한 묶음**이다.
묶음의 수를 반환한다. 대소문자는 구분하고, 같은 단어가 여러 번 나와도 한 묶음이다. 빈
배열의 답은 `0` 이다.

예: `["eat", "tea", "tan", "ate", "nat", "bat"]` 은 `{eat, tea, ate}`, `{tan, nat}`, `{bat}` 으로 `3` 이다.
""",
    notes="""
묶음의 이름표가 필요하다 — 글자를 정렬한 문자열이 그것이다. 같은 묶음이면 이름표가 같고
다른 묶음이면 다르다. 이름표를 집합에 넣으면 그 크기가 답이다.
""",
    drill_doc="""
Drill.visit(i, 0)             // 단어를 봤다
Drill.write(0, groups)        // 새 묶음이 생겼다
""",
    constraints="""
- `0 <= words.size <= 20_000`
- 각 단어의 길이는 `0` 이상 `100` 이하, 임의의 유니코드 문자
""",
    signature=dict(name="anagramGroups", parameters=[("words", "STRING_ARRAY")], returns="INT"),
    groups=standard_groups(),
    reference=_anagram_groups,
    cases={
        "sample": [
            ("01", [["eat", "tea", "tan", "ate", "nat", "bat"]]),
            ("02", [["a"]]),
        ],
        "boundary": [
            ("01-empty", [[]]),
            # 빈 단어끼리는 한 묶음이다.
            ("02-empty-words", [["", ""]]),
            ("03-duplicates", [["abc", "abc", "cab"]]),
            # 대소문자는 다르다.
            ("04-case", [["Abc", "abc"]]),
            # 글자 수가 다르면 다른 묶음이다 — 글자 집합만 보면 틀린다.
            ("05-multiset", [["aab", "abb", "aba"]]),
            ("06-non-ascii", [["한글", "글한", "한글글"]]),
            ("07-separators", [["a,b", "b,a", "a\tb"]]),
        ],
        "hidden": [
            ("01-all-same-group", [["abc", "acb", "bac", "bca", "cab", "cba"]]),
            ("02-all-different", [["a", "b", "c", "d"]]),
            ("03-random", [[chr(97 + (v % 3)) + chr(97 + (v // 3 % 3)) + chr(97 + (v // 9 % 3)) for v in randoms(500, 0, 26, salt=4101)]]),
            ("04-long-words", [["x" * 100, "x" * 99 + "y", "y" + "x" * 99]]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 글자를 정렬한 문자열이 묶음의 이름표다.
fun anagramGroups(words: Array<String>): Int {
    val labels = HashSet<String>()
    for ((i, word) in words.withIndex()) {
        Drill.visit(i, 0)
        if (labels.add(String(word.toCharArray().sortedArray()))) Drill.write(0, labels.size)
    }
    return labels.size
}
""",
    mutants=[
        ("char-set--ignores-counts", "WRONG_ALGORITHM",
         "글자의 집합을 이름표로 쓴다. 같은 글자가 몇 번인지 잊는다.",
         """
fun anagramGroups(words: Array<String>): Int = words.map { it.toSet() }.toSet().size
"""),
        ("case-insensitive", "MISSING_EDGE_CASE",
         "대소문자를 같게 본다.",
         """
fun anagramGroups(words: Array<String>): Int =
    words.map { String(it.lowercase().toCharArray().sortedArray()) }.toSet().size
"""),
        ("counts-words--not-groups", "WRONG_BRANCH",
         "서로 다른 단어의 수를 답한다. 재배열해 같아지는 것을 묶지 않는다.",
         """
fun anagramGroups(words: Array<String>): Int = words.toSet().size
"""),
    ],
))


# --- 91. 버전 비교 (독해 문제) -------------------------------------------------------------

def _compare_versions(a, b):
    xs = [int(part) for part in a.split(".")]
    ys = [int(part) for part in b.split(".")]
    n = max(len(xs), len(ys))
    xs += [0] * (n - len(xs))
    ys += [0] * (n - len(ys))
    for x, y in zip(xs, ys):
        if x != y:
            return -1 if x < y else 1
    return 0


PROBLEMS.append(Problem(
    id="compare-versions",
    title="버전 비교",
    summary="""
버전 문자열 `a`, `b` 가 주어진다. 버전은 점으로 나뉜 **십진 정수**들이다 — `1.0`, `1.2.10`,
`0.1` 처럼. 각 자리를 **숫자로** 앞에서부터 비교하고, 짧은 쪽의 빠진 자리는 `0` 으로 본다.

`a < b` 면 `-1`, `a > b` 면 `1`, 같으면 `0` 을 반환한다.

예: `1.0` 과 `1.0.0` 은 같다. `1.2.10` 은 `1.2.9` 보다 크다. `1.01` 과 `1.1` 은 같다.
""",
    notes="""
문자열로 비교하면 `10 < 9` 가 되고, 길이로 비교하면 `1.0 < 1.0.0` 이 된다. 자리마다 정수로
바꾼 뒤 비교하고, 한쪽이 먼저 끝나면 남은 자리를 0 으로 채운다 — 남은 자리가 `0.0` 이면
같은 것이고 `0.1` 이면 큰 것이다. 앞의 0 은 정수로 바꾸는 순간 사라진다.
""",
    drill_doc="""
Drill.compare(i, j)           // i 번째 자리를 비교했다
""",
    constraints="""
- `1 <= a.length, b.length <= 500`
- 자리는 `1` 개 이상이고, 각 자리는 `0` 이상 `2^31 - 1` 이하의 십진수 (앞에 0 이 붙을 수 있다)
""",
    signature=dict(name="compareVersions", parameters=[("a", "STRING"), ("b", "STRING")], returns="INT"),
    groups=standard_groups(),
    reference=_compare_versions,
    cases={
        "sample": [
            ("01", ["1.2.10", "1.2.9"]),
            ("02", ["1.0", "1.0.0"]),
        ],
        "boundary": [
            # 앞의 0. 숫자로는 같다.
            ("01-leading-zeros", ["1.01", "1.1"]),
            # 빠진 자리가 0 이 아니면 긴 쪽이 크다.
            ("02-longer-is-greater", ["1.0", "1.0.1"]),
            ("03-shorter-is-greater", ["2", "1.9.9.9"]),
            # 문자열로 비교하면 "10" < "9" 다.
            ("04-numeric-not-lexical", ["1.10", "1.9"]),
            ("05-equal-single", ["7", "7"]),
            # 자리가 Int 의 끝까지 간다.
            ("06-max-int-part", ["2147483647.0", "2147483646.999"]),
            ("07-many-zeros", ["1.0.0.0.0", "1"]),
            ("08-zero-vs-zero", ["0.0", "0"]),
        ],
        "hidden": [
            ("01-deep-equal", [".".join(str(v) for v in randoms(40, 0, 99, salt=4701)),
                               ".".join(str(v) for v in randoms(40, 0, 99, salt=4701))]),
            ("02-deep-differ-late", [".".join(str(v) for v in randoms(40, 0, 99, salt=4702)) + ".5",
                                     ".".join(str(v) for v in randoms(40, 0, 99, salt=4702)) + ".12"]),
            ("03-padded", ["03.0007.10", "3.7.10"]),
            ("04-long-tail-zero", ["4.2", "4.2.0.0.0.0.0.0.0.0.0.0"]),
            ("05-long-tail-nonzero", ["4.2", "4.2.0.0.0.0.0.0.0.0.0.1"]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 자리마다 정수로, 빠진 자리는 0.
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.')
    val ys = b.split('.')
    val n = maxOf(xs.size, ys.size)
    for (i in 0 until n) {
        val x = if (i < xs.size) xs[i].toLong() else 0L
        val y = if (i < ys.size) ys[i].toLong() else 0L
        Drill.compare(i, i)
        if (x != y) return if (x < y) -1 else 1
    }
    return 0
}
""",
    mutants=[
        ("lexical-parts", "WRONG_ALGORITHM",
         "자리를 문자열로 비교한다. 10 이 9 보다 작다.",
         """
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.'); val ys = b.split('.')
    val n = maxOf(xs.size, ys.size)
    for (i in 0 until n) {
        val x = if (i < xs.size) xs[i] else "0"
        val y = if (i < ys.size) ys[i] else "0"
        val c = x.compareTo(y)
        if (c != 0) return if (c < 0) -1 else 1
    }
    return 0
}
"""),
        ("longer-wins", "MISSING_EDGE_CASE",
         "공통 자리가 같으면 자리가 많은 쪽이 크다고 본다. 1.0 과 1.0.0 이 달라진다.",
         """
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.').map { it.toLong() }; val ys = b.split('.').map { it.toLong() }
    for (i in 0 until minOf(xs.size, ys.size)) if (xs[i] != ys[i]) return if (xs[i] < ys[i]) -1 else 1
    return xs.size.compareTo(ys.size).coerceIn(-1, 1)
}
"""),
        ("common-prefix-only", "MISSING_EDGE_CASE",
         "공통 자리만 비교하고 나머지는 무시한다. 1.0 과 1.0.1 이 같아진다.",
         """
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.').map { it.toLong() }; val ys = b.split('.').map { it.toLong() }
    for (i in 0 until minOf(xs.size, ys.size)) if (xs[i] != ys[i]) return if (xs[i] < ys[i]) -1 else 1
    return 0
}
"""),
        ("strips-one-trailing-zero", "MISSING_EDGE_CASE",
         "끝의 .0 을 한 번만 떼고 길이를 비교한다. 1.0.0 과 1 이 달라진다.",
         """
fun compareVersions(a: String, b: String): Int {
    val xs = a.removeSuffix(".0").split('.').map { it.toLong() }
    val ys = b.removeSuffix(".0").split('.').map { it.toLong() }
    for (i in 0 until minOf(xs.size, ys.size)) if (xs[i] != ys[i]) return if (xs[i] < ys[i]) -1 else 1
    return xs.size.compareTo(ys.size).coerceIn(-1, 1)
}
"""),
    ],
))


# --- 104. 모음의 수 (입문) ----------------------------------------------------------

def _count_vowels(text):
    return sum(1 for c in text if c in "aeiouAEIOU")


PROBLEMS.append(Problem(
    id="count-vowels",
    title="모음의 수",
    summary="""
문자열 `text` 에서 영문 모음 (`a, e, i, o, u` 와 대문자) 의 수를 반환한다. 다른 글자는
세지 않는다.

예: `"Hello, World"` → `3`.
""",
    notes="""
글자마다 모음인지 보면 된다. 대문자를 잊거나, `y` 를 세거나, 비영문 글자를 모음으로 착각하는
것이 갈림길이다.
""",
    drill_doc="""
Drill.visit(i, 0)             // 글자를 봤다
Drill.write(0, count)         // 지금까지의 수
""",
    constraints="""
- `0 <= text.length <= 200_000`, 임의의 유니코드 문자
""",
    signature=dict(name="countVowels", parameters=[("text", "STRING")], returns="INT"),
    groups=standard_groups(),
    reference=_count_vowels,
    cases={
        "sample": [("01", ["Hello, World"]), ("02", ["xyz"])],
        "boundary": [
            ("01-empty", [""]),
            ("02-all-vowels", ["aeiouAEIOU"]),
            # y 는 모음이 아니다.
            ("03-y", ["yyy"]),
            # 비영문 모음처럼 보이는 글자. 세지 않는다.
            ("04-accented", ["éàü"]),
            ("05-korean", ["아에이오우"]),
            ("06-separators", ["a,e\ti"]),
        ],
        "hidden": [
            ("01-long", ["ab" * 50000]),
            ("02-mixed", ["The quick brown fox jumps over the lazy dog"]),
            ("03-upper", ["AEIOU" * 1000 + "BCD" * 1000]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/).
fun countVowels(text: String): Int {
    var count = 0
    for ((i, c) in text.withIndex()) {
        Drill.visit(i, 0)
        if (c in "aeiouAEIOU") { count += 1; Drill.write(0, count) }
    }
    return count
}
""",
    mutants=[
        ("lowercase-only", "MISSING_EDGE_CASE", "소문자 모음만 센다.", """
fun countVowels(text: String): Int = text.count { it in "aeiou" }
"""),
        ("counts-y", "WRONG_BRANCH", "y 를 모음으로 센다.", """
fun countVowels(text: String): Int = text.count { it in "aeiouyAEIOUY" }
"""),
        ("counts-letters", "WRONG_ALGORITHM", "영문자 전부를 센다.", """
fun countVowels(text: String): Int = text.count { it in 'a'..'z' || it in 'A'..'Z' }
"""),
    ],
))


# --- 110. 가장 짧은 덮는 창 (글자 수 슬라이딩 윈도) ------------------------------------------------

def _min_window_length(s, t):
    from collections import Counter
    if not t or len(t) > len(s):
        return 0
    need = Counter(t)
    missing = len(t)
    best = 0
    left = 0
    for right, c in enumerate(s):
        if need[c] > 0:
            missing -= 1
        need[c] -= 1
        while missing == 0:
            length = right - left + 1
            if best == 0 or length < best:
                best = length
            need[s[left]] += 1
            if need[s[left]] > 0:
                missing += 1
            left += 1
    return best


PROBLEMS.append(Problem(
    id="min-window-substring",
    title="가장 짧은 덮는 창",
    summary="""
문자열 `s` 와 `t` 가 주어진다. `t` 의 **모든 글자를 개수까지** 포함하는 `s` 의 가장 짧은
연속 부분 문자열의 길이를 반환한다. 없으면 `0`. `t` 가 비면 `0` 이다.

예: `s = "ADOBECODEBANC"`, `t = "ABC"` → `"BANC"` 로 `4`. `t = "AA"` 면 `A` 가 둘 있어야 한다.
""",
    notes="""
글자별로 "아직 모자란 수"를 들고 창을 민다. 오른쪽을 늘려 모자란 것이 0 이 되면, 왼쪽을 줄여
가며 여전히 0 인 동안 길이를 잰다 — 왼쪽 글자를 빼서 모자라지면 멈춘다. 양쪽 끝이 각각 n
번만 움직인다. 글자마다 개수를 세지 않고 "있는가"만 보면 `AA` 를 놓친다.
""",
    drill_doc="""
Drill.pointer("left", i)      // 창의 왼쪽
Drill.pointer("right", j)     // 창의 오른쪽
Drill.write(0, best)          // 지금까지의 최소
""",
    constraints="""
- `0 <= s.length <= 200_000`, `0 <= t.length <= 200_000`, 임의의 유니코드 문자
""",
    signature=dict(name="minWindow", parameters=[("s", "STRING"), ("t", "STRING")], returns="INT"),
    groups=perf_groups(time_multiplier=0.5),
    reference=_min_window_length,
    cases={
        "sample": [("01", ["ADOBECODEBANC", "ABC"]), ("02", ["a", "a"])],
        "boundary": [
            ("01-empty-t", ["abc", ""]),
            ("02-t-longer", ["ab", "abc"]),
            # 개수까지 맞아야 한다.
            ("03-needs-two", ["aab", "aa"]),
            ("04-needs-two-missing", ["abcb", "aa"]),
            ("05-whole", ["abc", "cab"]),
            ("06-none", ["abc", "d"]),
            # 대소문자는 다르다.
            ("07-case", ["aA", "AA"]),
            ("08-non-ascii", ["가나다라나가", "가라"]),
        ],
        "hidden": [
            ("01-random", ["".join(chr(97 + v) for v in randoms(300, 0, 5, salt=7701)), "abc"]),
            ("02-random-counts", ["".join(chr(97 + v) for v in randoms(500, 0, 3, salt=7702)), "aabbc"]),
            ("03-answer-at-end", ["x" * 200 + "abc", "cba"]),
            ("04-answer-at-start", ["abc" + "x" * 200, "cab"]),
        ],
        "performance": [
            # z 가 맨 끝에만 있다. 시작점마다 세면 어디서 시작하든 끝까지 간다 — O(n²).
            ("01-small", ["".join(chr(97 + v) for v in randoms(20000, 0, 24, salt=7703)) + "z", "abcz"]),
            ("02-medium", ["".join(chr(97 + v) for v in randoms(100000, 0, 24, salt=7704)) + "z", "abcz"]),
            ("03-large", ["".join(chr(97 + v) for v in randoms(200000, 0, 24, salt=7705)) + "z", "abcz"]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 글자별 모자란 수를 들고 창을 민다.
fun minWindow(s: String, t: String): Int {
    if (t.isEmpty() || t.length > s.length) return 0
    val need = HashMap<Char, Int>()
    for (c in t) need[c] = (need[c] ?: 0) + 1
    var missing = t.length
    var best = 0
    var left = 0
    for (right in s.indices) {
        val c = s[right]
        val n = need[c] ?: 0
        if (n > 0) missing -= 1
        need[c] = n - 1
        Drill.pointer("right", right)
        while (missing == 0) {
            val length = right - left + 1
            if (best == 0 || length < best) { best = length; Drill.write(0, best) }
            val l = s[left]
            need[l] = (need[l] ?: 0) + 1
            if (need[l]!! > 0) missing += 1
            left += 1
            Drill.pointer("left", left)
        }
    }
    return best
}
""",
    mutants=[
        ("presence-only", "WRONG_ALGORITHM", "글자가 있는지만 본다. 개수를 잊는다.", """
fun minWindow(s: String, t: String): Int {
    if (t.isEmpty()) return 0
    val need = t.toSet()
    var best = 0
    for (i in s.indices) {
        val seen = HashSet<Char>()
        for (j in i until s.length) { if (s[j] in need) seen.add(s[j]); if (seen.size == need.size) { if (best == 0 || j - i + 1 < best) best = j - i + 1; break } }
    }
    return best
}
"""),
        ("shrinks-once", "WRONG_BRANCH", "창이 덮으면 왼쪽을 한 칸만 줄인다.", """
fun minWindow(s: String, t: String): Int {
    if (t.isEmpty() || t.length > s.length) return 0
    val need = HashMap<Char, Int>()
    for (c in t) need[c] = (need[c] ?: 0) + 1
    var missing = t.length; var best = 0; var left = 0
    for (right in s.indices) {
        val c = s[right]; val n = need[c] ?: 0
        if (n > 0) missing -= 1
        need[c] = n - 1
        if (missing == 0) {
            val length = right - left + 1
            if (best == 0 || length < best) best = length
            val l = s[left]; need[l] = (need[l] ?: 0) + 1; if (need[l]!! > 0) missing += 1; left += 1
        }
    }
    return best
}
"""),
        ("case-insensitive", "MISSING_EDGE_CASE", "대소문자를 같게 본다.", """
fun minWindow(s: String, t: String): Int {
    val ls = s.lowercase(); val lt = t.lowercase()
    if (lt.isEmpty() || lt.length > ls.length) return 0
    val need = HashMap<Char, Int>()
    for (c in lt) need[c] = (need[c] ?: 0) + 1
    var missing = lt.length; var best = 0; var left = 0
    for (right in ls.indices) {
        val c = ls[right]; val n = need[c] ?: 0
        if (n > 0) missing -= 1
        need[c] = n - 1
        while (missing == 0) {
            val length = right - left + 1
            if (best == 0 || length < best) best = length
            val l = ls[left]; need[l] = (need[l] ?: 0) + 1; if (need[l]!! > 0) missing += 1; left += 1
        }
    }
    return best
}
"""),
        ("recount-per-window--quadratic", "PERFORMANCE", "시작점마다 개수를 처음부터 세며 늘린다. O(n²).", """
fun minWindow(s: String, t: String): Int {
    if (t.isEmpty() || t.length > s.length) return 0
    val need = HashMap<Char, Int>()
    for (c in t) need[c] = (need[c] ?: 0) + 1
    var best = 0
    for (i in s.indices) {
        val have = HashMap<Char, Int>(); var covered = 0
        for (j in i until s.length) {
            Drill.compare(i, j)
            val c = s[j]; val h = (have[c] ?: 0) + 1; have[c] = h
            if (h <= (need[c] ?: 0)) covered += 1
            if (covered == t.length) { if (best == 0 || j - i + 1 < best) best = j - i + 1; break }
        }
    }
    return best
}
"""),
    ],
))


# --- 154. 제목 표기 (규칙 읽기) -------------------------------------------------------------------------

def _title_case(text):
    out = []
    start = True
    for ch in text:
        if ch == " ":
            out.append(ch)
            start = True
        elif start:
            out.append(ch.upper())
            start = False
        else:
            out.append(ch.lower())
    return "".join(out)


PROBLEMS.append(Problem(
    id="title-case",
    title="제목 표기",
    summary="""
영문자와 공백으로 된 문자열 `text` 를 **제목 표기**로 바꿔 반환한다. 단어의 첫 글자는 대문자,
나머지 글자는 소문자다. 단어는 공백으로 나뉘며, 공백은 개수와 자리를 **그대로** 둔다 — 앞뒤와
사이의 공백을 합치거나 지우지 않는다.
""",
    notes="""
규칙 셋을 그대로 옮기면 된다: 공백은 그대로, 단어의 첫 글자는 대문자, 그 뒤는 소문자. "단어의
첫 글자"는 문자열의 처음이거나 바로 앞이 공백인 글자다. `split` 으로 나누면 공백의 개수를
잃는다 — 글자를 하나씩 보며 "지금이 단어의 시작인가"를 들고 가는 것이 정확하다.
""",
    drill_doc="""
Drill.compare(i, start)       // 글자 i 가 단어의 시작인지 봤다
Drill.write(i, code)          // 글자를 적었다
""",
    constraints="""
- `0 <= text.length <= 100_000`, 영문자와 공백뿐
""",
    signature=dict(name="titleCase", parameters=[("text", "STRING")], returns="STRING"),
    groups=standard_groups(),
    reference=_title_case,
    limits={"timeMillis": 2000, "memoryMb": 256, "outputBytes": 400000},
    cases={
        "sample": [("01", ["hello world"]), ("02", ["tHE qUICK bROWN"])],
        "boundary": [
            ("01-empty", [""]),
            ("02-single-letter", ["a"]),
            # 앞뒤 공백과 사이의 여러 공백은 그대로.
            ("03-leading-and-double-spaces", ["  two  spaces "]),
            ("04-all-upper", ["ALL CAPS HERE"]),
            ("05-only-spaces", ["   "]),
            ("06-single-word-mixed", ["mIxEdCaSe"]),
        ],
        "hidden": [
            ("01-sentence", ["the rain in spain stays mainly in the plain"]),
            ("02-trailing-space", ["end with space "]),
            ("03-many-words", [" ".join(["wORD"] * 50)]),
            ("04-long", [("aBc dEf  gHi " * 5000).rstrip(" ")]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 글자마다 "단어의 시작인가"를 들고 간다.
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var start = true
    for ((i, ch) in text.withIndex()) {
        Drill.compare(i, if (start) 1 else 0)
        when {
            ch == ' ' -> { out.append(ch); start = true }
            start -> { out.append(ch.uppercaseChar()); start = false }
            else -> out.append(ch.lowercaseChar())
        }
    }
    return out.toString()
}
""",
    mutants=[
        ("split-joins-spaces", "WRONG_ALGORITHM",
         "공백으로 나눠 다시 합친다. 여러 공백과 앞뒤 공백이 사라진다.",
         """
fun titleCase(text: String): String =
    text.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
"""),
        ("keeps-rest-as-is", "MISSING_EDGE_CASE",
         "첫 글자만 대문자로 하고 나머지는 그대로 둔다. 소문자로 내려야 한다.",
         """
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var start = true
    for (ch in text) {
        when {
            ch == ' ' -> { out.append(ch); start = true }
            start -> { out.append(ch.uppercaseChar()); start = false }
            else -> out.append(ch)
        }
    }
    return out.toString()
}
"""),
        ("first-letter-only-once", "OFF_BY_ONE",
         "문자열의 첫 글자만 대문자로 한다. 둘째 단어부터는 소문자다.",
         """
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    for ((i, ch) in text.withIndex()) out.append(if (i == 0) ch.uppercaseChar() else ch.lowercaseChar())
    return out.toString()
}
"""),
        ("space-does-not-reset", "WRONG_BRANCH",
         "공백을 지나도 단어의 시작으로 돌아가지 않는다.",
         """
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var start = true
    for (ch in text) {
        when {
            ch == ' ' -> out.append(ch)
            start -> { out.append(ch.uppercaseChar()); start = false }
            else -> out.append(ch.lowercaseChar())
        }
    }
    return out.toString()
}
"""),
    ],
))


# --- 155. 로마 숫자 읽기 (규칙 읽기) -------------------------------------------------------------------

_ROMAN = {"I": 1, "V": 5, "X": 10, "L": 50, "C": 100, "D": 500, "M": 1000}


def _roman_to_integer(text):
    total = 0
    for i, ch in enumerate(text):
        value = _ROMAN[ch]
        if i + 1 < len(text) and _ROMAN[text[i + 1]] > value:
            total -= value
        else:
            total += value
    return total


def _integer_to_roman(n):
    out = []
    for value, symbol in ((1000, "M"), (900, "CM"), (500, "D"), (400, "CD"), (100, "C"), (90, "XC"), (50, "L"), (40, "XL"), (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I")):
        while n >= value:
            out.append(symbol)
            n -= value
    return "".join(out)


PROBLEMS.append(Problem(
    id="roman-to-integer",
    title="로마 숫자 읽기",
    summary="""
올바른 로마 숫자 문자열 `text` 를 정수로 바꿔 반환한다. 기호는 `I=1, V=5, X=10, L=50, C=100,
D=500, M=1000` 이고 보통은 큰 것부터 왼쪽에서 오른쪽으로 더한다. 다만 **작은 기호가 큰 기호
바로 앞에 오면 그 작은 값은 빼는** 것이다 — `IV = 4`, `IX = 9`, `XL = 40`, `XC = 90`, `CD = 400`,
`CM = 900`. 입력은 그 규칙을 지키는 문자열이다.
""",
    notes="""
규칙 하나면 된다: 기호마다 **다음 기호가 더 크면 빼고, 아니면 더한다.** 여섯 가지 조합을 따로
외울 필요가 없다 — 그 조합은 규칙에서 나온다. 마지막 기호는 다음이 없으니 늘 더한다.
""",
    drill_doc="""
Drill.compare(i, i + 1)       // 이웃 기호와 크기를 비교했다
Drill.write(0, total)         // 합을 갱신했다
""",
    constraints="""
- `1 <= text.length <= 15`, 올바른 로마 숫자, 값은 `1..3999`
""",
    signature=dict(name="romanToInteger", parameters=[("text", "STRING")], returns="INT"),
    groups=standard_groups(),
    reference=_roman_to_integer,
    cases={
        "sample": [("01", ["III"]), ("02", ["LVIII"]), ("03", ["MCMXCIV"])],
        "boundary": [
            ("01-one", ["I"]),
            ("02-four", ["IV"]),
            ("03-nine", ["IX"]),
            ("04-max", ["MMMCMXCIX"]),
            # 빼는 쌍이 연달아 온다.
            ("05-consecutive-subtractions", ["XCIV"]),
            ("06-thousand", ["M"]),
            ("07-forty", ["XL"]),
        ],
        "hidden": [
            ("01-random", [_integer_to_roman(1994)]),
            ("02-random", [_integer_to_roman(2468)]),
            ("03-random", [_integer_to_roman(3888)]),
            ("04-random", [_integer_to_roman(444)]),
            ("05-random", [_integer_to_roman(999)]),
            ("06-random", [_integer_to_roman(1049)]),
        ],
    },
    kotlin="""
// 검증용 정답 (§6.1 solutions/). 다음 기호가 더 크면 빼고, 아니면 더한다.
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        if (i + 1 < text.length) Drill.compare(i, i + 1)
        total += if (i + 1 < text.length && value(text[i + 1]) > v) -v else v
        Drill.write(0, total)
    }
    return total
}
""",
    mutants=[
        ("adds-everything", "WRONG_ALGORITHM",
         "빼는 규칙이 없다. IV 가 6 이 된다.",
         """
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    return text.sumOf { value(it) }
}
"""),
        ("subtracts-when-next-is-equal-or-larger", "OFF_BY_ONE",
         "다음 기호가 같아도 뺀다. III 가 1 이 된다.",
         """
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        total += if (i + 1 < text.length && value(text[i + 1]) >= v) -v else v
    }
    return total
}
"""),
        ("only-i-subtracts", "MISSING_EDGE_CASE",
         "I 앞에서만 빼는 규칙을 적용한다. XL·XC·CD·CM 을 놓친다.",
         """
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        total += if (text[i] == 'I' && i + 1 < text.length && value(text[i + 1]) > v) -v else v
    }
    return total
}
"""),
        ("compares-with-previous", "WRONG_BRANCH",
         "이전 기호와 비교해 뺀다. 빼야 할 것은 앞의 작은 기호인데 뒤의 큰 기호를 뺀다.",
         """
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        total += if (i > 0 && value(text[i - 1]) < v) -v else v
    }
    return total
}
"""),
    ],
))

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

from author import Problem, standard_groups

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

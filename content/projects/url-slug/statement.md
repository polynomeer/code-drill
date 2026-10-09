# URL 슬러그

글 제목으로 `/blog/cafe-creme-recipe` 같은 주소의 마지막 조각(슬러그)을 만든다. `src/slug/Slugs.java` 의 두 정적 메서드를
완성한다.

## `slugify(String title, int maxLength)`

1. **악센트를 뗀다.** 유니코드 NFD 로 분해한 뒤 결합 부호를 버린다 — `é` 는 `e`, `Ñ` 는 `N` 이 된다
   (`java.text.Normalizer`).
2. **아포스트로피는 지운다.** `'` 와 `’`(U+2019)는 낱말을 가르지 않는다: `Don't` → `dont`.
3. **ASCII 영숫자만 남긴다.** `A`~`Z` 는 소문자로 바꾸고, `a`~`z`·`0`~`9` 는 그대로 둔다. 그 밖의 글자(공백, 문장부호,
   한글, 이모지…)가 **한 번 이상 이어진 덩어리는 대시 하나**가 된다. 슬러그는 대시로 시작하거나 끝나지 않는다.
4. **길이를 넘으면 낱말 경계에서 자른다.** 슬러그가 `maxLength` 보다 길면 앞의 `maxLength` 글자만 남기는데,
   - 자른 자리가 낱말 한가운데면 그 낱말을 통째로 버린다: `big-cat-dog`, 6 → `big`.
   - 자른 자리가 딱 낱말 끝이면 그대로다: `big-cat-dog`, 7 → `big-cat`.
   - 남는 낱말이 하나뿐이라 버릴 수 없으면 그 자리에서 자른다: `supercalifragilistic`, 9 → `supercali`.
5. **남는 것이 없으면 `untitled`** 다(`maxLength` 와 상관없이).

`maxLength` 가 1 보다 작으면 `IllegalArgumentException`.

## `unique(String slug, Set<String> taken)`

`slug` 가 `taken` 에 없으면 그대로 돌려준다. 있으면 `slug-2`, `slug-3`, … 가운데 `taken` 에 없는 **가장 작은 번호**를
붙여 돌려준다.

## 제출

`src/slug/Slugs.java` 를 고친다(다른 파일을 더해도 된다). `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import static codedrill.Assertions.*;` 의 단언으로 쓴다 — 표준 라이브러리 외의 의존성은 없다.

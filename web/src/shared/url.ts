/**
 * 주소창의 쿼리 문자열 (PRD FR-201 필터 보존, §9.1 공유 가능한 링크).
 *
 * 화면 두 곳이 각자 주소를 쓰고 있었다. 문제 목록은 필터를, App 은 열어 둔 제출을
 * 적는데, 둘 다 `replaceState` 로 **통째로 갈아 끼우고** 있어서 나중에 쓴 쪽이 앞의 것을
 * 지웠다 — 필터를 걸고 제출을 열면 필터가 사라졌다.
 *
 * 그래서 여기 모은다. 각자 자기 키만 건드리고 나머지는 그대로 둔다.
 *
 * `pushState` 가 아니라 `replaceState` 를 쓴다. 체크박스 하나 켤 때마다 히스토리가
 * 쌓이면 뒤로가기가 쓸모없어진다.
 */

function commit(params: URLSearchParams): void {
  const search = params.toString()
  window.history.replaceState(null, '', search ? `?${search}` : window.location.pathname)
}

/** 값 하나짜리 키를 바꾼다. `null` 이면 지운다. */
export function setParam(key: string, value: string | null): void {
  const params = new URLSearchParams(window.location.search)
  if (value === null) params.delete(key)
  else params.set(key, value)
  commit(params)
}

/**
 * 여러 값을 갖는 키 묶음을 통째로 갈아 끼운다.
 *
 * [owned] 에 적힌 키만 지우고 [next] 로 다시 채운다. 지울 목록을 따로 받는 이유는,
 * 값이 하나도 없는 필터도 **꺼졌다는 사실을 반영해야** 하기 때문이다 — [next] 에
 * 없는 키를 그냥 두면 필터를 끈 것이 주소에 남는다.
 */
export function replaceParams(next: URLSearchParams, owned: string[]): void {
  const params = new URLSearchParams(window.location.search)
  for (const key of owned) params.delete(key)
  for (const [key, value] of next) params.append(key, value)
  commit(params)
}

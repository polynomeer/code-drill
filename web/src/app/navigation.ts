/**
 * 로그인 뒤 돌아갈 곳 (`/login?next=`).
 *
 * 우리 사이트 안의 경로만 받는다. `//evil.example` 이나 `https://…` 를 그대로 따라가면 로그인
 * 화면이 남의 사이트로 가는 문이 된다 (open redirect). 아니면 홈으로 간다.
 */
export function safeNext(raw: string | null): string {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || raw.startsWith('/\\')) return '/'
  return raw
}

/**
 * 사용자 코드를 맞춘다 — 서버의 `DeviceAuthorization.normalize` 와 같은 규칙. 소문자·공백·빠진 하이픈을 받아 주고,
 * 쓰지 않는 글자(모음, 숫자)가 있으면 null — 서버에 묻기 전에 오타를 잡는다.
 */
const ALPHABET = 'BCDFGHJKLMNPQRSTVWXZ'

export function normalizeUserCode(raw: string): string | null {
  const letters = raw.toUpperCase().replace(/[^A-Z0-9]/g, '')
  if (letters.length !== 8 || [...letters].some((c) => !ALPHABET.includes(c))) return null
  return `${letters.slice(0, 4)}-${letters.slice(4)}`
}

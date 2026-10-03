/**
 * 하네스의 전송 형식을 화면의 값으로 되돌린다 (docs/project-context.md "문제 패키지 값 타입").
 *
 * 채점기는 실제 출력을 비교용 형식 그대로 싣는다 — 정수 배열은 `1,2,3`, 문자열은 Base64,
 * 문자열 배열은 개수를 앞에 둔 Base64 목록, 격자는 `<행>,<열>,<원소…>`. 그대로 보이면 사용자는
 * `[1,2,3]` 을 기대했는데 `1,2,3` 이 나왔다고 읽고, 문자열 문제에서는 Base64 를 본다.
 *
 * 어떤 타입인지는 **시그니처의 반환형이 정한다** (서버의 규칙과 같다 — 모양에서 추론하지 않는다).
 * 되돌리지 못하면 원문을 그대로 돌려준다. 사용자 코드가 엉뚱한 값을 냈다는 것 자체가 정보다.
 */
export type ReturnKind = 'INT' | 'INT_ARRAY' | 'STRING' | 'STRING_ARRAY' | 'INT_MATRIX'

/** `fun f(…): IntArray` → INT_ARRAY. 문제 상세의 시그니처는 늘 코틀린 표기다. */
export function returnKind(signature: string): ReturnKind | null {
  const type = signature.slice(signature.lastIndexOf(')') + 1).replace(':', '').trim()
  switch (type) {
    case 'Int':
      return 'INT'
    case 'IntArray':
      return 'INT_ARRAY'
    case 'String':
      return 'STRING'
    case 'Array<String>':
      return 'STRING_ARRAY'
    case 'Array<IntArray>':
      return 'INT_MATRIX'
    default:
      return null
  }
}

export type Decoded = { ok: true; value: unknown } | { ok: false; raw: string }

export function decodeWire(kind: ReturnKind | null, raw: string): Decoded {
  try {
    switch (kind) {
      case 'INT':
        return int(raw)
      case 'INT_ARRAY':
        return { ok: true, value: raw === '' ? [] : raw.split(',').map(strictInt) }
      case 'STRING':
        return { ok: true, value: base64(raw) }
      case 'STRING_ARRAY': {
        const [count, ...items] = raw.split(',')
        const size = strictInt(count ?? '')
        // 빈 문자열 하나짜리 배열은 `1,` 이다 — split 이 빈 원소를 남기므로 개수와 맞춰 본다.
        const values = size === 0 ? [] : items
        if (values.length !== size) return { ok: false, raw }
        return { ok: true, value: values.map(base64) }
      }
      case 'INT_MATRIX': {
        const [rows, columns, ...cells] = raw.split(',')
        const r = strictInt(rows ?? '')
        const c = strictInt(columns ?? '')
        const values = r * c === 0 ? [] : cells.map(strictInt)
        if (values.length !== r * c) return { ok: false, raw }
        return { ok: true, value: Array.from({ length: r }, (_, i) => values.slice(i * c, (i + 1) * c)) }
      }
      default:
        return { ok: false, raw }
    }
  } catch {
    return { ok: false, raw }
  }
}

/** 화면에 보일 글. 되돌리지 못했으면 원문. */
export function showWire(kind: ReturnKind | null, raw: string): string {
  const decoded = decodeWire(kind, raw)
  return decoded.ok ? JSON.stringify(decoded.value) : raw
}

function int(raw: string): Decoded {
  return { ok: true, value: strictInt(raw) }
}

function strictInt(text: string): number {
  if (!/^-?\d+$/.test(text.trim())) throw new Error(`정수가 아니다: ${text}`)
  return Number(text)
}

function base64(text: string): string {
  const binary = atob(text)
  const bytes = Uint8Array.from(binary, (ch) => ch.charCodeAt(0))
  return new TextDecoder('utf-8', { fatal: true }).decode(bytes)
}

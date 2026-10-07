import { describe, expect, it } from 'vitest'
import { normalizeUserCode } from './deviceCode'

describe('normalizeUserCode', () => {
  it('소문자·공백·하이픈 없는 것을 받아 준다', () => {
    expect(normalizeUserCode(' wqxr kdpb ')).toBe('WQXR-KDPB')
    expect(normalizeUserCode('WQXRKDPB')).toBe('WQXR-KDPB')
  })

  it('길이가 틀리거나 쓰지 않는 글자가 있으면 null', () => {
    expect(normalizeUserCode('WQXR-KDP')).toBeNull()
    expect(normalizeUserCode('WQXR-KDPA')).toBeNull()
    expect(normalizeUserCode('WQXR-7KDP')).toBeNull()
  })
})

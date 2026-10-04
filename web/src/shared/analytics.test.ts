import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('UX 이벤트 (§16.2)', () => {
  const sent: unknown[] = []

  beforeEach(() => {
    vi.useFakeTimers()
    sent.length = 0
    vi.stubGlobal('fetch', vi.fn((_url: string, init: RequestInit) => {
      sent.push(JSON.parse(init.body as string))
      return Promise.resolve(new Response(null, { status: 202 }))
    }))
    const store = new Map<string, string>()
    vi.stubGlobal('sessionStorage', {
      getItem: (key: string) => store.get(key) ?? null,
      setItem: (key: string, value: string) => void store.set(key, value),
      removeItem: (key: string) => void store.delete(key),
    })
    vi.resetModules()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('조용해지면 모아서 한 번에 보내고, 긴 문자열은 보내지 않는다', async () => {
    const { track } = await import('./analytics')
    track('submission_created', { problem: 'two-sum', language: 'PYTHON' })
    track('problem_open', { source: 'list', problemId: 'x'.repeat(65) })
    expect(sent).toHaveLength(0)
    vi.advanceTimersByTime(2000)
    expect(sent).toHaveLength(1)
    const batch = sent[0] as { events: { name: string; props: Record<string, unknown> }[]; uiVersion: string }
    expect(batch.events.map((e) => e.name)).toEqual(['submission_created', 'problem_open'])
    expect(batch.events[1]!.props).toEqual({ source: 'list' })
    expect(batch.uiVersion).toBe('u9')
  })

  it('보기 이벤트는 같은 열쇠로 한 번만 센다', async () => {
    const { trackOnce, flush } = await import('./analytics')
    trackOnce('list:a', 'problem_list_view', { filters: 0, resultCount: 3, sort: 'number' })
    trackOnce('list:a', 'problem_list_view', { filters: 0, resultCount: 3, sort: 'number' })
    flush()
    expect((sent[0] as { events: unknown[] }).events).toHaveLength(1)
  })

  it('출처는 한 번 읽으면 지워진다', async () => {
    const { markOpenSource, takeOpenSource } = await import('./analytics')
    markOpenSource('search')
    expect(takeOpenSource()).toBe('search')
    expect(takeOpenSource()).toBe('direct')
  })
})

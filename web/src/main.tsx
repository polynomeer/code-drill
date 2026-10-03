import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AppRoutes } from './app/router'
import { ToastProvider } from './design'
import './design/base.css'
import './styles.css'

// 웹 글꼴은 첫 화면을 막지 않는다. @font-face 92개짜리 CSS 가 진입 번들에 들어가면 첫 화면이
// 그만큼 늦어진다 — 시스템 글꼴로 먼저 그리고 글꼴이 오면 바꾼다 (font-display: swap).
void import('./design/fonts.css')

const container = document.getElementById('root')
if (!container) throw new Error('#root 를 찾지 못했다')

/**
 * 서버 상태 캐시 (docs/ui-overhaul.md §3).
 *
 * 창에 돌아올 때 다시 읽는 것은 끈다. 풀이 화면은 오래 열어 두는 곳이라, 탭을 오갈 때마다
 * 목록이 깜빡이며 다시 그려지면 읽던 자리를 잃는다. 판정처럼 실시간이어야 하는 것은 SSE 가 맡는다.
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, refetchOnWindowFocus: false, retry: 1 },
  },
})

createRoot(container).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AppRoutes />
      </ToastProvider>
    </QueryClientProvider>
  </StrictMode>,
)

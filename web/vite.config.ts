import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// vite.config 는 Node 에서 돌지만 이 프로젝트에는 @types/node 가 없다. 한 줄 때문에
// 타입 패키지를 들이는 대신 쓰는 것만 선언한다.
declare const process: { env: Record<string, string | undefined> }

export default defineConfig({
  plugins: [react()],
  server: {
    // 로컬에서는 Control Plane 을 그대로 프록시한다. SSE 는 버퍼링 없이 흘려보내야 한다.
    //
    // 대상 주소를 환경변수로 받는다. 개발 머신에는 8080 을 쓰는 스택이 이미 떠 있는
    // 경우가 흔해서 scripts/up.py 가 빈 포트를 골라 띄우는데, 여기가 8080 으로 굳어
    // 있으면 웹만 엉뚱한 서비스를 부른다.
    proxy: {
      '/api': {
        target: process.env.CONTROL_PLANE_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

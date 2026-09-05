import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // 로컬에서는 Control Plane 을 그대로 프록시한다. SSE 는 버퍼링 없이 흘려보내야 한다.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})

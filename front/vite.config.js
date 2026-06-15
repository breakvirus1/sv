import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

export default defineConfig({
  root: resolve(__dirname, '.'),
  plugins: [react()],
  server: {
    port: 5174,
    host: true,
    proxy: {
      '/realms': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/protocol': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})

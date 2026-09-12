import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

const serverIp = process.env.VITE_SERVER_IP || process.env.SERVER_IP || '192.168.1.40'

export default defineConfig({
  root: resolve(__dirname, '.'),
  plugins: [react()],
  server: {
    port: 5174,
    host: true,
    proxy: {
      '/realms': {
        target: `http://${serverIp}:8080`,
        changeOrigin: true,
        secure: false
      },
      '/protocol': {
        target: `http://${serverIp}:8080`,
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

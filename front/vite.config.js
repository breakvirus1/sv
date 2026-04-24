import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
   server: {
     port: 5174,
     proxy: {
       '/auth': {
         target: 'http://localhost:8085',
         changeOrigin: true,
       },
       '/api': {
         target: 'http://localhost:8085',
         changeOrigin: true,
       },
       '/.well-known': {
         target: 'http://localhost:8085',
         changeOrigin: true,
       },
     },
   },
})
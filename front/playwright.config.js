import { defineConfig, devices, chromium } from '@playwright/test'
import fs from 'fs'

function resolveExecutablePath() {
  if (process.env.PLAYWRIGHT_CHROMIUM_PATH) return process.env.PLAYWRIGHT_CHROMIUM_PATH
  try {
    const bundled = chromium.executablePath()
    if (bundled && fs.existsSync(bundled)) return undefined
  } catch {
    // official browser not installed — fall through
  }
  const fallback = '/opt/yandex/browser/yandex_browser'
  if (fs.existsSync(fallback)) return fallback
  const edge = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
  if (fs.existsSync(edge)) return edge
  return undefined
}

const executablePath = resolveExecutablePath()

export default defineConfig({
  testDir: './e2e',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5174',
    // Снапшоты/скриншоты трейса включают CDP-скринкаст, который на Edge
    // подвешивает сеть: POST /api/v1/auth/token висит до 504 от nginx. Пишем трейс
    // без снапшотов — отладка сохраняется, обмен токенами не блокируется.
    trace: process.env.E2E_TRACE || { mode: 'retain-on-failure', snapshots: false, screenshots: false },
    screenshot: 'only-on-failure',
    ...(executablePath ? { launchOptions: { executablePath } } : {})
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
})

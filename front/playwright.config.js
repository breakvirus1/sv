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
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    ...(executablePath ? { launchOptions: { executablePath } } : {})
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
})

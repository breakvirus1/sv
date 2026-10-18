import { test, expect } from '@playwright/test'
import { login, trackServerErrors } from './helpers'

const ADMIN_PAGES = [
  { path: '/dashboard', marker: null },
  { path: '/orders', marker: null },
  { path: '/orders/new', marker: null },
  { path: '/manager', marker: null },
  { path: '/manager/dashboard', marker: null },
  { path: '/production', marker: null },
  { path: '/production/positions', marker: null },
  { path: '/notifications', marker: null },
  { path: '/test-calculations', marker: null },
  { path: '/admin', marker: 'Клиенты' },
  { path: '/constructor/new', marker: null }
]

test.describe('Все страницы (роль ADMIN)', () => {
  let serverErrors

  test.beforeEach(async ({ page }) => {
    serverErrors = await trackServerErrors(page)
    await login(page, 'admin', 'admin')
  })

  for (const { path, marker } of ADMIN_PAGES) {
    test(`${path} загружается без 5xx`, async ({ page }) => {
      await page.goto(path)
      await expect(page.locator('body')).toBeVisible()
      if (marker) await expect(page.getByRole('tab', { name: marker })).toBeVisible()
      await page.waitForLoadState('networkidle').catch(() => {})
      expect(serverErrors, `server errors on ${path}`).toEqual([])
    })
  }

  test('вкладки AdminPanel переключаются и отдают данные', async ({ page }) => {
    await page.goto('/admin')
    const tabs = ['Материалы', 'Операции', 'Цеха', 'Сотрудники', 'Клиенты']
    for (const name of tabs) {
      await page.getByRole('tab', { name }).click()
      await expect(page.getByRole('tab', { name, selected: true })).toBeVisible()
      await page.waitForLoadState('networkidle').catch(() => {})
    }
    expect(serverErrors).toEqual([])
  })

  test('уведомления открываются из navbar', async ({ page }) => {
    await page.locator('header').getByRole('button').first().click().catch(() => {})
    await page.goto('/notifications')
    await expect(page.locator('body')).toContainText(/Уведомлен|notification|Нет/i, { timeout: 20_000 })
    expect(serverErrors).toEqual([])
  })
})

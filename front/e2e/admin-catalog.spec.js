import { test, expect } from '@playwright/test'
import { trackServerErrors } from './helpers'

/**
 * Админ-панель ролью company 1 (adminsv1, password 111111):
 * справочники материалов (28) и операций (50) содержат только записи СВОЕЙ
 * компании — если изоляция по company_id сломается, spilled записи чужой
 * компании удвоят список (56/100). Страницы отдают без 5xx.
 * (Сид создаётся при создании компании через AdminDashboard — multitenancy.spec.js)
 */
test('admin panel: только свои материалы/операции, без 5xx', async ({ page }) => {
  const serverErrors = []
  page.on('response', (r) => {
    if (r.status() >= 500) serverErrors.push(`${r.status()} ${r.url()}`)
  })

  await page.goto('/')
  await page.getByLabel(/логин|username/i).fill('adminsv1')
  await page.getByLabel(/пароль|password/i).fill('111111')
  await page.getByRole('button', { name: /войти|login/i }).click()
  await page.waitForURL(/orders|admin|\/$/i, { timeout: 30000 })

  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: /админ/i })).toBeVisible({ timeout: 15000 })

  // Материалы: 28 строк своей компании
  await page.getByRole('tab', { name: /материалы/i }).click()
  const materialsTable = page.getByRole('table')
  await expect(materialsTable.getByRole('row').nth(1)).toBeVisible({ timeout: 30000 })
  const mRows = await materialsTable.getByRole('rowgroup').last().getByRole('row').count()
  expect(mRows, 'материалы без утечки из других компаний (28, не 56)').toBe(28)
  await page.screenshot({ path: '/tmp/opencode/shots/admin-materials-company1.png', fullPage: true })

  // Операции: 50 строк своей компании
  await page.getByRole('tab', { name: /операции/i }).click()
  const opsTable = page.getByRole('table')
  await expect(opsTable.getByRole('row').nth(1)).toBeVisible({ timeout: 30000 })
  const oRows = await opsTable.getByRole('rowgroup').last().getByRole('row').count()
  expect(oRows, 'операции без утечки из других компаний (50, не 100)').toBe(50)
  await page.screenshot({ path: '/tmp/opencode/shots/admin-operations-company1.png', fullPage: true })

  expect(serverErrors, `HTTP 5xx: ${serverErrors.join(', ')}`).toEqual([])
})

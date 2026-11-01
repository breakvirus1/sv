import { test, expect } from '@playwright/test'
import { login, trackServerErrors } from './helpers'

/**
 * Админ-панель ролью company 1 (adminsv1, password 111111):
 * справочники материалов и операций содержат только записи СВОЕЙ компании.
 * Если изоляция по company_id сломается, чужие записи удвоят список
 * (одинаковые названия повторятся) — на это и проверяем, не привязываясь
 * к конкретному объёму данных стенда. Страницы отдают без 5xx.
 * (Сид создаётся при создании компании через AdminDashboard — multitenancy.spec.js)
 */
test('admin panel: только свои материалы/операции, без 5xx', async ({ page }) => {
  const serverErrors = await trackServerErrors(page)

  await login(page, 'adminsv1', '111111')

  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: /админ/i })).toBeVisible({ timeout: 15000 })

  // Материалы: записи своей компании, без дублей из других компаний
  await page.getByRole('tab', { name: /материалы/i }).click()
  await assertNoForeignDuplicates(page, 'материалы')
  await page.screenshot({ path: '/tmp/opencode/shots/admin-materials-company1.png', fullPage: true })

  // Операции: то же для операций
  await page.getByRole('tab', { name: /операции/i }).click()
  await assertNoForeignDuplicates(page, 'операции')
  await page.screenshot({ path: '/tmp/opencode/shots/admin-operations-company1.png', fullPage: true })

  expect(serverErrors, `HTTP 5xx: ${serverErrors.join(', ')}`).toEqual([])
})

/** Первый столбец таблицы справочника: строк больше нуля, названия не повторяются. */
async function assertNoForeignDuplicates(page, label) {
  const table = page.getByRole('table')
  await expect(table.getByRole('row').nth(1)).toBeVisible({ timeout: 30000 })
  const names = (await table.getByRole('rowgroup').last()
    .getByRole('row')
    .evaluateAll((rows) => rows.map((r) => (r.cells?.[0]?.innerText || '').trim())))
    .filter(Boolean)
  expect(names.length, `${label}: справочник не должен быть пустым`).toBeGreaterThan(0)
  const duplicated = names.filter((n, i) => names.indexOf(n) !== i)
  expect(duplicated, `${label}: дубли записей других компаний: ${duplicated.join(', ')}`).toEqual([])
}

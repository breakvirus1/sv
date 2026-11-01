import { test, expect } from '@playwright/test'
import { login, logout, trackServerErrors, apiToken, apiGet } from './helpers.js'

/**
 * Мультитенантность: справочники создаются при создании компании (companyId у каждой записи).
 * 1. god создаёт компанию «Компания Тест E2E» через AdminDashboard (UI) — бэкенд засеивает 28 материалов и 50 операций с company_id
 * 2. god создаёт администратора компании (логин c2admin) → появляется пользователь с company_id
 * 3. c2admin логинится и Видит свой справочник материалов/операций
 * 4. admin (company_id=1) НЕ видит записей компании 2 (изоляция по company_id)
 */
const RUN = String(Date.now())
const COMPANY_NAME = `Компания Тест E2E ${RUN}`
const ADMIN_USER = `c2admin${RUN}`
const ADMIN_PASSWORD = 'c2admin'

test.describe.serial('Мультитенантность: материалы/операции на компанию', () => {
  let serverErrors = []

  test.beforeEach(async ({ page }) => {
    serverErrors = await trackServerErrors(page)
  })

  test('god: создаёт компанию и администратора компании', async ({ page }) => {
    page.on('response', (r) => {
      if (r.url().includes('/api/v1/companies') || r.url().includes('/api/v1/employees/keycloak-user')) {
        console.log('NR', r.request().method(), r.status(), r.url())
      }
    })
    page.on('pageerror', (e) => console.log('PAGEERROR', e.message))
    await login(page, 'admingod', 'admingod')
    await page.goto('/admin-dashboard')
    await expect(page.getByRole('tab', { name: 'Компании' })).toBeVisible({ timeout: 30000 })

    // ── Компании: создать компанию
    await page.getByRole('tab', { name: 'Компании' }).click()
    await page.getByRole('button', { name: 'Добавить компанию' }).click()
    await page.getByLabel('Название компании').fill(COMPANY_NAME)
    const companyResp = await (async () => {
      const p = page.waitForResponse((r) => r.url().includes('/api/v1/companies') && r.request().method() === 'POST')
      await page.getByRole('dialog').getByRole('button', { name: 'Создать' }).click()
      return p
    })()
    expect(companyResp.status(), `POST /api/v1/companies -> ${companyResp.status()}`).toBe(201)
    await expect(page.getByText('Компания создана')).toBeVisible()
    await page.getByRole('dialog').waitFor({ state: 'hidden', timeout: 10000 })
    await expect(page.getByRole('cell', { name: COMPANY_NAME })).toBeVisible()

    // ── Администраторы компаний: создать админа этой компании
    await page.getByRole('tab', { name: 'Администраторы компаний' }).click()
    await page.getByRole('button', { name: 'Добавить администратора' }).click()
    await page.getByLabel('ФИО').fill('Админ Второй')
    await page.getByLabel('Логин').fill(ADMIN_USER)
    await page.getByLabel('Пароль', { exact: true }).fill(ADMIN_PASSWORD)
    await page.getByLabel('Email').fill(`${ADMIN_USER}@example.com`)
    const companySelect = page.getByRole('dialog').getByRole('combobox').first()
    await companySelect.click()
    await page.getByRole('option', { name: COMPANY_NAME }).click()
    // Пользователя Keycloak создаёт бэкенд (браузер не держит admin-креденты Keycloak)
    const kcResp = await (async () => {
      const p = page.waitForResponse((r) => r.url().includes('/api/v1/employees/keycloak-user') && r.request().method() === 'POST')
      await page.getByRole('dialog').getByRole('button', { name: 'Создать' }).click()
      return p
    })()
    expect(kcResp.status(), `POST /api/v1/employees/keycloak-user -> ${await kcResp.text()}`).toBe(201)
    await expect(page.getByText('Администратор компании создан')).toBeVisible({ timeout: 30000 })
    await page.getByRole('dialog').waitFor({ state: 'hidden', timeout: 10000 })

    expect(serverErrors, `HTTP 5xx: ${serverErrors.join(', ')}`).toEqual([])
    await logout(page)
  })

  test('c2admin: видит свой справочник материалов и операций', async ({ page }) => {
    await login(page, ADMIN_USER, ADMIN_PASSWORD)

    // ждём создания employee-записи (employee-service уже должен синхронизироваться по create-admin потоку)
    await page.goto('/admin')
    await page.getByRole('tab', { name: 'Материалы' }).click()

    // справочник, засеянный при создании компании: 28 строк
    await expect(page.getByText('Баннер ламинированный (от 320 г/м²)')).toBeVisible({ timeout: 30000 })
    await expect(page.getByText('Скотч вспененный/баннерный')).toBeVisible()

    // операции получили company_id при создании компании
    await page.getByRole('tab', { name: 'Операции' }).click()
    await expect(page.getByText('Печать на плёнке 1440 dpi').first()).toBeVisible({ timeout: 30000 })

    expect(serverErrors, `HTTP 5xx: ${serverErrors.join(', ')}`).toEqual([])
    await logout(page)
  })

  test('admin компании 1: не видит записей компании 2', async ({ page, request }) => {
    await login(page, 'admin', 'admin')
    await page.goto('/admin')
    await page.getByRole('tab', { name: 'Материалы' }).click()

    // admin принадлежит компании 1 (claim company_id=1). Сид при создании компании
    // копирует одинаковые названия, поэтому утечка чужих записей проявляется дублями
    // и расхождением с API своей компании — привязка к числу 28/56 не нужна.
    const table = page.getByRole('table')
    await expect(table.getByRole('row').nth(1)).toBeVisible({ timeout: 30000 })
    const names = await table.getByRole('rowgroup').last().getByRole('row')
      .evaluateAll((rows) => rows.map((r) => (r.cells?.[0]?.innerText || '').trim()).filter(Boolean))
    const duplicated = names.filter((n, i) => names.indexOf(n) !== i)
    expect(duplicated, `утечка записей других компаний (дубли): ${duplicated.join(', ')}`).toEqual([])

    const token = await apiToken(page, 'admin', 'admin')
    const api = await apiGet(request, '/api/v1/materials?size=1000', token)
    expect(api.status(), await api.text()).toBe(200)
    const body = await api.json()
    const apiCount = (body.content || body).length
    expect(apiCount, 'справочник материалов компании 1 непустой').toBeGreaterThan(0)
    expect(names.length, 'в UI ровно то, что отдаёт API своей компании').toBe(apiCount)

    expect(serverErrors, `HTTP 5xx: ${serverErrors.join(', ')}`).toEqual([])
  })
})

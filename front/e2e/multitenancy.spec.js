import { test, expect } from '@playwright/test'
import { login, logout, trackServerErrors } from './helpers.js'

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
      if (r.url().includes('/api/v1/companies') || r.url().includes('/realms/master') || r.url().includes('/admin/realms/print-sv')) {
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
    await page.getByLabel('Пароль').fill(ADMIN_PASSWORD)
    await page.getByLabel('Email').fill(`${ADMIN_USER}@example.com`)
    const companySelect = page.getByRole('dialog').getByRole('combobox').first()
    await companySelect.click()
    await page.getByRole('option', { name: COMPANY_NAME }).click()
    const kcResp = await (async () => {
      const p = page.waitForResponse((r) => r.url().includes('/admin/realms/print-sv/users') && r.request().method() === 'POST')
      await page.getByRole('dialog').getByRole('button', { name: 'Создать' }).click()
      return p
    })()
    expect(kcResp.status(), `POST keycloak users -> ${kcResp.status()}`).toBe(201)
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

  test('admin компании 1: не видит записей компании 2', async ({ page }) => {
    await login(page, 'admin', 'admin')
    await page.goto('/admin')
    await page.getByRole('tab', { name: 'Материалы' }).click()

    // admin принадлежит компании 1 (claim company_id=1) — на чистом стенде
    // компания 1 создана этим же прогоном и получила свои 28 материалов.
    // Изоляция: админ видит РОВНО 28 строк, а не 56 (утечка компании 2).
    const table = page.getByRole('table')
    await expect(table.getByRole('row').nth(1)).toBeVisible({ timeout: 30000 })
    const dataRows = table.getByRole('rowgroup').last().getByRole('row')
    const count = await dataRows.count()
    expect(count, 'материалы видны только своей компании (28, не 56)').toBe(28)

    expect(serverErrors, `HTTP 5xx: ${serverErrors.join(', ')}`).toEqual([])
  })
})

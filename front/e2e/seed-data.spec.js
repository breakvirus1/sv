import { test, expect } from '@playwright/test'
import { login, trackServerErrors } from './helpers'

const TS = Date.now()
const SHOTS = '/tmp/opencode/shots'

test.describe('Seed: клиенты, сотрудники, 100 заказов', () => {
  test.setTimeout(900_000)

  test('создание данных через UI и генерация 100 заказов', async ({ page, context }) => {
    const failures = await trackServerErrors(page)
    const consoleErrors = []
    page.on('console', (msg) => {
      if (msg.type() === 'error') consoleErrors.push(msg.text())
    })

    // ---- 1. Логин ----
    await login(page, 'adminsv1', '111111')
    await expect(page).toHaveURL(/\/(orders|admin-dashboard|manager)/)
    await page.screenshot({ path: `${SHOTS}/01-after-login.png`, fullPage: true })

    // ---- 2. Админ панель -> Клиенты ----
    await page.goto('/admin')
    await expect(page.getByText('Админ панель')).toBeVisible({ timeout: 30_000 })
    await page.getByRole('tab', { name: 'Клиенты' }).click()
    await expect(page.getByText('Управление клиентами')).toBeVisible()

    const clientCount = 5
    for (let i = 1; i <= clientCount; i++) {
      await page.getByRole('button', { name: 'Добавить клиента' }).click()
      const dialog = page.getByRole('dialog')
      await expect(dialog).toBeVisible()
      await dialog.getByLabel('Название').fill(`PW Клиент ${TS}-${i}`)
      await dialog.getByLabel('Контактное лицо').fill(`Тест Тестов ${i}`)
      await dialog.getByLabel('Телефон').fill(`+790011122${String(i).padStart(2, '0')}`)
      await dialog.getByLabel('Email').fill(`pwclient${i}-${TS}@example.test`)
      await dialog.getByRole('button', { name: 'Сохранить' }).click()
      await expect(page.getByRole('dialog')).toBeHidden()
      await expect(page.getByText('Клиент создан')).toBeVisible({ timeout: 15_000 })
    }
    await expect(page.getByText(`PW Клиент ${TS}-1`)).toBeVisible()
    await expect(page.getByText(`PW Клиент ${TS}-${clientCount}`)).toBeVisible()
    await page.screenshot({ path: `${SHOTS}/02-clients.png`, fullPage: true })

    // ---- 3. Сотрудники (создание через Keycloak) ----
    await page.getByRole('tab', { name: 'Сотрудники' }).click()
    await expect(page.getByText('Управление сотрудниками')).toBeVisible()

    const employees = [
      { fullName: `ПВ Менеджер ${TS}`, username: `pwmgr${TS}`, role: 'Менеджер' },
      { fullName: `ПВ Производство ${TS}`, username: `pwprod${TS}`, role: 'Производство' },
      { fullName: `ПВ Менеджер2 ${TS}`, username: `pwmmgr${TS}`, role: 'Менеджер' },
    ]
    for (const emp of employees) {
      await page.getByRole('button', { name: 'Добавить сотрудника' }).click()
      const dialog = page.getByRole('dialog')
      await expect(dialog).toBeVisible()
      await dialog.getByLabel('ФИО').fill(emp.fullName)
      await dialog.getByLabel('Логин').fill(emp.username)
      await dialog.getByLabel('Пароль', { exact: true }).fill('Test1234!')
      await dialog.getByLabel('Email').fill(`${emp.username}@example.test`)
      const currentRole = await dialog.getByRole('combobox').textContent()
      if ((currentRole || '').trim() !== emp.role) {
        await dialog.getByRole('combobox').click()
        await page.getByRole('option', { name: emp.role, exact: true }).click()
      }
      await dialog.getByRole('button', { name: 'Создать' }).click()
      await expect(page.getByRole('dialog')).toBeHidden({ timeout: 60_000 })
      await expect(page.getByText('Сотрудник создан')).toBeVisible({ timeout: 30_000 })
    }
    await expect(page.getByText(`ПВ Менеджер ${TS}`)).toBeVisible()
    await expect(page.getByText(`ПВ Производство ${TS}`)).toBeVisible()
    await page.screenshot({ path: `${SHOTS}/03-employees.png`, fullPage: true })

    // ---- 4. Генерация 100 заказов ----
    await page.getByRole('tab', { name: 'Генерация данных' }).click()
    await expect(page.getByText('Генерация тестовых данных')).toBeVisible()
    const ordersField = page.getByLabel('Заказы')
    await ordersField.fill('100')
    await page
      .locator('div.MuiGrid-item')
      .filter({ has: page.getByLabel('Заказы') })
      .getByRole('button', { name: 'Сгенерировать' })
      .click()
    await expect(page.getByText('Сгенерировано 100 заказов')).toBeVisible({ timeout: 300_000 })
    await page.screenshot({ path: `${SHOTS}/04-generate-orders.png`, fullPage: true })

    // ---- 5. Проверка через API ----
    const token = await page.evaluate(() => sessionStorage.getItem('token'))
    const ordersRes = await page.evaluate(async (t) => {
      const r = await fetch('/api/v1/orders?page=0&size=1', { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, token)
    console.log('ORDERS_TOTAL=', ordersRes.body?.totalElements, 'status=', ordersRes.status)
    expect(ordersRes.status).toBe(200)
    expect(ordersRes.body.totalElements).toBeGreaterThanOrEqual(100)

    const clientsRes = await page.evaluate(async (t) => {
      const r = await fetch('/api/v1/clients?size=100', { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, token)
    const clients = Array.isArray(clientsRes.body) ? clientsRes.body : clientsRes.body?.content || []
    console.log('CLIENTS_TOTAL=', clients.length, 'status=', clientsRes.status)
    expect(clients.some((c) => c.name === `PW Клиент ${TS}-1`)).toBeTruthy()

    const employeesRes = await page.evaluate(async (t) => {
      const r = await fetch('/api/v1/employees?size=200', { headers: { Authorization: `Bearer ${t}` } })
      return { status: r.status, body: await r.json() }
    }, token)
    const emps = Array.isArray(employeesRes.body) ? employeesRes.body : employeesRes.body?.content || []
    console.log('EMPLOYEES_TOTAL=', emps.length, 'status=', employeesRes.status)
    expect(emps.some((e) => e.username === `pwmgr${TS}`)).toBeTruthy()
    expect(emps.some((e) => e.username === `pwprod${TS}`)).toBeTruthy()

    // ---- 6. UI: список менеджера ----
    await page.goto('/manager')
    await expect(page.getByText(/менеджер/i).first()).toBeVisible({ timeout: 30_000 }).catch(() => {})
    await page.waitForTimeout(3000)
    await page.screenshot({ path: `${SHOTS}/05-manager-orders.png`, fullPage: true })

    console.log('HTTP_5XX=', JSON.stringify(failures))
    console.log('CONSOLE_ERRORS=', JSON.stringify(consoleErrors.slice(0, 20)))
    expect(failures).toEqual([])
  })
})

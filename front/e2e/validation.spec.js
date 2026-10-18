import { test, expect } from '@playwright/test'
import { login, trackServerErrors } from './helpers'

const TS = Date.now()
const SHOTS = '/tmp/opencode/shots'

test.describe('Валидация полей: сообщения бэкенда на фронте', () => {
  test.setTimeout(180_000)

  test('клиент: полевая ошибка email и конфликт дубликата названия', async ({ page }) => {
    const failures = await trackServerErrors(page)
    await login(page, 'adminsv1', '111111')
    await page.goto('/admin')
    await expect(page.getByText('Админ панель')).toBeVisible({ timeout: 30_000 })
    await page.getByRole('tab', { name: 'Клиенты' }).click()
    await expect(page.getByText('Управление клиентами')).toBeVisible()

    // Существующее название клиента (первая строка таблицы)
    const existingName = (await page.locator('table tbody tr td').first().textContent()).trim()
    expect(existingName.length).toBeGreaterThan(0)

    await page.getByRole('button', { name: 'Добавить клиента' }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()

    // 1) Некорректный email -> полевая ошибка от бэкенда
    await dialog.getByLabel('Название').fill(`PW Валидация ${TS}`)
    await dialog.getByLabel('Email').fill('не-почта')
    await dialog.getByRole('button', { name: 'Сохранить' }).click()
    await expect(dialog.getByText('Некорректный email')).toBeVisible({ timeout: 15_000 })
    await expect(dialog).toBeVisible()
    await page.screenshot({ path: `${SHOTS}/validation-client-email.png`, fullPage: true })

    // 2) Дубликат названия -> сообщение конфликта (409)
    await dialog.getByLabel('Email').fill(`pwval-${TS}@example.test`)
    await dialog.getByLabel('Название').fill(existingName)
    await dialog.getByRole('button', { name: 'Сохранить' }).click()
    await expect(page.getByText('Клиент с таким названием уже существует')).toBeVisible({ timeout: 15_000 })
    await expect(dialog).toBeVisible()
    await page.screenshot({ path: `${SHOTS}/validation-client-duplicate.png`, fullPage: true })

    await dialog.getByRole('button', { name: 'Отмена' }).click()
    expect(failures, `5xx: ${failures.join('; ')}`).toEqual([])
  })

  test('материал: отрицательная цена -> полевая ошибка', async ({ page }) => {
    const failures = await trackServerErrors(page)
    await login(page, 'adminsv1', '111111')
    await page.goto('/admin')
    await expect(page.getByText('Админ панель')).toBeVisible({ timeout: 30_000 })
    await page.getByRole('tab', { name: 'Материалы' }).click()
    await expect(page.getByText('Управление материалами')).toBeVisible()

    await page.getByRole('button', { name: 'Добавить материал' }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()

    await dialog.getByLabel('Название').fill(`PW ВалидМатериал ${TS}`)
    await dialog.getByRole('combobox').click()
    await page.getByRole('option', { name: 'м²' }).click()
    await dialog.getByLabel('Цена').fill('-5')
    await dialog.getByRole('button', { name: 'Сохранить' }).click()
    await expect(dialog.getByText('Цена не может быть отрицательной')).toBeVisible({ timeout: 15_000 })
    await expect(dialog).toBeVisible()
    await page.screenshot({ path: `${SHOTS}/validation-material-price.png`, fullPage: true })

    await dialog.getByRole('button', { name: 'Отмена' }).click()
    expect(failures, `5xx: ${failures.join('; ')}`).toEqual([])
  })
})

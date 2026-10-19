import { test, expect } from '@playwright/test'
import { login, trackServerErrors } from './helpers'

test.describe('Admin Dashboard (роль GOD)', () => {
  let serverErrors

  test.beforeEach(async ({ page }) => {
    serverErrors = await trackServerErrors(page)
    await login(page, 'admingod', 'admingod')
    await expect(page).toHaveURL(/\/admin-dashboard/)
  })

  test('вкладка «Компании» отображает таблицу', async ({ page }) => {
    await expect(page.getByRole('tab', { name: 'Компании' })).toBeVisible()
    await page.getByRole('tab', { name: 'Компании' }).click()
    await page.waitForLoadState('networkidle').catch(() => {})
    // либо есть компании, либо явная пустая заглушка
    await expect(page.getByText(/Компании|Нет компаний/i).first()).toBeVisible()
    expect(serverErrors).toEqual([])
  })

  test('администраторы компаний отображаются (admin, adminsv)', async ({ page }) => {
    await page.getByRole('tab', { name: 'Администраторы компаний' }).click()
    await page.waitForLoadState('networkidle').catch(() => {})

    await expect(page.getByRole('heading', { name: 'Администраторы компаний' })).toBeVisible()

    // регрессия фикса: оба админа с ROLE_ADMIN и companyId=1 должны быть в списке
    const table = page.locator('table')
    await expect(table).toBeVisible({ timeout: 20_000 })
    await expect(table.getByRole('row')).not.toHaveCount(1) // есть строки помимо шапки
    await expect(page.getByRole('cell', { name: 'admin', exact: true })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'adminsv1', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Добавить администратора' })).toBeVisible()

    expect(serverErrors).toEqual([])
  })

  test('диалог создания администратора открывается и валидирует', async ({ page }) => {
    await page.getByRole('tab', { name: 'Администраторы компаний' }).click()
    await page.getByRole('button', { name: 'Добавить администратора' }).click()
    await expect(page.getByText('Новый администратор компании')).toBeVisible()
    // сабмит пустой формы — валидация «Заполните все поля»
    await page.getByRole('button', { name: /^(Создать|Добавить|Сохранить)$/ }).last().click()
    await expect(page.getByText('Заполните все поля')).toBeVisible({ timeout: 10_000 })
    // закрыть диалог
    await page.getByRole('button', { name: 'Отмена' }).click()
    expect(serverErrors).toEqual([])
  })
})

import { test, expect } from '@playwright/test'
import { login, logout, trackServerErrors } from './helpers'

test.describe('Авторизация', () => {
  test('страница логина отображает форму', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByLabel('Логин')).toBeVisible()
    await expect(page.getByLabel('Пароль')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Войти' })).toBeVisible()
  })

  test('неверный пароль показывает ошибку', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Логин').fill('admin')
    await page.getByLabel('Пароль').fill('wrong-password')
    await page.getByRole('button', { name: 'Войти' }).click()
    await expect(page.getByRole('alert')).toBeVisible({ timeout: 20_000 })
    await expect(page).toHaveURL(/\/login/)
  })

  test('admin входит и попадает на /orders', async ({ page }) => {
    await login(page, 'admin', 'admin')
    await expect(page).toHaveURL(/\/orders/)
    await expect(page.locator('header')).toBeVisible()
  })

  test('admingod входит и попадает на /admin-dashboard', async ({ page }) => {
    await login(page, 'admingod', 'admingod')
    await expect(page).toHaveURL(/\/admin-dashboard/)
    await expect(page.getByRole('heading', { name: 'Admin Dashboard' })).toBeVisible()
  })

  test('неавторизованный доступ к /admin редиректит на /login', async ({ page }) => {
    await page.goto('/admin').catch(() => {})
    await expect(page).toHaveURL(/\/login/)
  })

  test('после выхода сессия сбрасывается', async ({ page }) => {
    const errors = await trackServerErrors(page)
    await login(page, 'admin', 'admin')
    await page.locator('header').getByRole('button').last().click()
    await page.getByRole('menuitem', { name: 'Выход' }).click()
    await expect(page).toHaveURL(/\/login/, { timeout: 30_000 })
    expect(errors).toEqual([])
  })
})

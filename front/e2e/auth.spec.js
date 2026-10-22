import { test, expect } from '@playwright/test'
import { login, trackServerErrors } from './helpers'

test.describe('Авторизация', () => {
  test('страница логина предлагает вход через Keycloak', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('button', { name: 'Войти' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Print SV' })).toBeVisible()
  })

  test('неверный пароль показывает ошибку Keycloak', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('button', { name: 'Войти' }).click()
    await page.waitForURL(/\/realms\/print-sv\/protocol\/openid-connect\/auth/, { timeout: 30_000 })
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('wrong-password')
    await page.locator('#kc-login').click()
    await expect(page.locator('#input-error')).toBeVisible({ timeout: 20_000 })
    await expect(page).toHaveURL(/\/realms\/print-sv\/protocol\/openid-connect\/auth/)
  })

  test('admin входит и попадает в приложение', async ({ page }) => {
    await login(page, 'admin', 'admin')
    await expect(page).toHaveURL(/\/(manager|orders)/)
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

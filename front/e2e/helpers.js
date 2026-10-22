import { expect } from '@playwright/test'

/**
 * Собирает все ответы с HTTP 5xx — тесты обязаны убедиться, что список пуст.
 */
export async function trackServerErrors(page) {
  const failures = []
  page.on('response', (res) => {
    if (res.status() >= 500) failures.push(`${res.status()} ${res.url()}`)
  })
  return failures
}

/**
 * Вход через форму Keycloak (Authorization Code + PKCE).
 * Если SSO-сессия Keycloak уже активна, форма может быть пропущена.
 */
export async function login(page, username, password) {
  await page.goto('/login')
  await page.getByRole('button', { name: 'Войти' }).click()
  await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 30_000 })

  if (page.url().includes('/realms/')) {
    await page.locator('#username').fill(username)
    await page.locator('#password').fill(password)
    await page.locator('#kc-login').click()
  }

  await page.waitForURL(
    (url) =>
      !url.pathname.startsWith('/login') &&
      !url.pathname.startsWith('/callback') &&
      !url.pathname.includes('/realms/'),
    { timeout: 30_000 }
  )
}

export async function logout(page) {
  await page.locator('header').getByRole('button').last().click()
  await page.getByRole('menuitem', { name: 'Выход' }).click()
}

/**
 * Возвращает access token из текущей сессии браузера.
 * Если сессии нет — выполняет вход через UI Keycloak.
 */
export async function apiToken(page, username, password) {
  const existing = await page.evaluate(() => window.sessionStorage.getItem('token'))
  if (existing) return existing

  await login(page, username, password)
  const token = await page.evaluate(() => window.sessionStorage.getItem('token'))
  expect(token, 'access token должен быть сохранён в sessionStorage').toBeTruthy()
  return token
}

export async function apiGet(request, path, token) {
  return request.get(path, { headers: { Authorization: `Bearer ${token}` } })
}

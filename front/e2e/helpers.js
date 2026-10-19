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

export async function login(page, username, password) {
  await page.goto('/login')
  await page.getByLabel('Логин').fill(username)
  await page.getByLabel('Пароль').fill(password)
  await page.getByRole('button', { name: 'Войти' }).click()
  await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 30_000 })
}

export async function logout(page) {
  await page.locator('header').getByRole('button').last().click()
  await page.getByRole('menuitem', { name: 'Выход' }).click()
}

/**
 * Токен через password grant Keycloak (проксируется фронтенд-nginx как /realms).
 */
export async function apiToken(request, username, password) {
  const res = await request.post('/realms/print-sv/protocol/openid-connect/token', {
    form: {
      grant_type: 'password',
      client_id: 'frontend',
      username,
      password
    }
  })
  expect(res.ok(), `token request failed: ${res.status()} ${await res.text()}`).toBeTruthy()
  const data = await res.json()
  return data.access_token
}

export async function apiGet(request, path, token) {
  return request.get(path, { headers: { Authorization: `Bearer ${token}` } })
}

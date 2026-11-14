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
  await page.getByRole('button', { name: 'Войти через Keycloak (SSO)' }).click()
  await page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 60_000 })

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
    { timeout: 60_000 }
  )
}

export async function logout(page) {
  await page.locator('header').getByRole('button').last().click()
  await page.getByRole('menuitem', { name: 'Выход' }).click()
}

/**
 * Возвращает access token из текущей сессии браузера.
 * Всегда логинится заново (очищает sessionStorage), чтобы не вернуть чужой токен.
 */
/**
 * Возвращает access token через password grant API (минуя браузерный SSO).
 * Работает через Playwright APIRequestContext, надёжно и быстро.
 */
export async function apiToken(request, username, password) {
  const response = await request.post('/api/v1/auth/login', {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    form: { username, password }
  })
  if (!response.ok()) {
    const text = await response.text()
    throw new Error(`Login failed: ${response.status()} ${text.slice(0, 200)}`)
  }
  const data = await response.json()
  expect(data.access_token, 'access token должен быть получен').toBeTruthy()
  return data.access_token
}

/**
 * Делает fetch через контекст браузера (page.evaluate), чтобы запрос шёл
 * с теми же куки/заголовками, что и из приложения.
 * options может содержать headers и body/data.
 * Возвращает объект с методом .status() и .json() как у Playwright APIResponse.
 */
export async function apiFetch(page, path, method = 'GET', options = {}) {
  const headers = { ...(options.headers || {}) }
  if (options.Authorization) headers['Authorization'] = options.Authorization
  else if (options.token) headers['Authorization'] = `Bearer ${options.token}`
  const body = options.body !== undefined ? options.body : options.data
  if (body && !headers['Content-Type']) headers['Content-Type'] = 'application/json'
  const res = await page.evaluate(async ({ p, m, h, b }) => {
    const r = await fetch(p, {
      method: m,
      headers: h,
      body: b ? JSON.stringify(b) : undefined,
    })
    return { status: r.status, body: await r.text() }
  }, { p: path, m: method, h: headers, b: body })
  return {
    status: () => res.status,
    text: async () => res.body,
    json: async () => JSON.parse(res.body),
    body: res.body,
  }
}

export async function apiGet(page, path, optionsOrToken = {}) {
  let options = optionsOrToken
  if (typeof optionsOrToken === 'string') {
    options = { headers: { Authorization: `Bearer ${optionsOrToken}` } }
  }
  return apiFetch(page, path, 'GET', options)
}

export async function apiPost(page, path, bodyOrOptions = {}, tokenOrOptions = {}) {
  let options = bodyOrOptions
  let token = tokenOrOptions
  if (typeof bodyOrOptions === 'string' || !bodyOrOptions || !bodyOrOptions.headers) {
    options = { headers: {} }
    if (typeof bodyOrOptions === 'string') token = bodyOrOptions
    if (tokenOrOptions && typeof tokenOrOptions === 'string') token = tokenOrOptions
    if (token) options.headers['Authorization'] = `Bearer ${token}`
  }
  return apiFetch(page, path, 'POST', options)
}

export async function apiPut(page, path, options = {}) {
  return apiFetch(page, path, 'PUT', options)
}

export async function apiPatch(page, path, options = {}) {
  return apiFetch(page, path, 'PATCH', options)
}

export async function apiDelete(page, path, options = {}) {
  return apiFetch(page, path, 'DELETE', options)
}

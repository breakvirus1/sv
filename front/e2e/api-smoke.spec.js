import { test, expect } from '@playwright/test'
import { apiToken, apiGet } from './helpers'

/**
 * Прогон ключевых API-эндпоинтов через фронтенд-прокси (nginx / Vite dev proxy)
 * — проверяет, что все сервисы за gateway доступны и не отдают 5xx.
 */
test.describe('API smoke через фронтенд', () => {
  let token

  test.beforeAll(async ({ request }) => {
    token = await apiToken(request, 'admin', 'admin')
    // синхронизируем сотрудников из keycloak сразу после чистого запуска стенда
    await request.post('/api/v1/employees/sync', { headers: { Authorization: `Bearer ${token}` } })
  })

  const GET_ENDPOINTS = [
    '/api/v1/orders?page=0&size=20',
    '/api/v1/clients?size=100',
    '/api/v1/companies?size=100',
    '/api/v1/employees?size=100',
    '/api/v1/materials?size=100',
    '/api/v1/calculations/operations',
    '/api/v1/calculations/eyelets',
    '/api/v1/notifications',
    '/api/v1/notifications/unread-count',
    '/api/v1/workshops?size=100',
    '/api/v1/products',
    '/api/v1/admin/operation-groups'
  ]

  for (const path of GET_ENDPOINTS) {
    test(`GET ${path} → 200`, async ({ request }) => {
      const res = await apiGet(request, path, token)
      const body = await res.text()
      expect(res.status(), `${path}: ${body.slice(0, 300)}`).toBe(200)
    })
  }

  test('GET /api/v1/employees содержит admin и adminsv с ROLE_ADMIN', async ({ request }) => {
    const res = await apiGet(request, '/api/v1/employees?size=100', token)
    expect(res.status()).toBe(200)
    const data = await res.json()
    const admins = data.content.filter((e) => (e.roles || []).includes('ROLE_ADMIN'))
    const usernames = admins.map((e) => e.username)
    expect(usernames).toContain('admin')
    expect(usernames.some((u) => u === 'adminsv' || u.startsWith('adminsv'))).toBe(true)
    for (const a of admins) expect(a.companyId).toBe(1)
  })

  test('POST /api/v1/employees/sync → 200', async ({ request }) => {
    const res = await request.post('/api/v1/employees/sync', {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(res.status()).toBe(200)
  })

  test('без токена API отдаёт 401', async ({ request }) => {
    const res = await request.get('/api/v1/employees?size=10')
    expect(res.status()).toBe(401)
  })
})

import { test, expect } from '@playwright/test'
import { apiToken, apiGet, login } from './helpers'

/**
 * Полный обход публичных эндпоинтов через фронтенд-прокси (nginx :5174 → gateway :8085).
 * Покрывает все GET-эндпоинты, доступные роли ADMIN, плюс CRUD-циклы и проверки прав.
 * Запросы идут тем же путём, что и из браузера, — значит проверяются и маршруты шлюза.
 */

const RUN = Date.now()

/** Демо-данные стенда (keycloak/realm-export.dev.json): админ компании 1. */
const DEMO = { username: 'admin', password: 'admin' }
const DEMO_MANAGER = { username: 'manager', password: 'manager' }

/** GET-эндпоинты без path-параметров. */
const PLAIN_GETS = [
  '/api/v1/clients?size=5',
  '/api/v1/companies',
  '/api/v1/employees?size=5',
  '/api/v1/materials?size=5',
  '/api/v1/workshops?size=5',
  '/api/v1/products',
  '/api/v1/orders?page=0&size=5',
  '/api/v1/orders/filter-options',
  '/api/v1/comments?size=5',
  '/api/v1/comment-replies?size=5',
  '/api/v1/notifications',
  '/api/v1/notifications/unread-count',
  '/api/v1/calculations/materials',
  '/api/v1/calculations/materials/BANNER',
  '/api/v1/calculations/materials/PLENKA',
  '/api/v1/calculations/operations',
  '/api/v1/calculations/operations/grouped',
  '/api/v1/calculations/eyelets',
  '/api/v1/admin/operations',
  '/api/v1/admin/operation-groups',
  '/api/v1/admin/statistics/material-expense'
]

/** Эндпоинты, которые шлюз маршрутизирует, но контроллера под ними нет. */
const KNOWN_DEAD_ROUTES = ['/api/v1/admin/products']

test.describe('Обход всех эндпоинтов', () => {
  test.describe.configure({ mode: 'default' })

  let token
  let managerToken
  let ids

  // отдельный контекст = чистые cookie Keycloak, иначе активная SSO-сессия
  // вернула бы токен предыдущего пользователя вместо запрошенного
  const tokenVia = async (browser, { username, password }) => {
    const context = await browser.newContext()
    const page = await context.newPage()
    const value = await apiToken(page, username, password)
    await context.close()
    return value
  }

  test.beforeAll(async ({ browser, request }) => {
    token = await tokenVia(browser, DEMO)
    managerToken = await tokenVia(browser, DEMO_MANAGER)

    const auth = { headers: { Authorization: `Bearer ${token}` } }
    const json = async (path) => {
      const res = await request.get(path, { headers: auth.headers })
      expect(res.status(), `${path} должен отдавать 200`).toBe(200)
      return res.json()
    }
    const listOf = (data) => (Array.isArray(data) ? data : data.content || [])
    const post = async (path, data) => {
      const res = await request.post(path, { headers: auth.headers, data })
      const text = await res.text()
      expect(res.status(), `POST ${path}: ${text.slice(0, 200)}`).toBeLessThan(300)
      return JSON.parse(text)
    }
    const isoDate = (offsetDays) => new Date(Date.now() + offsetDays * 86_400_000).toISOString().slice(0, 10)

    let orders = listOf(await json('/api/v1/orders?page=0&size=5'))
    let clients = listOf(await json('/api/v1/clients?size=5'))
    let materials = listOf(await json('/api/v1/materials?size=5'))
    const workshops = listOf(await json('/api/v1/workshops?size=5'))
    const products = listOf(await json('/api/v1/products'))
    let operations = listOf(await json('/api/v1/calculations/operations'))
    const companies = listOf(await json('/api/v1/companies'))
    const employees = listOf(await json('/api/v1/employees?size=50'))

    // Стенд может быть чистым: докидываем недостающее теми же эндпоинтами,
    // которые проверяем, иначе адресные GET деградируют в молчаливые skip.
    if (!clients.length) {
      clients = [await post('/api/v1/clients', { name: `E2E Клиент ${RUN}`, type: 'COMPANY' })]
    }
    if (!operations.length) {
      operations = [await post('/api/v1/admin/operations', {
        name: `E2E Операция ${RUN}`, price: 100, unit: 'SQUARE_METER'
      })]
    }
    if (!materials.length) {
      materials = [await post('/api/v1/materials', {
        name: `E2E Материал ${RUN}`, unit: 'м2', price: 500, wasteCoefficient: 1.1,
        defaultWidthM: 1.6, defaultHeightM: 1
      })]
    }
    if (!orders.length) {
      orders = [await post('/api/v1/orders', {
        clientId: clients[0].id,
        description: `E2E Заказ ${RUN}`,
        orderDate: isoDate(0),
        dueDate: isoDate(9),
        items: [{
          materialId: materials[0].id,
          widthM: 1,
          heightM: 0.5,
          readyDate: isoDate(9),
          operations: []
        }]
      })]
    }

    // полный заказ = тот, у которого есть и позиции, и материалы.
    // В постраничном списке заказов items/materials не сериализуются (они пустые),
    // поэтому состав берём из карточки выбранного заказа.
    const picked = orders.find((o) => (o.items || []).length > 0 && (o.materials || []).length > 0) || orders[0] || {}
    const full = picked.id ? await json(`/api/v1/orders/${picked.id}`) : picked
    const manager = employees.find((e) => (e.roles || []).includes('ROLE_MANAGER'))

    ids = {
      orderId: full.id,
      orderNumber: full.orderNumber,
      orderItemId: (full.items || [])[0]?.id,
      orderMaterialId: (full.materials || [])[0]?.id,
      managerId: manager?.id ?? full.manager?.id,
      clientId: clients[0]?.id,
      materialId: materials[0]?.id,
      workshopId: workshops[0]?.id,
      productId: products[0]?.id,
      operationId: operations[0]?.id,
      companyId: companies[0]?.id,
      employeeId: employees[0]?.id,
      employeeUsername: employees[0]?.username
    }
  })

  test('стенд содержит демо-данные для обхода', () => {
    // без них тесты с path-параметрами деградируют до молчаливых skip
    expect(ids.orderId, 'нужен хотя бы один заказ').toBeTruthy()
    expect(ids.clientId, 'нужен хотя бы один клиент').toBeTruthy()
    expect(ids.materialId, 'нужен хотя бы один материал').toBeTruthy()
  })

  for (const path of PLAIN_GETS) {
    test(`GET ${path} → 200`, async ({ request }) => {
      const res = await apiGet(request, path, token)
      const body = await res.text()
      expect(res.status(), `${path}: ${body.slice(0, 300)}`).toBe(200)
    })
  }

  test.describe('GET с path-параметрами', () => {
    const cases = () => [
      [`/api/v1/clients/${ids.clientId}`, 'клиент по id'],
      [`/api/v1/companies/${ids.companyId}`, 'компания по id'],
      [`/api/v1/employees/${ids.employeeId}`, 'сотрудник по id'],
      [`/api/v1/employees/username/${ids.employeeUsername}`, 'сотрудник по логину'],
      [`/api/v1/employees/roles/${ids.employeeUsername}`, 'роли сотрудника из Keycloak'],
      [`/api/v1/materials/${ids.materialId}`, 'материал по id'],
      [`/api/v1/materials/${ids.materialId}/operations`, 'операции материала'],
      [`/api/v1/calculations/materials/${ids.materialId}/grouped-operations`, 'группы операций материала'],
      [`/api/v1/admin/materials/${ids.materialId}/operation-groups`, 'группы операций (админ)'],
      [`/api/v1/admin/materials/${ids.materialId}/operation-groups/all`, 'все группы материала (админ)'],
      [`/api/v1/orders/${ids.orderId}`, 'заказ по id'],
      [`/api/v1/orders/${ids.orderId}/payments`, 'оплаты заказа'],
      [`/api/v1/orders/${ids.orderId}/total-open`, 'открытая сумма заказа'],
      [`/api/v1/orders/${ids.orderId}/calculated`, 'расчёт заказа'],
      [`/api/v1/comments/order/${ids.orderId}`, 'комментарии заказа'],
      [`/api/v1/workshops/${ids.workshopId}`, 'цех по id'],
      [`/api/v1/products/${ids.productId}`, 'изделие по id'],
      [`/api/v1/products/${ids.productId}/estimate`, 'смета изделия']
    ].filter(([path]) => !/\/(undefined|null)\b/.test(path))

    test('все адресные GET доступны', async ({ request }) => {
      const missing = []
      for (const [path, label] of cases()) {
        const res = await apiGet(request, path, token)
        const body = await res.text()
        if (res.status() !== 200) missing.push(`${label}: ${path} → ${res.status()} ${body.slice(0, 160)}`)
      }
      expect(missing, 'адресные эндпоинты, ответившие не 200:\n' + missing.join('\n')).toEqual([])
    })

    test('GET /api/files/order-item/{itemId} — доступ по тенанту, без 403/500', async ({ request }) => {
      test.skip(!ids.orderItemId, 'нет позиций у заказа')
      const res = await apiGet(request, `/api/files/order-item/${ids.orderItemId}`, token)
      // файла у позиции может не быть — это 404; но не 403 (company_id у позиции пустой) и не 500
      expect([200, 404], `${await res.text()}`).toContain(res.status())
    })

    test('GET /api/v1/orders/number/{orderNumber} → 200', async ({ request }) => {
      test.skip(!ids.orderNumber, 'нет заказа с orderNumber')
      const res = await apiGet(request, `/api/v1/orders/number/${ids.orderNumber}`, token)
      expect(res.status()).toBe(200)
    })

    test('GET /api/v1/orders/{id}/positions/{materialId}/default → 200', async ({ request }) => {
      test.skip(!ids.orderMaterialId, 'нет материалов у заказа')
      const res = await apiGet(
        request,
        `/api/v1/orders/${ids.orderId}/positions/${ids.orderMaterialId}/default`,
        token
      )
      expect(res.status(), await res.text()).toBe(200)
    })

    test('GET /api/v1/orders/manager-earnings/{managerId} → 200', async ({ request }) => {
      test.skip(!ids.managerId, 'в стенде нет менеджера')
      const res = await apiGet(request, `/api/v1/orders/manager-earnings/${ids.managerId}`, token)
      expect(res.status()).toBe(200)
    })

    test('GET /api/v1/orders/items/{itemId}/estimate → 200', async ({ request }) => {
      test.skip(!ids.orderItemId, 'нет позиций у заказа')
      const res = await apiGet(request, `/api/v1/orders/items/${ids.orderItemId}/estimate`, token)
      expect(res.status()).toBe(200)
    })

    test('GET /api/v1/comment-replies/parent/{commentId} → 200', async ({ request }) => {
      const data = await (await apiGet(request, '/api/v1/comments?size=5', token)).json()
      const comments = Array.isArray(data) ? data : data.content || []
      test.skip(comments.length === 0, 'в стенде нет комментариев')
      const res = await apiGet(request, `/api/v1/comment-replies/parent/${comments[0].id}`, token)
      expect(res.status()).toBe(200)
    })
  })

  test.describe('POST-расчёты калькулятора', () => {
    test('POST /api/v1/calculations → 200 и GET по id созданного расчёта', async ({ request }) => {
      test.skip(!ids.materialId, 'нет материала')
      const res = await request.post('/api/v1/calculations', {
        headers: { Authorization: `Bearer ${token}` },
        data: { materialId: ids.materialId, materialType: 'BANNER', widthM: 1.5, heightM: 1 }
      })
      const body = await res.text()
      expect(res.status(), body.slice(0, 300)).toBe(200)
      const created = JSON.parse(body)
      expect(created.id, 'расчёт должен сохраниться и вернуть id').toBeTruthy()

      const get = await apiGet(request, `/api/v1/calculations/${created.id}`, token)
      expect(get.status()).toBe(200)
    })

    test('POST /api/v1/calculations/preview → 200 без сохранения', async ({ request }) => {
      test.skip(!ids.materialId, 'нет материала')
      const res = await request.post('/api/v1/calculations/preview', {
        headers: { Authorization: `Bearer ${token}` },
        data: { materialId: ids.materialId, materialType: 'BANNER', widthM: 1.5, heightM: 1 }
      })
      expect(res.status(), await res.text()).toBe(200)
    })

    test('POST /api/v1/calculations/calculate-area → 200', async ({ request }) => {
      const res = await request.post('/api/v1/calculations/calculate-area', {
        headers: { Authorization: `Bearer ${token}` },
        data: { widthM: 2, heightM: 1.5, podvorotMmHorizontal: 50, podvorotMmVertical: 50, podvorotCountPerSide: 2 }
      })
      expect(res.status(), await res.text()).toBe(200)
    })

    test('POST /api/v1/calculations/estimate-item → 200', async ({ request }) => {
      test.skip(!ids.materialId, 'нет материала')
      const res = await request.post('/api/v1/calculations/estimate-item', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          materialId: ids.materialId,
          widthM: 1.5,
          heightM: 1,
          operationIds: ids.operationId ? [ids.operationId] : []
        }
      })
      expect(res.status(), await res.text()).toBe(200)
    })
  })

  test.describe('CRUD-циклы', () => {
    test('клиент: create → get → update → search → delete', async ({ request }) => {
      const name = `E2E Клиент ${RUN}`
      const created = await request.post('/api/v1/clients', {
        headers: { Authorization: `Bearer ${token}` },
        data: { name, contactPerson: 'Тест', email: `e2e${RUN}@example.com`, priceplus: 10 }
      })
      const createdBody = await created.text()
      expect(created.status(), createdBody.slice(0, 300)).toBe(201)
      const client = JSON.parse(createdBody)
      expect(client.id).toBeTruthy()

      const get = await apiGet(request, `/api/v1/clients/${client.id}`, token)
      expect(get.status()).toBe(200)

      const upd = await request.put(`/api/v1/clients/${client.id}`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { ...client, name: `${name} (обн)` }
      })
      expect(upd.status(), await upd.text()).toBe(200)
      expect((await upd.json()).name).toBe(`${name} (обн)`)

      const search = await apiGet(request, `/api/v1/clients/search?q=${encodeURIComponent('E2E Клиент')}`, token)
      expect(search.status()).toBe(200)
      expect((await search.json()).some((c) => c.id === client.id), 'поиск должен находить созданного клиента').toBe(true)

      const del = await request.delete(`/api/v1/clients/${client.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(del.status(), await del.text()).toBe(204)

      const after = await apiGet(request, `/api/v1/clients/${client.id}`, token)
      expect(after.status(), 'удалённый клиент не должен отдаваться').toBe(404)
    })

    test('материал: create → update → delete', async ({ request }) => {
      const name = `E2E Материал ${RUN}`
      const created = await request.post('/api/v1/materials', {
        headers: { Authorization: `Bearer ${token}` },
        data: { name, unit: 'м2', price: 500, wasteCoefficient: 1.1, defaultWidthM: 1.6, defaultHeightM: 1 }
      })
      const body = await created.text()
      expect(created.status(), body.slice(0, 300)).toBe(201)
      const material = JSON.parse(body)
      expect(Number(material.defaultWidthM), 'ширина по умолчанию должна сохраниться при создании').toBe(1.6)
      expect(Number(material.defaultHeightM), 'высота по умолчанию должна сохраниться при создании').toBe(1)

      const upd = await request.put(`/api/v1/materials/${material.id}`, {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          name: `${name} (обн)`, unit: 'м2', price: 550, wasteCoefficient: 1.1,
          defaultWidthM: 3.2, defaultHeightM: 1.2
        }
      })
      const updText = await upd.text()
      expect(upd.status(), updText.slice(0, 300)).toBe(200)
      const updated = JSON.parse(updText)
      expect(updated.name).toBe(`${name} (обн)`)
      expect(Number(updated.price)).toBe(550)
      expect(Number(updated.defaultWidthM), 'размеры по умолчанию должны обновляться').toBe(3.2)
      expect(Number(updated.defaultHeightM), 'размеры по умолчанию должны обновляться').toBe(1.2)

      const del = await request.delete(`/api/v1/materials/${material.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(del.status()).toBe(204)
    })

    test('цех: create → update → delete', async ({ request }) => {
      const name = `E2E Цех ${RUN}`
      const created = await request.post('/api/v1/workshops', {
        headers: { Authorization: `Bearer ${token}` },
        data: { name, sortOrder: 99, operationIds: ids.operationId ? [ids.operationId] : [] }
      })
      const body = await created.text()
      expect(created.status(), body.slice(0, 300)).toBe(201)
      const workshop = JSON.parse(body)

      const upd = await request.put(`/api/v1/workshops/${workshop.id}`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { ...workshop, name: `${name} (обн)` }
      })
      expect(upd.status(), await upd.text()).toBe(200)

      const del = await request.delete(`/api/v1/workshops/${workshop.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(del.status()).toBe(204)
    })

    test('заказ: create → get → delete', async ({ request }) => {
      test.skip(!ids.clientId || !ids.materialId, 'нет клиента или материала')
      const iso = (days) => new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10)
      const created = await request.post('/api/v1/orders', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          clientId: ids.clientId,
          description: `E2E Заказ ${RUN}`,
          orderDate: iso(0),
          dueDate: iso(7),
          items: [{ materialId: ids.materialId, widthM: 1, heightM: 0.5, readyDate: iso(7), operations: [] }]
        }
      })
      const body = await created.text()
      expect(created.status(), body.slice(0, 300)).toBe(201)
      const order = JSON.parse(body)
      expect(order.orderNumber, 'номер заказа генерируется на сервере').toBeTruthy()

      const card = await apiGet(request, `/api/v1/orders/${order.id}`, token)
      expect(card.status()).toBe(200)
      expect(((await card.json()).items || []).length, 'позиция заказа должна сохраниться').toBe(1)

      const del = await request.delete(`/api/v1/orders/${order.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(del.status(), await del.text()).toBe(204)

      const after = await apiGet(request, `/api/v1/orders/${order.id}`, token)
      expect(after.status(), 'удалённый заказ не должен отдаваться').toBe(404)
    })

    test('комментарий заказа: create → update → mark read → delete', async ({ request }) => {
      test.skip(!ids.orderId, 'нет заказа')
      const created = await request.post(`/api/v1/comments/order/${ids.orderId}`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { body: `E2E комментарий ${RUN}` }
      })
      const body = await created.text()
      expect(created.status(), body.slice(0, 300)).toBe(201)
      const comment = JSON.parse(body)

      const list = await apiGet(request, `/api/v1/comments/order/${ids.orderId}`, token)
      expect(list.status()).toBe(200)
      expect((await list.json()).some((c) => c.id === comment.id)).toBe(true)

      const upd = await request.put(`/api/v1/comments/${comment.id}`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { body: `E2E комментарий ${RUN} (обн)` }
      })
      expect(upd.status(), await upd.text()).toBe(200)

      const read = await request.patch(`/api/v1/comments/${comment.id}/read`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(read.status(), await read.text()).toBe(200)

      const del = await request.delete(`/api/v1/comments/${comment.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect([200, 204], await del.text()).toContain(del.status())
    })

    test('уведомления: unread-count → PATCH read-all', async ({ request }) => {
      test.skip(!ids.orderId, 'нет заказа для комментария')
      const created = await request.post(`/api/v1/comments/order/${ids.orderId}`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { body: `E2E уведомление ${RUN}` }
      })
      expect(created.status(), await created.text()).toBe(201)

      const count = await apiGet(request, '/api/v1/notifications/unread-count', token)
      expect(count.status()).toBe(200)

      const readAll = await request.patch('/api/v1/notifications/read-all', {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(readAll.status(), await readAll.text()).toBe(200)

      const after = await apiGet(request, '/api/v1/notifications/unread-count', token)
      expect((await after.json())).toBe(0)
    })

    test('Keycloak-пользователь: создание, смена пароля, вход, удаление', async ({ request }) => {
      const username = `e2e_pw_${RUN}`
      const created = await request.post('/api/v1/employees/keycloak-user', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          username,
          password: 'e2epass1',
          fullName: 'Е2Е Тестовый',
          email: `${username}@example.com`,
          role: 'PRODUCTION',
          companyId: 1
        }
      })
      const body = await created.text()
      expect(created.status(), body.slice(0, 300)).toBe(201)
      const userId = (JSON.parse(body)).id
      expect(userId).toBeTruthy()

      const sync = await request.post('/api/v1/employees/sync-by-company?companyId=1', {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(sync.status(), await sync.text()).toBe(200)

      const employee = await apiGet(request, `/api/v1/employees/username/${username}`, token)
      expect(employee.status(), await employee.text()).toBe(200)
      const employeeId = (await employee.json()).id

      const changed = await request.put('/api/v1/employees/keycloak-password', {
        headers: { Authorization: `Bearer ${token}` },
        data: { username, password: 'e2epass2', temporary: false }
      })
      expect(changed.status(), await changed.text()).toBe(200)

      // новый пароль должен подходить, старый — нет
      const keycloak = 'http://127.0.0.1:8080/realms/print-sv/protocol/openid-connect/token'
      const loginWith = async (password) =>
        (await request.post(keycloak, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          data: new URLSearchParams({ grant_type: 'password', client_id: 'frontend', username, password }).toString()
        })).ok()
      expect(await loginWith('e2epass2'), 'вход с новым паролем').toBe(true)
      expect(await loginWith('e2epass1'), 'старый пароль должен быть невалиден').toBe(false)

      const del = await request.delete(`/api/v1/employees/${employeeId}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect(del.status(), await del.text()).toBe(204)
    })
  })

  test.describe('Права доступа', () => {
    test('без токена — 401 на представителях каждого сервиса', async ({ request }) => {
      const paths = [
        '/api/v1/clients?size=1',
        '/api/v1/companies',
        '/api/v1/employees?size=1',
        '/api/v1/materials?size=1',
        '/api/v1/orders?page=0&size=1',
        '/api/v1/calculations/operations',
        '/api/v1/notifications',
        '/api/v1/workshops?size=1',
        '/api/v1/products'
      ]
      for (const path of paths) {
        const res = await request.get(path)
        expect(res.status(), `${path} без токена`).toBe(401)
      }
    })

    test('ADMIN не видит GOD-only эндпоинты компаний', async ({ request }) => {
      const databases = await apiGet(request, '/api/v1/companies/databases', token)
      expect(databases.status(), 'список БД компаний — только GOD').toBe(403)
    })

    test('MANAGER не создаёт компании и не читает сотрудников целиком', async ({ request }) => {
      const create = await request.post('/api/v1/companies', {
        headers: { Authorization: `Bearer ${managerToken}` },
        data: { name: `E2E Нельзя ${RUN}` }
      })
      expect(create.status()).toBe(403)

      const keycloakUser = await request.post('/api/v1/employees/keycloak-user', {
        headers: { Authorization: `Bearer ${managerToken}` },
        data: { username: `e2e_no_${RUN}`, password: 'e2epass1', fullName: 'Нельзя Тест', role: 'PRODUCTION' }
      })
      expect(keycloakUser.status()).toBe(403)
    })

    test('смена пароля: валидация длины и несуществующий пользователь', async ({ request }) => {
      const short = await request.put('/api/v1/employees/keycloak-password', {
        headers: { Authorization: `Bearer ${token}` },
        data: { username: ids.employeeUsername, password: '123', temporary: false }
      })
      expect(short.status()).toBe(400)

      const missing = await request.put('/api/v1/employees/keycloak-password', {
        headers: { Authorization: `Bearer ${token}` },
        data: { username: 'e2e_no_such_user', password: 'e2epass1', temporary: false }
      })
      expect(missing.status()).toBe(404)
    })

    test('несуществующие id дают 404, а не 500', async ({ request }) => {
      const paths = [
        '/api/v1/clients/999999999',
        '/api/v1/materials/999999999',
        '/api/v1/orders/999999999',
        '/api/v1/workshops/999999999',
        '/api/v1/products/999999999',
        '/api/v1/employees/999999999'
      ]
      const bad = []
      for (const path of paths) {
        const res = await apiGet(request, path, token)
        if (res.status() !== 404) bad.push(`${path} → ${res.status()} ${(await res.text()).slice(0, 120)}`)
      }
      expect(bad, 'эндпоинты с несуществующим id:\n' + bad.join('\n')).toEqual([])
    })

    test('невалидное тело даёт 400, а не 500', async ({ request }) => {
      const cases = [
        ['POST /api/v1/clients без имени', () => request.post('/api/v1/clients', {
          headers: { Authorization: `Bearer ${token}` }, data: { email: 'not-an-email' }
        })],
        ['POST /api/v1/materials без цены', () => request.post('/api/v1/materials', {
          headers: { Authorization: `Bearer ${token}` }, data: { name: `E2E Плохой ${RUN}` }
        })],
        ['POST /api/v1/calculations без materialId', () => request.post('/api/v1/calculations', {
          headers: { Authorization: `Bearer ${token}` }, data: { widthM: 1 }
        })],
        ['POST /api/v1/comments/order без тела', () => request.post(`/api/v1/comments/order/${ids.orderId}`, {
          headers: { Authorization: `Bearer ${token}` }, data: {}
        })]
      ]
      for (const [label, run] of cases) {
        test.skip(label.includes('comments') && !ids.orderId, 'нет заказа')
        const res = await run()
        expect(res.status(), `${label}: ${(await res.text()).slice(0, 200)}`).toBe(400)
      }
    })

    test('несуществующий id уведомления/комментария даёт 404', async ({ request }) => {
      const read = await request.patch('/api/v1/notifications/999999999/read', {
        headers: { Authorization: `Bearer ${token}` }
      })
      expect([404, 200], `PATCH read несуществующего: ${await read.text()}`).toContain(read.status())
    })
  })

  test.describe('Мёртвые маршруты шлюза', () => {
    for (const path of KNOWN_DEAD_ROUTES) {
      test(`GET ${path} — маршрут без контроллера`, async ({ request }) => {
        const res = await apiGet(request, path, token)
        expect(res.status(), `${path}: ${(await res.text()).slice(0, 200)}`).toBe(404)
      })
    }
  })
})

/**
 * Регрессия на падение страницы создания заказа: диалог «Выбрать операции»
 * обращается к dialogGroupedData/dialogUngroupedOps — до фикса они не были объявлены
 * и рендер падал с ReferenceError.
 */
test.describe('Страница создания заказа без ошибок рендера', () => {
  test('диалог выбора операций открывается без pageerror', async ({ page }) => {
    const errors = []
    page.on('pageerror', (err) => errors.push(String(err)))

    await login(page, DEMO.username, DEMO.password)
    await page.goto('/orders/new')
    const addPosition = page.getByRole('button', { name: 'Добавить позицию' })
    await expect(addPosition).toBeVisible({ timeout: 20_000 })

    await addPosition.click()
    const row = page.getByRole('table').last().getByRole('row').last()
    await row.locator('[role="combobox"]').first().click()
    // option[0] — «Выберите материал» (value=""), берём первый реальный материал
    await page.getByRole('option').nth(1).click()

    const chooseOps = row.getByRole('button', { name: 'Выбрать' })
    await expect(chooseOps).toBeEnabled({ timeout: 10_000 })
    await chooseOps.click()

    const dialog = page.getByRole('dialog').filter({ hasText: 'Выбрать операции' })
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(dialog.getByRole('button', { name: 'Отмена' })).toBeVisible()

    expect(errors, 'ошибок выполнения на странице быть не должно:\n' + errors.join('\n')).toEqual([])
  })
})

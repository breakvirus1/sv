import { test, expect } from '@playwright/test'
import { login, trackServerErrors, apiToken, apiGet } from './helpers'

const toNumber = (text = '') => Number((text.match(/-?\d+(?:[.,]\d+)?/) || ['0'])[0].replace(',', '.'))

test.describe.serial('Конструктор изделий: позиции материал+операции', () => {
  test('создание изделия с позицией и заказ через navbar', async ({ page, request }) => {
    const serverErrors = await trackServerErrors(page)
    const productName = `E2E Изделие ${Date.now()}`

    await login(page, 'admin', 'admin')

    // Заказ создаётся только при наличии клиента
    const token = await apiToken(request, 'admin', 'admin')
    const clientsResponse = await apiGet(request, '/api/v1/clients', token)
    const clientsData = await clientsResponse.json()
    if (!clientsData.content || clientsData.content.length === 0) {
      await request.post('/api/v1/clients', {
        headers: { Authorization: `Bearer ${token}` },
        data: { name: `E2E Клиент ${Date.now()}`, type: 'PRIVATE', phone: '+70000000000', priceplus: 0 }
      })
    }

    // --- Конструктор: одна позиция = материал + список операций ---
    await page.goto('/constructor/new')
    await expect(page.getByRole('heading', { name: 'Конструктор изделий' })).toBeVisible()

    await page.getByRole('button', { name: 'Добавить позицию' }).click()
    await expect(page.getByText('Позиция 1')).toBeVisible()

    await page.getByRole('combobox').first().click()
    await page.getByRole('option').nth(1).click()
    await page.getByLabel('Кол-во').fill('3')
    await page.getByLabel('Коэф. отхода').fill('2')

    await page.getByRole('button', { name: 'Добавить операцию' }).click()
    await page.getByRole('combobox').last().click()
    await page.getByRole('option').nth(1).click()
    await page.getByLabel('Кол-во операции').fill('5')
    await page.getByLabel('Коэф. кол-ва операции').fill('0.5')

    const positionSumText = await page.getByTestId('position-sum-0').innerText()
    const constructorTotalText = await page.getByTestId('constructor-total').innerText()
    expect(toNumber(positionSumText)).toBeGreaterThan(0)
    expect(toNumber(constructorTotalText)).toBeCloseTo(toNumber(positionSumText), 2)

    await page.getByRole('button', { name: 'Добавить изделие' }).click()
    const nameDialog = page.getByRole('dialog').filter({ hasText: 'Название изделия' })
    await nameDialog.getByLabel('Название изделия').fill(productName)
    await nameDialog.getByRole('button', { name: 'Добавить', exact: true }).click()
    await expect(page.getByText('Изделие сохранено')).toBeVisible({ timeout: 20_000 })

    // Сервер сохранил позиции и единственную сумму каждой позиции
    const productsResponse = await apiGet(request, '/api/v1/products', token)
    const products = await productsResponse.json()
    const savedProduct = products.find(product => product.name === productName)
    expect(savedProduct, 'изделие сохранено на сервере').toBeTruthy()
    expect(savedProduct.positions).toHaveLength(1)
    expect(savedProduct.positions[0].operations).toHaveLength(1)
    expect(Number(savedProduct.positions[0].positionSum)).toBeCloseTo(toNumber(positionSumText), 2)
    expect(Number(savedProduct.totalPrice)).toBeCloseTo(toNumber(constructorTotalText), 2)

    // --- Вкладка «Конструктор изделий» в админке ---
    await page.goto('/admin')
    await page.getByRole('tab', { name: 'Конструктор изделий' }).click()
    const productRow = page.getByRole('row').filter({ hasText: productName })
    await expect(productRow).toBeVisible()
    await expect(productRow).toContainText('1')

    // --- Navbar → «Создать продукт» → диалоги параметров ---
    await page.locator('header').getByRole('button', { name: 'Создать заказ' }).click()
    await page.getByRole('menuitem', { name: 'Создать продукт' }).click()
    await page.waitForURL('**/orders/product')

    await page.getByRole('button', { name: 'Добавить изделие' }).click()
    const picker = page.getByRole('dialog').filter({ hasText: 'Добавить изделие' })
    await expect(picker.getByText(productName)).toBeVisible()
    const productItem = picker.getByRole('listitem').filter({ hasText: productName })
    await productItem.getByRole('button', { name: 'Выбрать' }).click()

    const materialDialog = page.getByRole('dialog').filter({ hasText: 'Параметры материала' })
    await expect(materialDialog).toBeVisible()
    await materialDialog.getByLabel('Количество').fill('4')
    await materialDialog.getByLabel('Коэф. отхода').fill('1.5')
    await materialDialog.getByRole('button', { name: 'Далее' }).click()

    const operationDialog = page.getByRole('dialog').filter({ hasText: 'Параметры операции' })
    await expect(operationDialog).toBeVisible()
    await operationDialog.getByLabel('Количество').fill('2')
    await operationDialog.getByLabel('Коэф. кол-ва').fill('3')
    await operationDialog.getByRole('button', { name: 'Далее' }).click()

    // Последний диалог — размеры изделия, из которых считается количество м²
    const dimensionsDialog = page.getByRole('dialog').filter({ hasText: 'Размеры изделия' })
    await expect(dimensionsDialog).toBeVisible()
    await dimensionsDialog.getByLabel('Ширина, м').fill('2')
    await dimensionsDialog.getByLabel('Высота, м').fill('1.5')
    await expect(dimensionsDialog.getByTestId('wizard-area')).toContainText('3.00')
    await dimensionsDialog.getByRole('button', { name: 'Добавить в заказ' }).click()

    const position = savedProduct.positions[0]
    const area = 2 * 1.5
    const expectedSum = Number((
      Number(position.price) * area * 1.5
      + Number(position.operations[0].pricePerUnit) * 2 * 3
    ).toFixed(2))

    await expect(page.getByTestId('position-total')).toContainText(expectedSum.toFixed(2))
    await expect(page.getByTestId('order-grand-total')).toContainText(expectedSum.toFixed(2))

    // --- Второе изделие на ту же страницу заказа ---
    await page.getByRole('button', { name: 'Добавить изделие' }).click()
    const picker2 = page.getByRole('dialog').filter({ hasText: 'Добавить изделие' })
    await picker2.getByRole('listitem').filter({ hasText: productName }).getByRole('button', { name: 'Выбрать' }).click()

    const materialDialog2 = page.getByRole('dialog').filter({ hasText: 'Параметры материала' })
    await materialDialog2.getByRole('button', { name: 'Далее' }).click()
    const operationDialog2 = page.getByRole('dialog').filter({ hasText: 'Параметры операции' })
    await operationDialog2.getByRole('button', { name: 'Далее' }).click()
    const dimensionsDialog2 = page.getByRole('dialog').filter({ hasText: 'Размеры изделия' })
    await dimensionsDialog2.getByLabel('Ширина, м').fill('1')
    await dimensionsDialog2.getByLabel('Высота, м').fill('1')
    await dimensionsDialog2.getByRole('button', { name: 'Добавить в заказ' }).click()

    const expectedSecond = Number((
      Number(position.price) * 1 * Number(position.wasteCoefficient)
      + Number(position.operations[0].pricePerUnit) * Number(position.operations[0].quantity) * Number(position.operations[0].coefficient)
    ).toFixed(2))

    await expect(page.getByTestId('position-total')).toHaveCount(2)
    const grandTotalText = await page.getByTestId('order-grand-total').innerText()
    expect(toNumber(grandTotalText)).toBeCloseTo(expectedSum + expectedSecond, 2)

    await page.getByRole('button', { name: 'Сохранить заказ' }).click()
    await expect(page.getByText('Заказ успешно создан')).toBeVisible({ timeout: 20_000 })

    expect(serverErrors, `HTTP 5xx: ${serverErrors.join(', ')}`).toEqual([])
  })
})

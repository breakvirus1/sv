import { test, expect } from '@playwright/test';

const BASE = process.env.BASE_URL || 'http://localhost:5174';

test('admin panel shows seeded materials and operations from reklama1 price list', async ({ page }) => {
  await page.goto(BASE);
  await page.getByLabel(/логин|username/i).fill('adminsv1');
  await page.getByLabel(/пароль|password/i).fill('111111');
  await page.getByRole('button', { name: /войти|login/i }).click();
  await page.waitForURL(/orders|admin|\/$/i, { timeout: 30000 });

  await page.goto(`${BASE}/admin`);
  await expect(page.getByRole('heading', { name: /админ/i })).toBeVisible({ timeout: 15000 });

  // Materials tab (MUI Tabs)
  await page.getByRole('tab', { name: /материалы/i }).click();
  await page.waitForTimeout(1500);
  await expect(page.getByText('Бумага BlueBack (для постеров)')).toBeVisible({ timeout: 15000 });
  await expect(page.getByText('Плёнка 641 (белая, прозрачная)')).toBeVisible();
  await page.screenshot({ path: '/tmp/opencode/shots/admin-materials-reklama1.png', fullPage: true });

  // Operations tab
  await page.getByRole('tab', { name: /операции/i }).click();
  await page.waitForTimeout(1500);
  await expect(page.getByText('Спайка баннера')).toBeVisible({ timeout: 15000 });
  await expect(page.getByText('УФ-печать лаком')).toBeVisible();
  await page.screenshot({ path: '/tmp/opencode/shots/admin-operations-reklama1.png', fullPage: true });
});

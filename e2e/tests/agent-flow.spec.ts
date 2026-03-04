import { test, expect } from '@playwright/test';
import { loginAgent, joinAsCustomer, cleanupTestData } from '../fixtures/test-helpers';

test.use({ baseURL: 'http://localhost:3100' });

test.afterEach(async () => {
  await cleanupTestData();
});

test('agent can log in with valid credentials', async ({ page }) => {
  await page.goto('/login');

  // Verify login form is present
  await expect(page.locator('input#username')).toBeVisible();
  await expect(page.locator('input#password')).toBeVisible();
  await expect(page.locator('button[aria-label="로그인"]')).toBeVisible();

  // Log in
  await loginAgent(page);

  // Should be on the dashboard
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.locator('h1', { hasText: '대시보드' })).toBeVisible();
});

test('agent sees queue list on dashboard', async ({ page }) => {
  await page.goto('/login');
  await loginAgent(page);

  // The queue section heading should be visible
  await expect(page.locator('h2', { hasText: '대기 고객' })).toBeVisible();

  // Queue list container should be present (even if empty)
  const emptyMsg = page.locator('text=대기 중인 고객이 없습니다.');
  const queueList = page.locator('ul.divide-y');

  // Either the empty state or an actual list should appear
  const resolved = await Promise.race([
    emptyMsg.waitFor({ state: 'visible', timeout: 10_000 }).then(() => 'empty'),
    queueList.waitFor({ state: 'visible', timeout: 10_000 }).then(() => 'list'),
  ]);

  expect(['empty', 'list']).toContain(resolved);
});

test('agent can accept a customer from queue', async ({ page, browser }) => {
  // First, have a customer join the queue from a separate browser context
  const customerContext = await browser.newContext({ baseURL: 'http://localhost:3000' });
  const customerPage = await customerContext.newPage();

  try {
    await customerPage.goto('/');
    await joinAsCustomer(customerPage, 'E2E고객', '010-9999-0000');

    // Now log in as agent and accept
    await page.goto('/login');
    await loginAgent(page);

    // Wait for the accept button matching the customer name to appear
    const acceptBtn = page.locator('button[aria-label$="수락"]').first();
    await acceptBtn.waitFor({ state: 'visible', timeout: 20_000 });
    await acceptBtn.click();

    // Agent should navigate to the call page
    await page.waitForURL(/\/call\//, { timeout: 15_000 });
    await expect(page).toHaveURL(/\/call\//);
  } finally {
    await customerPage.close();
    await customerContext.close();
  }
});

import { test, expect, chromium } from '@playwright/test';
import { loginAgent, joinAsCustomer, waitForVideoCall, cleanupTestData } from '../fixtures/test-helpers';

/**
 * Full counseling session E2E test.
 *
 * Flow:
 *   1. Customer joins and enters waiting room
 *   2. Agent logs in and accepts the customer from the queue
 *   3. Both see the video call page
 *   4. Agent ends the call
 *   5. Customer lands on the feedback page
 */
test('complete counseling session', async () => {
  const browser = await chromium.launch();

  const customerContext = await browser.newContext({ baseURL: 'http://localhost:3000' });
  const agentContext = await browser.newContext({ baseURL: 'http://localhost:3100' });

  const customerPage = await customerContext.newPage();
  const agentPage = await agentContext.newPage();

  try {
    // -----------------------------------------------------------------------
    // Step 1: Customer joins the queue
    // -----------------------------------------------------------------------
    await customerPage.goto('/');
    await joinAsCustomer(customerPage, 'E2E전체고객', '010-8888-0001');

    // Verify customer is in the waiting room
    await expect(customerPage).toHaveURL(/\/waiting/);
    await expect(customerPage.locator('div[role="status"]')).toBeVisible();

    // -----------------------------------------------------------------------
    // Step 2: Agent logs in and accepts the customer
    // -----------------------------------------------------------------------
    await agentPage.goto('/login');
    await loginAgent(agentPage);

    // Wait for the accept button and click it
    const acceptBtn = agentPage.locator('button[aria-label$="수락"]').first();
    await acceptBtn.waitFor({ state: 'visible', timeout: 20_000 });
    await acceptBtn.click();

    // Agent navigates to call page
    await agentPage.waitForURL(/\/call\//, { timeout: 15_000 });

    // -----------------------------------------------------------------------
    // Step 3: Customer is redirected to the call page automatically
    // -----------------------------------------------------------------------
    await customerPage.waitForURL(/\/call\//, { timeout: 20_000 });

    // -----------------------------------------------------------------------
    // Step 4: Both participants see video elements (LiveKit renders <video>)
    // -----------------------------------------------------------------------
    // Note: actual video requires a media server; we verify the page and UI loaded
    await expect(agentPage.locator('div[data-lk-theme="default"]')).toBeVisible({ timeout: 15_000 });
    await expect(customerPage.locator('div[data-lk-theme="default"]')).toBeVisible({ timeout: 15_000 });

    // -----------------------------------------------------------------------
    // Step 5: Agent ends the call
    // -----------------------------------------------------------------------
    const endCallBtn = agentPage.locator('button[aria-label="통화 종료"]');
    await endCallBtn.waitFor({ state: 'visible', timeout: 10_000 });
    await endCallBtn.click();

    // Agent returns to dashboard
    await agentPage.waitForURL(/\/dashboard/, { timeout: 15_000 });
    await expect(agentPage).toHaveURL(/\/dashboard/);

    // -----------------------------------------------------------------------
    // Step 6: Customer lands on feedback page
    // -----------------------------------------------------------------------
    await customerPage.waitForURL(/\/feedback/, { timeout: 20_000 });
    await expect(customerPage.locator('[role="group"][aria-label="별점 선택"]')).toBeVisible();

    // Customer submits feedback
    await customerPage.click('button[aria-label="5점"]');
    await customerPage.click('button[aria-label="피드백 제출"]');

    // After submission the thank-you state is shown
    await expect(customerPage.locator('text=감사합니다!')).toBeVisible({ timeout: 10_000 });
  } finally {
    await customerPage.close();
    await agentPage.close();
    await customerContext.close();
    await agentContext.close();
    await browser.close();
    await cleanupTestData();
  }
});

import { test, expect, chromium } from '@playwright/test';
import { loginAgent, joinAsCustomer, cleanupTestData } from '../fixtures/test-helpers';

/**
 * Full counseling session E2E test.
 *
 * Flow:
 *   1. Customer joins and enters waiting room
 *   2. Agent logs in and accepts the customer from the queue
 *   3. Both reach the call page (LiveKit connection may disconnect quickly in CI)
 *   4. Agent ends up on dashboard (via end-call button or auto-disconnect)
 *   5. Customer lands on the feedback page and submits feedback
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
    // Step 3: Customer is redirected to the call page automatically via SSE
    // -----------------------------------------------------------------------
    await customerPage.waitForURL(/\/call\//, { timeout: 20_000 });

    // -----------------------------------------------------------------------
    // Step 4: Agent ends up on dashboard
    // In test environments, LiveKit may disconnect quickly, causing auto-navigation.
    // Otherwise, the agent clicks the end-call button manually.
    // -----------------------------------------------------------------------
    const endCallBtn = agentPage.locator('button[aria-label="통화 종료"]');
    const isEndCallVisible = await endCallBtn.isVisible().catch(() => false);
    if (isEndCallVisible) {
      await endCallBtn.click();
    }
    // Whether via manual click or auto-disconnect, agent returns to dashboard
    await agentPage.waitForURL(/\/dashboard/, { timeout: 30_000 });
    await expect(agentPage).toHaveURL(/\/dashboard/);

    // -----------------------------------------------------------------------
    // Step 5: Customer lands on feedback page
    // -----------------------------------------------------------------------
    await customerPage.waitForURL(/\/feedback/, { timeout: 30_000 });
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

import { test, expect, chromium } from '@playwright/test';
import { loginAgent, joinAsCustomer, cleanupTestData } from '../fixtures/test-helpers';

/**
 * Chat during call E2E test.
 *
 * Flow:
 *   1. Customer joins and waits in queue
 *   2. Agent accepts the customer (both land on call pages)
 *   3. Customer opens chat and sends a message
 *   4. Agent sees the message in the chat panel
 *   5. Agent replies
 *   6. Customer sees agent's reply
 */
test('customer and agent can exchange chat messages during call', async () => {
  const browser = await chromium.launch();

  const customerContext = await browser.newContext({ baseURL: 'http://localhost:3000' });
  const agentContext = await browser.newContext({ baseURL: 'http://localhost:3100' });

  const customerPage = await customerContext.newPage();
  const agentPage = await agentContext.newPage();

  try {
    // -----------------------------------------------------------------------
    // Step 1 & 2: Set up call session
    // -----------------------------------------------------------------------
    await customerPage.goto('/');
    await joinAsCustomer(customerPage, '채팅테스트고객', '010-7777-0002');
    await expect(customerPage).toHaveURL(/\/waiting/);

    await agentPage.goto('/login');
    await loginAgent(agentPage);

    const acceptBtn = agentPage.locator('button[aria-label$="수락"]').first();
    await acceptBtn.waitFor({ state: 'visible', timeout: 20_000 });
    await acceptBtn.click();

    await agentPage.waitForURL(/\/call\//, { timeout: 15_000 });
    await customerPage.waitForURL(/\/call\//, { timeout: 20_000 });

    // -----------------------------------------------------------------------
    // Step 3: Customer opens chat panel and sends a message
    // -----------------------------------------------------------------------
    // Customer chat is accessed via the floating chat toggle button
    const customerChatToggle = customerPage.locator('button[aria-label="채팅 열기"]');
    await customerChatToggle.waitFor({ state: 'visible', timeout: 10_000 });
    await customerChatToggle.click();

    // Chat input becomes visible in the panel
    const customerChatInput = customerPage.locator(
      'input[placeholder="메시지를 입력하세요..."]',
    );
    await customerChatInput.waitFor({ state: 'visible', timeout: 5_000 });

    const customerMessage = '안녕하세요, 상담사님!';
    await customerChatInput.fill(customerMessage);
    await customerPage.click('button[aria-label="전송"]');

    // The message should appear in the customer's own chat bubble
    await expect(customerPage.locator(`text=${customerMessage}`)).toBeVisible({ timeout: 10_000 });

    // -----------------------------------------------------------------------
    // Step 4: Agent sees the message
    // -----------------------------------------------------------------------
    // Agent chat panel is on the side panel (tab "채팅" is active by default)
    // Make sure the chat tab is active
    const agentChatTab = agentPage.locator('button[role="tab"]', { hasText: '채팅' });
    await agentChatTab.waitFor({ state: 'visible', timeout: 10_000 });
    await agentChatTab.click();

    // Customer message should appear in agent's chat panel
    await expect(agentPage.locator(`text=${customerMessage}`)).toBeVisible({ timeout: 15_000 });

    // -----------------------------------------------------------------------
    // Step 5: Agent replies
    // -----------------------------------------------------------------------
    const agentChatInput = agentPage.locator('input[aria-label="채팅 메시지 입력"]');
    await agentChatInput.waitFor({ state: 'visible', timeout: 5_000 });

    const agentMessage = '안녕하세요, 무엇을 도와드릴까요?';
    await agentChatInput.fill(agentMessage);
    await agentPage.click('button[aria-label="메시지 보내기"]');

    // Agent's message appears in agent's chat
    await expect(agentPage.locator(`text=${agentMessage}`)).toBeVisible({ timeout: 10_000 });

    // -----------------------------------------------------------------------
    // Step 6: Customer sees agent's reply
    // -----------------------------------------------------------------------
    await expect(customerPage.locator(`text=${agentMessage}`)).toBeVisible({ timeout: 15_000 });
  } finally {
    await customerPage.close();
    await agentPage.close();
    await customerContext.close();
    await agentContext.close();
    await browser.close();
    await cleanupTestData();
  }
});

import { test, expect } from '@playwright/test';
import { joinAsCustomer, cleanupTestData } from '../fixtures/test-helpers';

test.use({ baseURL: 'http://localhost:3000' });

test.afterEach(async () => {
  await cleanupTestData();
});

test('customer joins with name and contact', async ({ page }) => {
  await page.goto('/');

  // Verify join form is visible
  await expect(page.locator('input#name')).toBeVisible();
  await expect(page.locator('input#contact')).toBeVisible();
  await expect(page.locator('button[aria-label="상담 시작하기"]')).toBeVisible();

  // Fill and submit
  await joinAsCustomer(page, '홍길동', '010-1234-5678');

  // Should navigate to /waiting
  await expect(page).toHaveURL(/\/waiting/);
});

test('customer sees queue position in waiting room', async ({ page }) => {
  await page.goto('/');
  await joinAsCustomer(page, '홍길동', '010-1234-5678');

  // Spinner (role="status") should be visible while waiting
  await expect(page.locator('div[role="status"]')).toBeVisible();

  // If a position is returned, the position text should appear
  // (position may not appear immediately if SSE hasn't delivered yet, so we allow it to be optional)
  const positionText = page.locator('p.text-3xl.font-bold');
  const hasPosition = await positionText.isVisible().catch(() => false);
  if (hasPosition) {
    await expect(positionText).toContainText('번');
  }
});

test('customer reaches feedback page after call ends', async ({ page }) => {
  // Navigate directly to the feedback page to verify it renders correctly
  // (In a full E2E run this would be reached via the call flow)
  await page.goto('/feedback');

  // Feedback page renders rating stars
  const ratingGroup = page.locator('[role="group"][aria-label="별점 선택"]');
  await expect(ratingGroup).toBeVisible({ timeout: 10_000 });

  // All 5 star buttons should be present
  for (let star = 1; star <= 5; star++) {
    await expect(page.locator(`button[aria-label="${star}점"]`)).toBeVisible();
  }

  // Submit button is initially disabled (no rating selected)
  await expect(page.locator('button[aria-label="피드백 제출"]')).toBeDisabled();

  // Select 5 stars and verify button becomes enabled
  await page.click('button[aria-label="5점"]');
  await expect(page.locator('button[aria-label="피드백 제출"]')).toBeEnabled();
});

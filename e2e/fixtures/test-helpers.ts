import { Page } from '@playwright/test';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';

// ---------------------------------------------------------------------------
// Agent helpers
// ---------------------------------------------------------------------------

/**
 * Log in as an agent via the login form.
 * Waits for navigation to /dashboard after successful login.
 */
export async function loginAgent(
  page: Page,
  username = 'test-agent',
  password = 'test-password',
): Promise<void> {
  await page.fill('input#username', username);
  await page.fill('input#password', password);
  await page.click('button[aria-label="로그인"]');
  await page.waitForURL('**/dashboard', { timeout: 15_000 });
}

// ---------------------------------------------------------------------------
// Customer helpers
// ---------------------------------------------------------------------------

/**
 * Join the counseling queue as a customer.
 * Waits for navigation to /waiting after successful form submission.
 */
export async function joinAsCustomer(
  page: Page,
  name: string,
  contact: string,
): Promise<void> {
  await page.fill('input#name', name);
  await page.fill('input#contact', contact);
  await page.click('button[aria-label="상담 시작하기"]');
  await page.waitForURL('**/waiting', { timeout: 15_000 });
}

// ---------------------------------------------------------------------------
// Call helpers
// ---------------------------------------------------------------------------

/**
 * Wait until the LiveKit video element is visible on the call page.
 */
export async function waitForVideoCall(page: Page): Promise<void> {
  // LiveKit renders a <video> element when the room is connected.
  await page.waitForSelector('video', { state: 'visible', timeout: 30_000 });
}

// ---------------------------------------------------------------------------
// API helpers
// ---------------------------------------------------------------------------

/**
 * Seed a test agent account via the backend API.
 * Returns silently if the agent already exists (409).
 */
export async function seedTestAgent(
  username = 'test-agent',
  password = 'test-password',
  name = '테스트 상담사',
): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/api/agents`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, name }),
  });

  if (res.status === 409) {
    return; // already exists
  }

  if (!res.ok) {
    const body = await res.text();
    throw new Error(`seedTestAgent failed: ${res.status} ${body}`);
  }
}

/**
 * Clean up E2E test data after a test run.
 * Calls the backend cleanup endpoint if available.
 */
export async function cleanupTestData(): Promise<void> {
  try {
    await fetch(`${BACKEND_URL}/api/test/cleanup`, { method: 'DELETE' });
  } catch {
    // Cleanup endpoint is optional — swallow errors
  }
}

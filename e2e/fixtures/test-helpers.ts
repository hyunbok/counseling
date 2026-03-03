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
 * Verify test agent exists by attempting login.
 * The test agent must be seeded in the database before running E2E tests.
 * See docs/tasks/v1-25-e2e-testing-performance-plan.md for DB seeding instructions.
 */
export async function seedTestAgent(
  username = 'test-agent',
  password = 'test-password',
): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Id': 'default',
    },
    body: JSON.stringify({ username, password }),
  });

  if (res.ok) {
    return; // agent exists and can log in
  }

  const body = await res.text();
  throw new Error(
    `Test agent verification failed (${res.status}): ${body}. ` +
      'Ensure the test agent is seeded in the tenant DB.',
  );
}

/**
 * Clear all entries from the queue by fetching and deleting each.
 */
export async function clearQueue(): Promise<void> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/queue`, {
      headers: {
        'X-Tenant-Id': 'default',
        Authorization: `Bearer ${await getAgentToken()}`,
      },
    });
    if (!res.ok) return;
    const entries: { entryId: string }[] = await res.json();
    await Promise.all(
      entries.map((e) =>
        fetch(`${BACKEND_URL}/api/queue/${e.entryId}`, {
          method: 'DELETE',
          headers: { 'X-Tenant-Id': 'default' },
        }),
      ),
    );
  } catch {
    // Best-effort cleanup
  }
}

/**
 * Reset agent status to ONLINE by logging in.
 */
async function getAgentToken(): Promise<string> {
  const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Id': 'default' },
    body: JSON.stringify({ username: 'test-agent', password: 'test-password' }),
  });
  if (!res.ok) return '';
  const data = await res.json();
  return data.accessToken ?? '';
}

/**
 * Clean up E2E test data after a test run.
 * Clears the queue and resets agent state.
 */
export async function cleanupTestData(): Promise<void> {
  await clearQueue();
}

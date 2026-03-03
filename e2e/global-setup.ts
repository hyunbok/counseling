import { chromium, FullConfig } from '@playwright/test';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';
const AGENT_APP_URL = process.env.AGENT_APP_URL ?? 'http://localhost:3100';

async function waitForHealth(url: string, maxRetries = 30, intervalMs = 2000): Promise<void> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const res = await fetch(`${url}/actuator/health`);
      if (res.ok) {
        console.log(`[global-setup] Backend healthy at ${url}`);
        return;
      }
    } catch {
      // not ready yet
    }
    console.log(`[global-setup] Waiting for backend... (${i + 1}/${maxRetries})`);
    await new Promise((r) => setTimeout(r, intervalMs));
  }
  throw new Error(`[global-setup] Backend did not become healthy at ${url}`);
}

async function seedTestAgent(): Promise<void> {
  // Attempt to seed a test agent account via the admin API.
  // If the endpoint returns 409 (already exists) that is acceptable.
  try {
    const res = await fetch(`${BACKEND_URL}/api/agents`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'test-agent',
        password: 'test-password',
        name: '테스트 상담사',
      }),
    });

    if (res.status === 409) {
      console.log('[global-setup] Test agent already exists, skipping seed');
      return;
    }

    if (!res.ok) {
      const body = await res.text();
      console.warn(`[global-setup] Seed agent returned ${res.status}: ${body}`);
    } else {
      console.log('[global-setup] Test agent seeded successfully');
    }
  } catch (err) {
    console.warn('[global-setup] Could not seed test agent (backend may not support this endpoint):', err);
  }
}

async function globalSetup(_config: FullConfig): Promise<void> {
  // 1. Health-check the backend
  await waitForHealth(BACKEND_URL);

  // 2. Seed test data
  await seedTestAgent();

  // 3. Verify agent app is reachable
  const browser = await chromium.launch();
  const page = await browser.newPage();
  try {
    await page.goto(AGENT_APP_URL, { waitUntil: 'domcontentloaded', timeout: 30_000 });
    console.log('[global-setup] Agent app reachable');
  } catch (err) {
    console.warn('[global-setup] Agent app not reachable:', err);
  } finally {
    await browser.close();
  }
}

export default globalSetup;

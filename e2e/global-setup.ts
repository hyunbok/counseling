import { chromium, FullConfig } from '@playwright/test';
import { seedTestAgent, clearQueue } from './fixtures/test-helpers';

const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';
const AGENT_APP_URL = process.env.AGENT_APP_URL ?? 'http://localhost:3100';

async function waitForHealth(url: string, maxRetries = 30, intervalMs = 2000): Promise<void> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const res = await fetch(`${url}/actuator/health`);
      // Accept any response (including 503/DOWN) — the server is running
      if (res.status < 500 || res.status === 503) {
        console.log(`[global-setup] Backend reachable at ${url} (status: ${res.status})`);
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

async function globalSetup(_config: FullConfig): Promise<void> {
  // 1. Health-check the backend
  await waitForHealth(BACKEND_URL);

  // 2. Seed test data
  try {
    await seedTestAgent();
    console.log('[global-setup] Test agent seeded successfully');
  } catch (err) {
    console.warn('[global-setup] Could not seed test agent:', err);
  }

  // 3. Clear queue from previous runs
  try {
    await clearQueue();
    console.log('[global-setup] Queue cleared');
  } catch (err) {
    console.warn('[global-setup] Could not clear queue:', err);
  }

  // 4. Verify agent app is reachable
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

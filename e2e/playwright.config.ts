import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: 'html',
  globalSetup: require.resolve('./global-setup'),
  use: {
    ...devices['Desktop Chrome'],
    trace: 'on-first-retry',
  },
});

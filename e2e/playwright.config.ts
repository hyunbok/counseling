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
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'customer',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: 'http://localhost:3000',
      },
    },
    {
      name: 'agent',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: 'http://localhost:3100',
      },
    },
  ],
});

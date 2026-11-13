import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './tests/e2e', globalSetup: './tests/e2e/environment.js',
  fullyParallel: false, workers: 1, retries: 0, timeout: 30000,
  reporter: [['list'], ['json', { outputFile: 'test-results/results.json' }]],
  use: { browserName: 'chromium', channel: process.env.PW_CHANNEL || undefined,
    headless: true, trace: 'off', video: 'off', screenshot: 'only-on-failure' }
});

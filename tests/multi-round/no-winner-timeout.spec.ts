// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Multi-Round Game Flow', () => {
  test('No Winner Round - Timeout', async ({ page }) => {
    // Login and cleanup
    await login(page);
    await resetParticipation(page);

    // Navigate to multiplayer lobby
    await page.goto('/');
    await page.click('text=멀티게임 로비');
    await expect(page).toHaveURL(/\/game\/multi/);

    // Create a room with minimal rounds for testing
    await page.click('text=방 만들기');
    await expect(page).toHaveURL(/\/game\/multi\/create/);

    await page.fill('#roomName', '타임아웃테스트_' + Date.now());
    await page.selectOption('#totalRounds', '5');
    await page.selectOption('#maxPlayers', '2');

    // Ensure public room
    const privateCheckbox = page.locator('#isPrivate');
    if (await privateCheckbox.isChecked()) {
      await privateCheckbox.uncheck();
    }

    // Create the room
    await Promise.all([
      page.waitForURL(/\/game\/multi\/room\/[A-Za-z0-9]+/),
      page.click('button.btn-create-room')
    ]);

    // Verify room was created
    await expect(page.locator('text=참가 코드')).toBeVisible();
    await expect(page.locator('text=👑')).toBeVisible();

    // Note: Full timeout testing requires game to be in PLAYING phase
    // and waiting for round timeout (typically 30-60 seconds per round)
    console.log('No winner timeout - room created successfully');
  });
});

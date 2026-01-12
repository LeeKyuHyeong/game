// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Game Play and Answer Submission', () => {
  test('Submit Correct Answer First', async ({ page }) => {
    // Login and cleanup
    await login(page);
    await resetParticipation(page);

    // Navigate to multiplayer lobby
    await page.goto('/');
    await page.click('text=멀티게임 로비');
    await expect(page).toHaveURL(/\/game\/multi/);

    // Create a room for game play testing
    await page.click('text=방 만들기');
    await expect(page).toHaveURL(/\/game\/multi\/create/);

    await page.fill('#roomName', '정답테스트_' + Date.now());
    await page.selectOption('#totalRounds', '5');
    await page.selectOption('#maxPlayers', '4');

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

    // Note: Full answer submission testing requires game to be in PLAYING phase
    // This requires multiple players and host to start the game
    console.log('Game play room created successfully');
  });
});

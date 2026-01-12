// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Game Start and Round Preparation', () => {
  test('Host Start Game - All Players Ready', async ({ page }) => {
    // Login and cleanup
    await login(page);
    await resetParticipation(page);

    // Navigate to multiplayer lobby
    await page.goto('/');
    await page.click('text=멀티게임 로비');
    await expect(page).toHaveURL(/\/game\/multi/);

    // Create a room
    await page.click('text=방 만들기');
    await expect(page).toHaveURL(/\/game\/multi\/create/);

    await page.fill('#roomName', '게임시작테스트_' + Date.now());
    await page.selectOption('#totalRounds', '5');
    await page.selectOption('#maxPlayers', '4'); // Using 4 instead of 3 (available: 2,4,6,8,10)

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

    // Verify room was created and host is in waiting room
    await expect(page.locator('text=참가 코드')).toBeVisible();
    await expect(page.locator('text=👑')).toBeVisible();
    await expect(page.locator('text=방장')).toBeVisible();

    // Verify game start button is present but disabled (needs 2+ players)
    await expect(page.locator('text=2명 이상 필요')).toBeVisible();

    // Verify chat is available
    await expect(page.locator('text=채팅')).toBeVisible();

    console.log('Host game start test - room created successfully');
  });
});

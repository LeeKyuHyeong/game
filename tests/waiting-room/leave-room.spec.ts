// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Waiting Room and Pre-Game', () => {
  test('Leave Room', async ({ page }) => {
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

    await page.fill('#roomName', '나가기테스트_' + Date.now());
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

    // Click leave room button
    page.on('dialog', dialog => dialog.accept()); // Accept confirmation dialog
    await page.click('button.btn-leave');

    // Verify redirect to lobby
    await page.waitForURL(/\/game\/multi$/, { timeout: 15000 });

    // Verify we're back in lobby (wait for page load)
    await page.waitForLoadState('networkidle');
    await expect(page.locator('a:has-text("방 만들기")')).toBeVisible({ timeout: 10000 });

    console.log('Leave room - test completed successfully');
  });
});

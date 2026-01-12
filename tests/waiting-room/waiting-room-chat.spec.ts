// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Waiting Room and Pre-Game', () => {
  test('Waiting Room Chat', async ({ page }) => {
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

    await page.fill('#roomName', '채팅테스트_' + Date.now());
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

    // Verify chat section is visible (specifically the chat heading)
    await expect(page.locator('h3:has-text("채팅")')).toBeVisible();

    // Find chat input
    const chatInput = page.locator('input[placeholder*="메시지"]');
    await expect(chatInput).toBeVisible();

    // Send a test message
    const testMessage = '안녕하세요 테스트 메시지입니다';
    await chatInput.fill(testMessage);
    await chatInput.press('Enter');

    // Verify message appears
    await expect(page.locator(`text=${testMessage}`)).toBeVisible();

    console.log('Waiting room chat - test completed successfully');
  });
});

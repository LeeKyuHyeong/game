// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Room Creation and Configuration', () => {
  test('Create Public Room with Default Settings', async ({ page }) => {
    // Login first
    await login(page);

    // Clean up any existing room participation
    await resetParticipation(page);

    // 1. Navigate to home page
    await page.goto('/');

    // 2. Click '멀티게임 로비' button
    await page.click('text=멀티게임 로비');

    // 3. Verify redirect to multiplayer lobby
    await expect(page).toHaveURL(/\/game\/multi/);

    // 4. Click '방 만들기' button
    await page.click('text=방 만들기');

    // 5. Verify redirect to room creation page
    await expect(page).toHaveURL(/\/game\/multi\/create/);

    // 6. Enter room name
    await page.fill('#roomName', '테스트방');

    // 7. Verify default settings (8명, 10 라운드, 전체 랜덤 mode)
    await expect(page.locator('#maxPlayers')).toHaveValue('8');
    await expect(page.locator('#totalRounds')).toHaveValue('10');
    await expect(page.locator('input[value="RANDOM"]')).toBeChecked();

    // 8. Ensure '비공개 방' checkbox is unchecked
    const privateCheckbox = page.locator('#isPrivate');
    if (await privateCheckbox.isChecked()) {
      await privateCheckbox.uncheck();
    }

    // 9. Click '방 만들기' button and wait for navigation
    await Promise.all([
      page.waitForURL(/\/game\/multi\/room\//, { timeout: 30000 }),
      page.click('button.btn-create-room')
    ]);

    // Expected Results verification
    // - Room code is displayed and visible
    await expect(page.locator('text=참가 코드')).toBeVisible();

    // - User appears as host (👑 icon)
    await expect(page.locator('text=👑')).toBeVisible();

    // - User is marked as '방장'
    await expect(page.locator('text=방장')).toBeVisible();
  });
});

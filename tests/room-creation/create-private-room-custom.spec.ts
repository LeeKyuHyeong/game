// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Room Creation and Configuration', () => {
  test('Create Private Room with Custom Settings', async ({ page }) => {
    // 1. Login as a registered user
    await login(page);

    // Clean up any existing room participation
    await resetParticipation(page);

    // 2. Navigate to room creation page
    await page.goto('/game/multi/create');
    await page.waitForLoadState('networkidle');

    // 3. Enter room name
    await page.fill('#roomName', '비공개 테스트');

    // 4. Change max players to 4명
    await page.selectOption('#maxPlayers', '4');

    // 5. Change total rounds to 5 라운드
    await page.selectOption('#totalRounds', '5');

    // 6. Select '장르 고정' radio button
    await page.click('input[type="radio"][value="FIXED_GENRE"]');

    // 7. Wait for genre dropdown to appear
    await page.waitForSelector('#fixedGenreId', { state: 'visible' });

    // 8. Select a genre from dropdown
    const genreOptions = await page.locator('#fixedGenreId option').count();
    if (genreOptions > 1) {
      await page.selectOption('#fixedGenreId', { index: 1 });
    }

    // 9. Check '비공개 방' checkbox
    await page.check('#isPrivate');

    // 10. Click '방 만들기' button and wait for navigation
    await Promise.all([
      page.waitForURL(/\/game\/multi\/room\/[A-Za-z0-9]+/, { timeout: 30000 }),
      page.click('button.btn-create-room')
    ]);

    // Verify room was created with correct settings
    // - Room name is displayed
    await expect(page.locator('text=비공개 테스트')).toBeVisible();

    // - Room code is displayed
    await expect(page.locator('text=참가 코드')).toBeVisible();

    // - User appears as host (👑 icon)
    await expect(page.locator('text=👑')).toBeVisible();

    // - Custom settings are reflected (5 라운드, 4명)
    await expect(page.locator('text=5 라운드')).toBeVisible();
    await expect(page.locator('text=최대 4명')).toBeVisible();

    // - User is marked as host
    await expect(page.locator('text=방장')).toBeVisible();
  });
});

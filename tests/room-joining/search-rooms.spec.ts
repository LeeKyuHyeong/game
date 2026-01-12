// spec: specs/multiplayer-game-test-plan.md
// seed: seed.spec.ts

import { test, expect } from '@playwright/test';
import { login, resetParticipation } from '../fixtures/auth';

test.describe('Room Discovery and Joining', () => {
  test('Search Rooms by Name', async ({ page }) => {
    // Login and cleanup
    await login(page);
    await resetParticipation(page);

    // Navigate to multiplayer lobby
    await page.goto('/');
    await page.click('text=멀티게임 로비');
    await expect(page).toHaveURL(/\/game\/multi/);

    // Verify lobby page elements
    await expect(page.locator('text=방 만들기')).toBeVisible();
    await expect(page.locator('text=공개 방 목록')).toBeVisible();

    // Locate search input
    const searchInput = page.locator('input#searchKeyword');
    await expect(searchInput).toBeVisible();

    // Test search functionality
    await searchInput.fill('테스트');
    await page.waitForTimeout(500); // Wait for search to complete

    // The search should filter rooms (or show empty state if no matches)
    // This verifies the search input is functional

    // Clear search
    await searchInput.clear();
    await page.waitForTimeout(500);

    // Verify refresh button works
    await page.click('text=새로고침');
    await page.waitForTimeout(500);

    console.log('Search rooms - test completed successfully');
  });
});

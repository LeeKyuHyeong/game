import { Page } from '@playwright/test';

// Test credentials
export const TEST_USER = {
  email: 'leekh5@truebon.co.kr',
  password: '1234'
};

// Login helper function
export async function login(page: Page) {
  await page.goto('/auth/login');
  await page.fill('input[name="email"]', TEST_USER.email);
  await page.fill('input[name="password"]', TEST_USER.password);
  await page.click('button[type="submit"]');
  await page.waitForURL('/', { timeout: 10000 });
}

// Navigate to multi lobby after login
export async function goToMultiLobby(page: Page) {
  await login(page);
  await page.goto('/');
  await page.click('text=멀티게임 로비');
  await page.waitForURL(/\/game\/multi/);
}

// Create a room and return the room code
export async function createRoom(page: Page, roomName: string = '테스트방') {
  await goToMultiLobby(page);
  await page.click('text=방 만들기');
  await page.waitForURL(/\/game\/multi\/create/);
  await page.fill('#roomName', roomName);
  await page.click('button.btn-create-room');
  await page.waitForURL(/\/game\/multi\/room\/[A-Za-z0-9]+/, { timeout: 10000 });

  // Extract room code from URL
  const url = page.url();
  const roomCode = url.split('/room/')[1]?.split('?')[0];
  return roomCode;
}

// Reset room participation (clean up from previous tests)
export async function resetParticipation(page: Page) {
  const response = await page.request.post('/game/multi/reset-participation');
  return response.ok();
}

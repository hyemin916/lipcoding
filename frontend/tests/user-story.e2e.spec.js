import { test, expect } from '@playwright/test';

// 사용자 스토리 기반 E2E 시나리오

test.describe('멘토-멘티 매칭 앱 사용자 스토리 E2E', () => {
  test('회원가입, 로그인, 프로필, 멘토목록, 매칭요청, 요청수락/거절, 요청목록', async ({ page }) => {
    // 1. 비로그인 상태에서 / 접근 시 /login으로 리디렉션
    await page.goto('http://localhost:3000/');
    await expect(page).toHaveURL(/\/login$/);

    // 2. 회원가입 페이지 이동
    await page.getByText(/회원가입|Sign up/i).click();
    await expect(page).toHaveURL(/\/signup$/);

    // 3. 회원가입 (멘티)
    await page.locator('#email').fill('mentee@example.com');
    await page.locator('#password').fill('password123');
    await page.locator('#name').fill('멘티유저');
    await page.locator('#role').selectOption('mentee');
    await page.locator('#signup').click();
    // 회원가입 후 /login 리디렉션
    await expect(page).toHaveURL(/\/login$/);

    // 4. 로그인 (멘티)
    await page.locator('#email').fill('mentee@example.com');
    await page.locator('#password').fill('password123');
    await page.locator('#login').click();
    // 로그인 후 /profile 리디렉션
    await expect(page).toHaveURL(/\/profile$/);

    // 5. 프로필 페이지에서 정보 확인 및 수정
    await expect(page.locator('#name')).toBeVisible();
    await expect(page.locator('#bio')).toBeVisible();
    await expect(page.locator('#profile-photo')).toBeVisible();
    await expect(page.locator('#profile')).toBeVisible();
    await expect(page.locator('#save')).toBeVisible();
    await page.locator('#bio').fill('열정적인 멘티입니다!');
    await page.locator('#save').click();

    // 6. 멘토 회원가입 및 로그인 (새 창)
    const mentorPage = await page.context().newPage();
    await mentorPage.goto('http://localhost:3000/signup');
    await mentorPage.locator('#email').fill('mentor@example.com');
    await mentorPage.locator('#password').fill('password123');
    await mentorPage.locator('#name').fill('멘토유저');
    await mentorPage.locator('#role').selectOption('mentor');
    await mentorPage.locator('#signup').click();
    await expect(mentorPage).toHaveURL(/\/login$/);
    await mentorPage.locator('#email').fill('mentor@example.com');
    await mentorPage.locator('#password').fill('password123');
    await mentorPage.locator('#login').click();
    await expect(mentorPage).toHaveURL(/\/profile$/);
    // 멘토 프로필 정보 입력
    await mentorPage.locator('#bio').fill('프론트엔드 멘토입니다!');
    await mentorPage.locator('#skillsets').fill('React,Vue');
    await mentorPage.locator('#save').click();
    // 멘토 프로필 저장 후, 저장 버튼이 다시 활성화될 때까지 대기 (성공 메시지 없는 경우)
    await expect(mentorPage.locator('#save')).toBeEnabled({ timeout: 10000 });

    // 멘토 저장 직후 멘티 탭에서 1초 대기 (데이터 전파 보장)
    await page.waitForTimeout(1000);

    // 7. 멘티가 멘토 목록 조회 및 검색/정렬
    await page.getByText(/Mentors/).click();
    await expect(page).toHaveURL(/\/mentors$/);
    // 멘토 데이터가 비동기로 반영될 때까지 polling
    let mentorFound = false;
    for (let i = 0; i < 40; i++) {
      if (await page.locator('text=Failed to fetch mentors').isVisible()) {
        // 인증 만료 등으로 실패 시, 멘티가 로그인 상태인지 확인
        if (await page.locator('#login').isVisible()) {
          throw new Error('Mentee session expired. Please check login flow.');
        }
        throw new Error('Mentor list API failed');
      }
      if (await page.locator('.mentor').count() > 0) {
        mentorFound = true;
        break;
      }
      await page.waitForTimeout(500);
    }
    expect(mentorFound).toBeTruthy();
    await page.locator('#search').fill('React');
    await page.locator('button', { hasText: 'Search' }).click();
    // 검색 후에도 polling
    mentorFound = false;
    for (let i = 0; i < 20; i++) {
      if (await page.locator('.mentor').count() > 0) {
        mentorFound = true;
        break;
      }
      await page.waitForTimeout(500);
    }
    expect(mentorFound).toBeTruthy();
    await page.locator('#name').check(); // 이름 정렬
    await page.locator('#skill').check(); // 스킬 정렬

    // 8. 멘티가 멘토에게 매칭 요청
    const mentorId = await page.locator('.mentor').first().getAttribute('data-id');
    await page.locator('#message').fill('멘토링 요청합니다!');
    await page.locator('#request').click();
    await expect(page.locator('#request-status')).toBeVisible();

    // 9. 멘토가 요청 수락/거절
    await mentorPage.getByText(/Requests/).click();
    await expect(mentorPage).toHaveURL(/\/requests$/);
    await expect(mentorPage.locator('.request-message')).toBeVisible();
    await mentorPage.locator('#accept').click();
    await expect(mentorPage.locator('.request-message')).toContainText('accepted');

    // 10. 멘티가 요청 상태 확인
    await page.getByText(/Requests/).click();
    await expect(page).toHaveURL(/\/requests$/);
    await expect(page.locator('.request-item')).toContainText('accepted');
  });
});

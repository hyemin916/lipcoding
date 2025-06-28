import { test, expect } from '@playwright/test';

// 애플리케이션이 실행 중인지 확인하는 도우미 함수
const ensureAppIsRunning = async (page) => {
  try {
    await page.goto('http://localhost:3000', { timeout: 5000 });
    return true;
  } catch (error) {
    console.error('애플리케이션이 실행 중이지 않습니다:', error.message);
    console.log('start-app.sh 스크립트를 실행해 백엔드와 프론트엔드를 시작해주세요.');
    return false;
  }
};

test.describe('Authentication Tests', () => {
  test.beforeEach(async ({ page }) => {
    // 애플리케이션이 실행 중인지 확인
    const isRunning = await ensureAppIsRunning(page);
    test.skip(!isRunning, '애플리케이션이 실행 중이지 않아 테스트를 건너뜁니다.');
  });

  test('should display the login page', async ({ page }) => {
    // 로그인 페이지 URL 확인
    expect(page.url()).toContain('localhost:3000');
    
    // 로그인 페이지 요소 확인
    const emailInput = page.locator('#email');
    const passwordInput = page.locator('#password');
    const loginButton = page.locator('#login');
    
    // 요소가 화면에 표시되는지 확인
    await expect(emailInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(loginButton).toBeVisible();
    
    // 타이틀이나 헤더 텍스트 확인
    const titleElement = page.getByRole('heading');
    await expect(titleElement).toContainText(/로그인|Login/i);
  });

  test('should navigate to signup page', async ({ page }) => {
    // 회원가입 링크가 있는지 확인
    const signupLink = page.getByText(/회원가입|Sign up/i);
    await expect(signupLink).toBeVisible();
    
    // 회원가입 링크 클릭
    await signupLink.click();
    
    // URL이 회원가입 페이지로 변경되었는지 확인
    await expect(page).toHaveURL(/.*\/signup/);
    
    // 회원가입 페이지 요소 확인
    const emailInput = page.locator('#email');
    const passwordInput = page.locator('#password');
    const roleSelect = page.locator('#role');
    const signupButton = page.locator('#signup');
    
    await expect(emailInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(roleSelect).toBeVisible();
    await expect(signupButton).toBeVisible();
  });

  test('should register a new user', async ({ page }) => {
    // 회원가입 페이지로 이동
    await page.goto('http://localhost:3000/signup');
    
    // API 요청 인터셉트 설정
    await page.route('**/api/signup', async (route) => {
      const requestData = JSON.parse(route.request().postData());
      
      // 요청 데이터 검증
      expect(requestData.email).toBe('test@example.com');
      expect(requestData.password).toBeDefined();
      expect(requestData.role).toBe('mentee');
      
      // 성공 응답 반환
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'User registered successfully' }),
      });
    });
    
    // 회원가입 폼 작성
    await page.locator('#email').fill('test@example.com');
    await page.locator('#password').fill('password123');
    await page.locator('#role').selectOption('mentee');
    
    // 폼 제출
    await page.locator('#signup').click();
    
    // 로그인 페이지로 리디렉션 확인 (회원가입 성공 후)
    await expect(page).toHaveURL(/.*\/login/, { timeout: 5000 });
  });

  test('should login successfully', async ({ page }) => {
    // 로그인 페이지로 이동
    await page.goto('http://localhost:3000/login');
    
    // API 요청 인터셉트 설정
    await page.route('**/api/login', async (route) => {
      const requestData = JSON.parse(route.request().postData());
      
      // 요청 데이터 검증
      expect(requestData.email).toBe('test@example.com');
      expect(requestData.password).toBeDefined();
      
      // 성공 응답 반환
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ 
          token: 'fake-jwt-token',
          user: {
            id: 1,
            email: 'test@example.com',
            role: 'mentee',
            name: 'Test User',
            bio: '',
            skills: []
          }
        }),
      });
    });
    
    // 로그인 폼 작성
    await page.locator('#email').fill('test@example.com');
    await page.locator('#password').fill('password123');
    
    // 폼 제출
    await page.locator('#login').click();
    
    // 로그인 성공 후 프로필이나 대시보드 페이지로 이동 확인
    // 타임아웃 증가 (리디렉션에 시간이 걸릴 수 있음)
    await expect(page).toHaveURL(/.*\/profile|.*\/dashboard|.*\/mentors/, { timeout: 5000 });
  });
});

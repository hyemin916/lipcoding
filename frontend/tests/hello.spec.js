import { test, expect } from '@playwright/test';

// 가장 기본적인 테스트 - 실제 브라우저 없이도 실행 가능
test('simple test', async () => {
  // 간단한 검증만 수행
  expect(1 + 1).toBe(2);
  expect('Hello').toContain('Hell');
  expect(true).toBeTruthy();
  
  console.log('테스트가 성공적으로 실행되었습니다!');
});

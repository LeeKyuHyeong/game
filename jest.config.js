module.exports = {
  testEnvironment: 'jsdom',
  roots: ['<rootDir>/src/test/javascript'],
  testMatch: ['**/*.test.js'],
  moduleFileExtensions: ['js'],
  verbose: true,
  testTimeout: 10000,
  // Fake timers for testing async/timing scenarios
  fakeTimers: {
    enableGlobally: false
  }
};

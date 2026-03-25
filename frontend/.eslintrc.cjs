/* eslint-env node */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
  ],
  plugins: ['vue'],
  parserOptions: {
    ecmaVersion: 2022,
    sourceType: 'module',
  },
  rules: {
    // ── Vue 관련 ──────────────────────────────────────────────────────────────
    'vue/multi-word-component-names': 'off',   // App.vue 등 단어 컴포넌트 허용
    'vue/no-unused-vars': 'error',
    'vue/html-self-closing': ['warn', {
      html: { void: 'always', normal: 'never', component: 'always' },
    }],
    'vue/no-v-html': 'warn',                   // XSS 주의
    'vue/require-default-prop': 'warn',

    // ── JS 품질 ───────────────────────────────────────────────────────────────
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    'no-var': 'error',
    'prefer-const': 'error',
    'eqeqeq': ['error', 'always'],
    'no-eval': 'error',
  },
  ignorePatterns: ['dist/', 'node_modules/', '*.config.js', '*.config.cjs'],
}

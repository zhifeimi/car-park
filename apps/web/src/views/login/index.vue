<template>
  <div class="login">
    <div class="login-lang-switch">
      <el-dropdown @command="handleLocaleChange" trigger="click">
        <span class="lang-btn">
          <el-icon><Operation /></el-icon>
          <span>{{ currentLocaleLabel }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="option in localeOptions" :key="option.code" :command="option.code"
              :disabled="locale === option.code" :lang="option.code">{{ option.label }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="login-brand">
      <span class="brand-icon">P</span>
      <h1 class="title">{{ siteName || t('login.brandTitle') }}</h1>
      <p v-if="siteName" class="subtitle">{{ t('login.brandTitle') }}</p>
    </div>
    <div class="login-card">
      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" size="large" auto-complete="off" :placeholder="t('login.username')">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" size="large" type="password" auto-complete="off"
            :placeholder="t('login.password')" show-password @keyup.enter="handleLogin">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button size="large" type="primary" style="width: 100%; border-radius: 8px; height: 44px; font-size: 15px" :loading="loading"
            @click="handleLogin">
            <span v-if="!loading">{{ t('login.loginBtn') }}</span>
            <span v-else>{{ t('login.loggingIn') }}</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="login-footer">
      <span>{{ t('login.footer') }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, Operation, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useI18n } from '@/locales'

const router = useRouter()
const userStore = useUserStore()
const { locale, setLocale, t, siteName, localeOptions } = useI18n()

const currentLocaleLabel = computed(() => localeOptions.find((o) => o.code === locale.value)?.label)

function handleLocaleChange(lang) {
  setLocale(lang)
}

const loginRef = ref(null)
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })

const loginRules = computed(() => ({
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
}))

async function handleLogin() {
  const valid = await loginRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* The login screen is the park's entrance sign: flat signboard ground,
   cream lettering, one plain form card. */
.login {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: var(--np-space-5);
  min-height: 100vh;
  padding: var(--np-space-6) var(--np-space-4);
  background: var(--np-sign);
  position: relative;
}

.login-lang-switch {
  position: absolute;
  top: 24px;
  right: 24px;
}

.lang-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--np-on-sign);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  border: 1px solid var(--np-on-sign-line);
  transition: background 0.15s;
}

.lang-btn:hover {
  background: var(--np-on-sign-wash);
}

.login-brand {
  text-align: center;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: var(--np-radius-md);
  background: var(--np-on-sign);
  color: var(--np-sign);
  font-size: 30px;
  font-weight: 800;
  margin-bottom: var(--np-space-4);
}

.title {
  margin: 0;
  font-size: var(--np-text-xl);
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--np-on-sign);
  text-wrap: balance;
}

.subtitle {
  margin: var(--np-space-2) 0 0;
  font-size: var(--np-text-sm);
  letter-spacing: 0.08em;
  color: var(--np-on-sign-muted);
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: var(--np-surface);
  border-radius: var(--np-radius-lg);
}

.login-form {
  padding: 28px 28px 8px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--np-radius-md);
}

.login-footer {
  position: fixed;
  bottom: 20px;
  width: 100%;
  text-align: center;
  color: var(--np-on-sign-muted);
  opacity: 0.7;
  font-size: 12px;
}
</style>

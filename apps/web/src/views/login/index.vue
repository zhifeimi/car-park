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
            <el-dropdown-item command="en" :disabled="locale === 'en'">English</el-dropdown-item>
            <el-dropdown-item command="zh-CN" :disabled="locale === 'zh-CN'">简体中文</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="login-card">
      <div class="login-brand">
        <span class="brand-icon">P</span>
        <h3 class="title">{{ t('login.brandTitle') }}</h3>
        <p class="subtitle">{{ t('login.brandSubtitle') }}</p>
      </div>
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
const { locale, setLocale, t } = useI18n()

const currentLocaleLabel = computed(() => (locale.value === 'zh-CN' ? '简体中文' : 'English'))

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
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 40%, #334155 100%);
  position: relative;
  overflow: hidden;
}

.login-lang-switch {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 10;
}

.lang-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.2s;
}

.lang-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.login::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(67, 97, 238, 0.15) 0%, transparent 70%);
  top: -200px;
  right: -100px;
}

.login::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(46, 196, 182, 0.1) 0%, transparent 70%);
  bottom: -100px;
  left: -50px;
}

.login-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.login-brand {
  text-align: center;
  padding: 36px 28px 20px;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--cp-primary), var(--cp-primary-light));
  color: #fff;
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 16px;
  box-shadow: 0 4px 14px rgba(67, 97, 238, 0.3);
}

.title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: var(--cp-text-primary);
}

.subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--cp-text-muted);
}

.login-form {
  padding: 8px 32px 32px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.login-footer {
  position: fixed;
  bottom: 20px;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  z-index: 1;
}
</style>

<template>
  <el-container class="layout-container">
    <el-aside :width="sidebarWidth" class="layout-aside">
      <div class="logo">
        <span class="logo-icon">P</span>
        <span class="logo-text">{{ t('nav.brandTitle') }}</span>
      </div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>{{ t('nav.dashboard') }}</span>
        </el-menu-item>
        <el-menu-item index="/park">
          <el-icon><OfficeBuilding /></el-icon>
          <span>{{ t('nav.park') }}</span>
        </el-menu-item>
        <el-menu-item index="/booking">
          <el-icon><Tickets /></el-icon>
          <span>{{ t('nav.booking') }}</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>{{ t('nav.user') }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleLocaleChange" trigger="click">
            <span class="header-action-btn">
              <el-icon><Operation /></el-icon>
              <span>{{ currentLocaleLabel }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="en" :disabled="locale === 'en'">English</el-dropdown-item>
                <el-dropdown-item command="zh-CN" :disabled="locale === 'zh-CN'">简体中文</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleCommand">
            <span class="header-user">
              <span class="avatar">{{ avatarLetter }}</span>
              {{ userStore.nickname || userStore.username || t('nav.admin') }}
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">{{ t('nav.modifyPassword') }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>{{ t('nav.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="passwordVisible" :title="t('nav.modifyPassword')" width="420px" append-to-body>
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="120px">
      <el-form-item :label="t('layout.oldPassword')" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="t('layout.newPassword')" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="t('layout.confirmPassword')" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="passwordLoading" @click="submitPassword">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, HomeFilled, OfficeBuilding, Operation, Tickets, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { changePassword, getMe } from '@/api/auth'
import { useI18n } from '@/locales'

const sidebarWidth = '240px'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { locale, setLocale, t } = useI18n()

const pageTitles = computed(() => ({
  '/dashboard': t('nav.dashboard'),
  '/park': t('nav.park'),
  '/booking': t('nav.booking'),
  '/user': t('nav.user'),
}))
const pageTitle = computed(() => pageTitles.value[route.path] || t('nav.systemTitle'))

const currentLocaleLabel = computed(() => (locale.value === 'zh-CN' ? '简体中文' : 'English'))

function handleLocaleChange(lang) {
  setLocale(lang)
}

const avatarLetter = computed(() => {
  const name = userStore.nickname || userStore.username || 'A'
  return name.charAt(0).toUpperCase()
})

onMounted(async () => {
  try {
    const me = await getMe()
    userStore.setProfile(me)
  } catch {
    // token invalid — the request interceptor already handles the re-login dialog
  }
})

const passwordVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const passwordRules = computed(() => ({
  oldPassword: [{ required: true, message: t('layout.oldPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('layout.newPasswordRequired'), trigger: 'blur' },
    { min: 8, max: 64, message: t('layout.newPasswordLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('layout.confirmPasswordRequired'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error(t('layout.passwordMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}))

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm(t('nav.logoutConfirm'), t('common.systemTip'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (command === 'password') {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordVisible.value = true
  }
}

async function submitPassword() {
  await passwordFormRef.value.validate()
  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordVisible.value = false
    ElMessage.success(t('layout.passwordChangedSuccess'))
    userStore.logout()
    router.push('/login')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background: var(--cp-sidebar-bg);
  overflow-x: hidden;
  overflow-y: auto;
  transition: width 0.3s;
  border-right: none;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: var(--cp-sidebar-logo-bg);
  padding: 0 20px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: var(--cp-primary);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
  color: #fff;
  flex-shrink: 0;
  letter-spacing: -0.5px;
}

.logo-text {
  color: #f1f5f9;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.layout-aside :deep(.el-menu) {
  border-right: none;
  background: transparent;
  padding: 8px;
}

.layout-aside :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 4px;
  border-radius: var(--cp-radius-md);
  color: var(--cp-sidebar-text);
  transition: all 0.2s;
  font-size: 14px;
}

.layout-aside :deep(.el-menu-item:hover) {
  background: var(--cp-sidebar-hover);
  color: #e2e8f0;
}

.layout-aside :deep(.el-menu-item.is-active) {
  background: var(--cp-sidebar-active);
  color: #fff;
}

.layout-aside :deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 8px;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--cp-header-bg);
  box-shadow: var(--cp-header-shadow);
  z-index: 1;
  height: 64px;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--cp-text-primary);
}

.header-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--cp-text-secondary);
  font-size: 13px;
  padding: 6px 10px;
  border-radius: var(--cp-radius-md);
  transition: all 0.2s;
  border: 1px solid var(--cp-border);
}

.header-action-btn:hover {
  background: var(--cp-body-bg);
  color: var(--cp-primary);
  border-color: var(--cp-primary-light);
}

.header-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--cp-text-secondary);
  font-size: 14px;
  padding: 6px 12px;
  border-radius: var(--cp-radius-md);
  transition: all 0.2s;
}

.header-user:hover {
  background: var(--cp-body-bg);
  color: var(--cp-text-primary);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--cp-primary), var(--cp-primary-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.arrow {
  font-size: 12px;
  transition: transform 0.2s;
}

.layout-main {
  background-color: var(--cp-body-bg);
  overflow-y: auto;
  padding: 24px;
}
</style>

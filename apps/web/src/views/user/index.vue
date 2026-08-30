<template>
  <div class="page-container">
    <div class="page-card">
      <el-alert type="info" :closable="false" class="tip">
        {{ t('user.tip') }}
      </el-alert>

      <div class="toolbar">
        <el-button type="primary" plain @click="openForm">{{ t('user.addAdmin') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="userList">
        <el-table-column :label="t('user.username')" prop="username" width="160" />
        <el-table-column :label="t('user.nickname')" prop="nickname" width="160" />
        <el-table-column :label="t('common.remark')" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('user.createTime')" prop="createTime" width="180" />
        <el-table-column :label="t('common.operation')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="t('user.addAdmin')" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('user.username')" prop="username">
          <el-input v-model="form.username" :placeholder="t('user.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('user.password')" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="t('user.passwordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('user.nickname')" prop="nickname">
          <el-input v-model="form.nickname" :placeholder="t('user.nicknamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :placeholder="t('user.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addUser, deleteUser, listUsers } from '@/api/auth'
import { useI18n } from '@/locales'

const { t } = useI18n()

const loading = ref(false)
const userList = ref([])

async function getList() {
  loading.value = true
  try {
    const users = await listUsers()
    userList.value = Array.isArray(users) ? users : users?.rows ?? []
  } finally {
    loading.value = false
  }
}

const formVisible = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '', nickname: '', remark: '' })

const rules = computed(() => ({
  username: [
    { required: true, message: t('user.usernameRequired'), trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,30}$/, message: t('user.usernamePattern'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('user.passwordRequired'), trigger: 'blur' },
    { min: 8, max: 64, message: t('user.passwordLength'), trigger: 'blur' },
  ],
}))

function openForm() {
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.remark = ''
  formVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  await addUser({ ...form })
  ElMessage.success(t('common.addSuccess'))
  formVisible.value = false
  getList()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    t('user.deleteAdminConfirm', { username: row.username }),
    t('common.systemTip'),
    { type: 'warning', confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel') },
  )
  await deleteUser(row.id)
  ElMessage.success(t('common.deleteSuccess'))
  getList()
}

onMounted(getList)
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-card {
  background: var(--cp-card-bg);
  border-radius: var(--cp-card-radius);
  box-shadow: var(--cp-card-shadow);
  padding: 20px;
}

.tip {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
</style>

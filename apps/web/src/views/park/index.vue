<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-section">
        <el-form :model="queryParams" inline>
          <el-form-item :label="t('park.parkName')">
            <el-input v-model="queryParams.name" :placeholder="t('park.searchParkPlaceholder')" clearable
              @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="t('park.availableDates')">
            <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
              range-separator="—" :start-placeholder="t('park.startDate')" :end-placeholder="t('park.endDate')" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">{{ t('common.search') }}</el-button>
            <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="toolbar">
        <el-button type="primary" plain @click="openForm()">{{ t('common.add') }}</el-button>
        <el-button type="warning" plain @click="handleExport">{{ t('common.export') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="parkList">
        <el-table-column :label="t('park.parkName')" prop="name" min-width="140" />
        <el-table-column :label="t('park.totalLots')" prop="lotNum" width="120" />
        <el-table-column :label="t('park.todayRemaining')" width="140">
          <template #default="{ row }">
            <el-tag :type="row.leftLotNum > 0 ? 'success' : 'danger'" round>{{ row.leftLotNum ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('park.openDateBegin')" prop="openDateBegin" width="140" />
        <el-table-column :label="t('park.openDateEnd')" prop="openDateEnd" width="140" />
        <el-table-column :label="t('park.openTime')" prop="openTime" width="110" />
        <el-table-column :label="t('park.closeTime')" prop="closeTime" width="110" />
        <el-table-column :label="t('common.remark')" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column :label="t('common.operation')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? t('park.editPark') : t('park.addPark')" width="540px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-form-item :label="t('park.parkName')" prop="name">
          <el-input v-model="form.name" :placeholder="t('park.searchParkPlaceholder')" />
          <div class="form-tip">{{ t('park.nameTip') }}</div>
        </el-form-item>
        <el-form-item :label="t('park.totalLots')" prop="lotNum">
          <el-input-number v-model="form.lotNum" :min="0" :step="1" />
        </el-form-item>
        <el-form-item :label="t('park.dateSpan')" prop="dateSpan">
          <el-date-picker v-model="form.dateSpan" type="daterange" value-format="YYYY-MM-DD"
            range-separator="-" :start-placeholder="t('park.startDate')" :end-placeholder="t('park.endDate')" />
        </el-form-item>
        <el-form-item :label="t('park.timeSpan')" prop="timeSpan">
          <el-time-picker v-model="form.timeSpan" is-range value-format="HH:mm:ss"
            range-separator="-" :start-placeholder="t('park.openTime')" :end-placeholder="t('park.closeTime')" />
        </el-form-item>
        <el-form-item :label="t('common.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :placeholder="t('park.remarkPlaceholder')" />
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
import Pagination from '@/components/Pagination.vue'
import { addPark, deletePark, exportParks, listPark, updatePark } from '@/api/parking'
import { useI18n } from '@/locales'

const { t } = useI18n()

const loading = ref(false)
const parkList = ref([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, name: undefined })
const dateRange = ref([])

async function getList() {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (dateRange.value?.length === 2) {
      params.openDateBegin = dateRange.value[0]
      params.openDateEnd = dateRange.value[1]
    }
    const { rows, total: count } = await listPark(params)
    parkList.value = rows
    total.value = count
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.name = undefined
  dateRange.value = []
  handleQuery()
}

const formVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: undefined,
  name: '',
  lotNum: 0,
  dateSpan: [],
  timeSpan: [],
  remark: '',
})

const rules = computed(() => ({
  name: [{ required: true, message: t('park.nameRequired'), trigger: 'blur' }],
  lotNum: [{ required: true, message: t('park.lotsRequired'), trigger: 'change' }],
}))

function openForm(row) {
  if (row) {
    form.id = row.id
    form.name = row.name
    form.lotNum = row.lotNum ?? 0
    form.dateSpan = row.openDateBegin && row.openDateEnd ? [row.openDateBegin, row.openDateEnd] : []
    form.timeSpan = row.openTime && row.closeTime ? [row.openTime, row.closeTime] : []
    form.remark = row.remark || ''
  } else {
    form.id = undefined
    form.name = ''
    form.lotNum = 0
    form.dateSpan = []
    form.timeSpan = []
    form.remark = ''
  }
  formVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  const payload = {
    name: form.name,
    lotNum: form.lotNum,
    openDateBegin: form.dateSpan?.[0] ?? null,
    openDateEnd: form.dateSpan?.[1] ?? null,
    openTime: form.timeSpan?.[0] ?? null,
    closeTime: form.timeSpan?.[1] ?? null,
    remark: form.remark,
  }
  if (form.id) {
    await updatePark({ id: form.id, ...payload })
    ElMessage.success(t('common.editSuccess'))
  } else {
    await addPark(payload)
    ElMessage.success(t('common.addSuccess'))
  }
  formVisible.value = false
  getList()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    t('park.deleteParkConfirm', { name: row.name }),
    t('common.systemTip'),
    { type: 'warning', confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel') },
  )
  await deletePark(row.id)
  ElMessage.success(t('common.deleteSuccess'))
  getList()
}

async function handleExport() {
  const params = { name: queryParams.name }
  if (dateRange.value?.length === 2) {
    params.openDateBegin = dateRange.value[0]
    params.openDateEnd = dateRange.value[1]
  }
  const blob = await exportParks(params)
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `park_${Date.now()}.xlsx`
  link.click()
  URL.revokeObjectURL(link.href)
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

.filter-section {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--cp-border);
  margin-bottom: 16px;
}

.filter-section :deep(.el-form-item) {
  margin-bottom: 0;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.form-tip {
  font-size: 12px;
  color: var(--cp-warning);
  line-height: 1.5;
  margin-top: 4px;
}
</style>

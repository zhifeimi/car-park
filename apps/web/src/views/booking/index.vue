<template>
  <div class="page-container">
    <div class="page-card">
      <div class="filter-section">
        <el-form :model="queryParams" inline>
          <el-form-item :label="t('booking.carPlate')">
            <el-input v-model="queryParams.carPlate" :placeholder="t('booking.searchPlatePlaceholder')" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="t('booking.park')">
            <el-input v-model="queryParams.park" :placeholder="t('booking.searchParkPlaceholder')" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="t('booking.bookingDate')">
            <el-date-picker v-model="queryParams.bookingDate" type="date" value-format="YYYY-MM-DD"
              :placeholder="t('booking.selectDate')" clearable />
          </el-form-item>
          <el-form-item :label="t('booking.phone')">
            <el-input v-model="queryParams.cellPhone" :placeholder="t('booking.searchPhonePlaceholder')" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="t('common.status')">
            <el-select v-model="queryParams.status" :placeholder="t('booking.statusAll')" clearable style="width: 140px">
              <el-option :label="displayFor(0).label" :value="0" />
              <el-option :label="displayFor(-1).label" :value="-1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">{{ t('common.search') }}</el-button>
            <el-button @click="resetQuery">{{ t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="toolbar">
        <el-button type="warning" plain @click="handleExport">{{ t('common.export') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="bookingList">
        <el-table-column :label="t('booking.carPlate')" prop="carPlate" width="130" />
        <el-table-column :label="t('booking.park')" prop="park" min-width="140" />
        <el-table-column :label="t('booking.bookingDate')" prop="bookingDate" width="130" />
        <el-table-column :label="t('booking.phone')" prop="cellPhone" width="140" />
        <el-table-column :label="t('common.status')" width="130">
          <template #default="{ row }">
            <el-tag :type="tagTypeFor(row.status)" round>
              {{ displayFor(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('booking.bookingTime')" prop="createTime" width="180" />
        <el-table-column :label="t('booking.createdBy')" prop="createBy" width="110" />
        <el-table-column :label="t('common.remark')" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column :label="t('common.operation')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination.vue'
import { deleteBooking, exportBookings, listBooking } from '@/api/parking'
import { bookingLifecycleDisplay } from '@car-park/utils'
import { useI18n } from '@/locales'

const { locale, t } = useI18n()

function displayFor(status) {
  return bookingLifecycleDisplay(status, locale.value)
}

function tagTypeFor(status) {
  return displayFor(status).treatment === 'positive' ? 'success' : 'info'
}

const loading = ref(false)
const bookingList = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  carPlate: undefined,
  park: undefined,
  bookingDate: undefined,
  cellPhone: undefined,
  status: undefined,
})

async function getList() {
  loading.value = true
  try {
    const { rows, total: count } = await listBooking({ ...queryParams })
    bookingList.value = rows
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
  queryParams.carPlate = undefined
  queryParams.park = undefined
  queryParams.bookingDate = undefined
  queryParams.cellPhone = undefined
  queryParams.status = undefined
  handleQuery()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    t('booking.deleteBookingConfirm', { plate: row.carPlate }),
    t('common.systemTip'),
    { type: 'warning', confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel') },
  )
  await deleteBooking(row.id)
  ElMessage.success(t('common.deleteSuccess'))
  getList()
}

async function handleExport() {
  const blob = await exportBookings({
    carPlate: queryParams.carPlate,
    park: queryParams.park,
    bookingDate: queryParams.bookingDate,
    cellPhone: queryParams.cellPhone,
    status: queryParams.status,
  })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `booking_${Date.now()}.xlsx`
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
</style>

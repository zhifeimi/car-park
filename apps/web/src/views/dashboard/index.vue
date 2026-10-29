<template>
  <div class="dashboard">
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ sum(statistic.todayBooking) }}</span>
          <span class="stat-label">{{ t('dashboard.todayBooking') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ sum(statistic.weekBooking) }}</span>
          <span class="stat-label">{{ t('dashboard.weekBooking') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ sum(statistic.monthBooking) }}</span>
          <span class="stat-label">{{ t('dashboard.monthBooking') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ sum(statistic.yearBooking) }}</span>
          <span class="stat-label">{{ t('dashboard.yearBooking') }}</span>
        </div>
      </div>
    </div>

    <div class="content-row">
      <div class="content-card today-card">
        <div class="card-head">
          <span class="card-title">{{ t('dashboard.todayParkBookings') }}</span>
          <el-date-picker v-model="date" type="datetime" :placeholder="t('dashboard.selectDatePlaceholder')"
            size="small" @change="loadStatistic" />
        </div>
        <el-table :data="statistic.todayBooking" size="small" :empty-text="t('dashboard.noBookingsToday')" class="inner-table">
          <el-table-column prop="park" :label="t('dashboard.park')" />
          <el-table-column prop="booking" :label="t('dashboard.bookingCount')" width="120">
            <template #default="{ row }">
              <span class="booking-count">{{ row.booking }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="content-card park-card">
        <div class="card-head">
          <span class="card-title">{{ t('dashboard.parkOverview') }}</span>
        </div>
        <div class="park-list">
          <div v-for="p in parks" :key="p.id" class="park-item">
            <div class="park-name">{{ p.name }}</div>
            <div class="park-detail">
              <div class="park-slots">
                <span class="slot-available" :class="{ full: (p.leftLotNum ?? 0) === 0 }">
                  {{ p.leftLotNum ?? 0 }}
                </span>
                <span class="slot-sep">/</span>
                <span class="slot-total">{{ p.lotNum }}</span>
                <span class="slot-label">{{ t('dashboard.available') }}</span>
              </div>
              <div class="park-time">{{ p.openTime ?? '—' }} ~ {{ p.closeTime ?? '—' }}</div>
            </div>
          </div>
          <el-empty v-if="!parks.length" :description="t('dashboard.noParks')" :image-size="48" />
        </div>
      </div>
    </div>

    <div class="content-card history-card">
      <div class="card-head">
        <span class="card-title">{{ t('dashboard.recentBookings') }}</span>
        <span class="card-sub">{{ t('dashboard.recent10Bookings') }}</span>
      </div>
      <div v-if="bookings.length" class="booking-list">
        <div v-for="item in bookings" :key="item.id" class="booking-row">
          <div class="booking-plate">{{ item.carPlate }}</div>
          <div class="booking-info">
            <span>{{ item.park }}</span>
            <span class="dot">&middot;</span>
            <span>{{ item.bookingDate }}</span>
            <span class="dot">&middot;</span>
            <span class="booking-phone">{{ item.cellPhone }}</span>
          </div>
          <el-tag size="small" :type="tagTypeFor(item.status)" round>
            {{ displayFor(item.status).label }}
          </el-tag>
          <div class="booking-time">{{ item.createTime }}</div>
        </div>
      </div>
      <el-empty v-else :description="t('dashboard.noBookings')" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getStatisticInfo, listBooking, listPark } from '@/api/parking'
import { bookingLifecycleDisplay } from '@car-park/utils'
import { useI18n } from '@/locales'

const { locale, t } = useI18n()

function displayFor(status) {
  return bookingLifecycleDisplay(status, locale.value)
}

function tagTypeFor(status) {
  return displayFor(status).treatment === 'positive' ? 'success' : 'info'
}

const date = ref(new Date())
const statistic = reactive({ todayBooking: [], weekBooking: [], monthBooking: [], yearBooking: [] })
const parks = ref([])
const bookings = ref([])

function sum(rows) {
  return (rows || []).reduce((acc, row) => acc + Number(row.booking || 0), 0)
}

async function loadStatistic() {
  const info = await getStatisticInfo(date.value)
  statistic.todayBooking = info?.todayBooking ?? []
  statistic.weekBooking = info?.weekBooking ?? []
  statistic.monthBooking = info?.monthBooking ?? []
  statistic.yearBooking = info?.yearBooking ?? []
}

async function loadParks() {
  const { rows } = await listPark({})
  parks.value = rows
}

async function loadBookings() {
  const { rows } = await listBooking({ pageNum: 1, pageSize: 10 })
  bookings.value = rows
}

onMounted(() => {
  loadStatistic()
  loadParks()
  loadBookings()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--cp-card-bg);
  border-radius: var(--cp-card-radius);
  box-shadow: var(--cp-card-shadow);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--np-shadow-raised);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--np-brand-soft);
  color: var(--np-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-variant-numeric: tabular-nums;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--cp-text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--cp-text-muted);
  margin-top: 2px;
}

.content-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.content-card {
  background: var(--cp-card-bg);
  border-radius: var(--cp-card-radius);
  box-shadow: var(--cp-card-shadow);
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--cp-border);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cp-text-primary);
}

.card-sub {
  font-size: 12px;
  color: var(--cp-text-muted);
}

.inner-table {
  border-radius: 0;
}

.booking-count {
  font-weight: 700;
  color: var(--cp-primary);
}

.park-list {
  padding: 8px 12px;
}

.park-item {
  padding: 12px 8px;
  border-bottom: 1px solid var(--cp-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.park-item:last-child {
  border-bottom: none;
}

.park-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--cp-text-primary);
}

.park-detail {
  display: flex;
  align-items: center;
  gap: 20px;
}

.park-slots {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.slot-available {
  font-variant-numeric: tabular-nums;
  font-size: 22px;
  font-weight: 800;
  color: var(--cp-success);
}

.slot-available.full {
  color: var(--cp-danger);
}

.slot-sep {
  color: var(--cp-text-muted);
  font-size: 14px;
  margin: 0 2px;
}

.slot-total {
  font-size: 14px;
  color: var(--cp-text-muted);
}

.slot-label {
  font-size: 11px;
  color: var(--cp-text-muted);
  margin-left: 4px;
}

.park-time {
  font-size: 13px;
  color: var(--cp-text-secondary);
}

.booking-list {
  padding: 4px 0;
}

.booking-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--cp-border);
  transition: background 0.15s;
}

.booking-row:hover {
  background: var(--np-paper);
}

.booking-row:last-child {
  border-bottom: none;
}

.booking-plate {
  font-weight: 700;
  font-size: 14px;
  color: var(--cp-text-primary);
  min-width: 90px;
}

.booking-info {
  flex: 1;
  font-size: 13px;
  color: var(--cp-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  color: var(--cp-text-muted);
}

.booking-phone {
  font-variant-numeric: tabular-nums;
}

.booking-time {
  font-size: 12px;
  color: var(--cp-text-muted);
  min-width: 140px;
  text-align: right;
}
</style>

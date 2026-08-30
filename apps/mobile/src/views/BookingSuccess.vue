<template>
  <div class="page">
    <div class="header-bar">
      <div class="header-placeholder" />
      <div class="lang-switch">
        <button
          :class="['lang-btn', { active: locale === 'en' }]"
          @click="setLocale('en')"
        >
          EN
        </button>
        <span class="lang-sep">/</span>
        <button
          :class="['lang-btn', { active: locale === 'zh-CN' }]"
          @click="setLocale('zh-CN')"
        >
          中文
        </button>
      </div>
    </div>

    <div class="form">
      <div class="qr-card">
        <template v-if="booking.id">
          <div class="mark">{{ display.label }}</div>
          <template v-if="display.canShowQr">
            <div class="notice">{{ t('success.notice1') }}</div>
            <div class="notice">{{ t('success.notice2') }}</div>
            <img v-if="qrDataUrl" :src="qrDataUrl" class="qr-image" alt="QR Code" />
          </template>
          <van-button
            v-if="display.canShowCancellationControl"
            type="primary"
            :disabled="submitting"
            class="cancel-button"
            round
            @click="cancelBooking"
          >
            {{ t('success.cancelBooking') }}
          </van-button>
        </template>
        <van-empty v-else :description="t('success.noRecord')" />
      </div>
    </div>

    <div class="form">
      <div v-if="booking.id" class="detail-card">
        <van-cell :title="t('success.bookingDate')" :value="booking.bookingDate" />
        <van-cell :title="t('success.carPlate')" :value="booking.carPlate" />
        <van-cell :title="t('success.phone')" :value="booking.cellPhone" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import QRCode from 'qrcode'
import { showFailToast, showSuccessToast } from 'vant'
import { getLastBooking, updateBooking } from '@/api/parking'
import { bookingLifecycleDisplay, generateQrPayload, PHONE_STORAGE_KEY } from '@car-park/utils'
import { useI18n } from '@/locales'

const { locale, setLocale, t } = useI18n()

const booking = ref({})
const submitting = ref(false)
const qrDataUrl = ref('')

const display = computed(() =>
  bookingLifecycleDisplay(booking.value.status, locale.value),
)

async function refreshQr() {
  if (!booking.value.id || !display.value.canShowQr) {
    qrDataUrl.value = ''
    return
  }
  try {
    const payload = generateQrPayload({
      carPlate: booking.value.carPlate,
      cellPhone: booking.value.cellPhone,
      park: booking.value.park,
      bookingDate: booking.value.bookingDate,
    })
    qrDataUrl.value = await QRCode.toDataURL(payload, {
      width: 200,
      margin: 1,
    })
  } catch {
    qrDataUrl.value = ''
  }
}

/**
 * The booking is always loaded by phone via /parking/booking/last — the anonymous
 * surface has no per-id read (GET /parking/booking/{id} is gone; see issue 02). The
 * phone is stored by BookingAdd before navigating, so submit-redirects and reloads
 * take this same single path.
 */
async function loadBooking() {
  const cellPhone = localStorage.getItem(PHONE_STORAGE_KEY)
  if (!cellPhone) return
  try {
    const last = await getLastBooking(cellPhone)
    booking.value = last || {}
    await refreshQr()
  } catch {
    showFailToast(t('success.fetchFailed'))
  }
}

async function cancelBooking() {
  const previousStatus = booking.value.status
  booking.value.status = -1 // optimistic, matches the legacy UI behavior
  submitting.value = true
  try {
    await updateBooking({ id: booking.value.id, status: -1 })
    showSuccessToast(t('success.cancelSuccess'))
  } catch (err) {
    // recovery: restore UI state and refetch the truth from the server
    booking.value.status = previousStatus
    showFailToast(err?.message || t('success.cancelFailed'))
    await loadBooking()
  } finally {
    submitting.value = false
  }
}

onMounted(loadBooking)
</script>

<style scoped>
.page {
  min-height: 100vh;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0;
}

.lang-switch {
  display: inline-flex;
  align-items: center;
  background: #ebedf0;
  padding: 2px 6px;
  border-radius: 14px;
}

.lang-btn {
  background: transparent;
  border: none;
  font-size: 12px;
  font-weight: 500;
  color: #646566;
  padding: 2px 6px;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.2s;
}

.lang-btn.active {
  background: #fff;
  color: #1989fa;
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.lang-sep {
  color: #c8c9cc;
  font-size: 11px;
  margin: 0 1px;
}

.form {
  padding: 16px;
}

.qr-card {
  padding: 32px 16px;
  border-radius: 12px;
  background: #fff;
  text-align: center;
}

.mark {
  font-size: 24px;
  font-weight: 700;
  line-height: 2;
  color: #323233;
}

.notice {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.8;
  color: #646566;
}

.qr-image {
  display: block;
  width: 200px;
  height: 200px;
  margin: 20px auto;
}

.cancel-button {
  width: 80%;
  margin-top: 8px;
}

.detail-card {
  border-radius: 12px;
  overflow: hidden;
}
</style>

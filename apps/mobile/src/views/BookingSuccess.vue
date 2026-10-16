<template>
  <div class="page">
    <header class="signboard">
      <div>
        <p v-if="siteName" class="signboard-eyebrow">{{ siteName }}</p>
        <h1 class="signboard-title">{{ t('success.title') }}</h1>
      </div>
      <lang-switch />
    </header>

    <div class="form">
      <article v-if="booking.id" :class="['pass', `is-${display.state}`]">
        <div class="pass-head">
          <span class="pass-status">{{ display.label }}</span>
          <span class="pass-date">{{ booking.bookingDate }}</span>
        </div>
        <div class="pass-body">
          <div class="pass-park">{{ booking.park }}</div>
          <template v-if="display.canShowQr">
            <img v-if="qrDataUrl" :src="qrDataUrl" class="qr-image" alt="QR Code" />
            <p class="pass-notice">{{ t('success.notice1') }}<br />{{ t('success.notice2') }}</p>
          </template>
        </div>
        <div class="pass-tear" aria-hidden="true" />
        <dl class="pass-details">
          <div>
            <dt>{{ t('success.carPlate') }}</dt>
            <dd><span class="plate">{{ booking.carPlate }}</span></dd>
          </div>
          <div>
            <dt>{{ t('success.phone') }}</dt>
            <dd>{{ booking.cellPhone }}</dd>
          </div>
        </dl>
      </article>
      <van-empty v-else :description="t('success.noRecord')" />

      <van-button
        v-if="booking.id && display.canShowCancellationControl"
        type="danger"
        plain
        block
        :disabled="submitting"
        @click="cancelBooking"
      >
        {{ t('success.cancelBooking') }}
      </van-button>
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
import LangSwitch from '@/components/LangSwitch.vue'

const { locale, t, siteName } = useI18n()

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

.form {
  display: flex;
  flex-direction: column;
  gap: var(--np-space-4);
  padding: var(--np-space-4);
}

/* A paper permit: status band, QR, tear line, vehicle stub. */
.pass {
  background: var(--np-surface);
  border: 1px solid var(--np-line);
  border-radius: var(--np-radius-lg);
  overflow: hidden;
}

.pass-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--np-space-3);
  padding: var(--np-space-3) var(--np-space-4);
  background: var(--np-neutral-soft);
  color: var(--np-neutral);
}

.pass.is-active .pass-head {
  background: var(--np-success-soft);
  color: var(--np-success);
}

.pass-status {
  font-size: var(--np-text-lg);
  font-weight: 700;
}

.pass-date {
  font-size: var(--np-text-md);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.pass-body {
  padding: var(--np-space-5) var(--np-space-4);
  text-align: center;
}

.pass-park {
  font-size: var(--np-text-xl);
  font-weight: 700;
  color: var(--np-ink);
}

.qr-image {
  display: block;
  width: 200px;
  height: 200px;
  margin: var(--np-space-4) auto var(--np-space-3);
}

.pass-notice {
  margin: 0;
  font-size: var(--np-text-sm);
  line-height: 1.6;
  color: var(--np-ink-2);
}

.pass-tear {
  position: relative;
  margin: 0 var(--np-space-4);
  border-top: 2px dashed var(--np-line);
}

.pass-tear::before,
.pass-tear::after {
  content: '';
  position: absolute;
  top: -10px;
  width: 18px;
  height: 18px;
  border: 1px solid var(--np-line);
  border-radius: 50%;
  background: var(--np-paper);
}

.pass-tear::before {
  left: calc(-1 * var(--np-space-4) - 10px);
}

.pass-tear::after {
  right: calc(-1 * var(--np-space-4) - 10px);
}

.pass-details {
  display: flex;
  flex-direction: column;
  gap: var(--np-space-3);
  margin: 0;
  padding: var(--np-space-4);
}

.pass-details > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--np-space-3);
}

.pass-details dt {
  font-size: var(--np-text-sm);
  color: var(--np-ink-3);
}

.pass-details dd {
  margin: 0;
  font-size: var(--np-text-md);
  font-weight: 600;
  color: var(--np-ink);
  font-variant-numeric: tabular-nums;
}

/* Outlined like a real number plate so staff match it at a glance. */
.plate {
  display: inline-block;
  padding: 2px var(--np-space-2);
  border: 2px solid var(--np-ink);
  border-radius: var(--np-radius-sm);
  font-size: var(--np-text-lg);
  font-weight: 700;
  letter-spacing: 0.08em;
}
</style>

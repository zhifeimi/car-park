<template>
  <div class="page">
    <header class="signboard">
      <div>
        <p v-if="siteName" class="signboard-eyebrow">{{ siteName }}</p>
        <h1 class="signboard-title">{{ t('booking.title') }}</h1>
      </div>
      <lang-switch />
    </header>

    <div class="form">
      <section class="card">
        <van-field
          v-if="intl"
          v-model="form.carPlate"
          class="plate-field"
          :label="t('booking.carPlate')"
          :placeholder="t('booking.carPlatePlaceholderIntl')"
          maxlength="16"
          autocapitalize="characters"
          autocomplete="off"
          :spellcheck="false"
          required
          @blur="form.carPlate = normalizePlate(form.carPlate)"
        />
        <van-field
          v-else
          v-model="form.carPlate"
          class="plate-field"
          :label="t('booking.carPlate')"
          :placeholder="t('booking.carPlatePlaceholder')"
          readonly
          required
          is-link
          @click="showKeyboard = true"
        />
        <van-field
          v-model="form.cellPhone"
          :label="t('booking.cellPhone')"
          :placeholder="t(intl ? 'booking.cellPhonePlaceholderIntl' : 'booking.cellPhonePlaceholder')"
          type="tel"
          autocomplete="tel"
          :maxlength="intl ? 20 : 11"
          required
        />
      </section>

      <section class="card">
        <van-cell :title="t('booking.parkLocation')" required :border="false" />
        <van-radio-group v-model="form.park" class="park-radios">
          <van-radio v-for="park in parkList" :key="park.id" :name="park.name">
            {{ park.name }}
          </van-radio>
        </van-radio-group>
        <van-field
          v-model="form.bookingDate"
          :label="t('booking.bookingDate')"
          :placeholder="t('booking.selectDatePlaceholder')"
          readonly
          required
          is-link
          @click="showCalendar = true"
        />
        <div class="availability" aria-live="polite">
          <div>
            <div class="availability-label">{{ t('booking.leftLots') }}</div>
            <div class="availability-total">{{ t('booking.totalLots') }} {{ displayLotNum || '—' }}</div>
          </div>
          <strong :class="['availability-count', { full: displayLeftLotNum === '0' }]">
            {{ displayLeftLotNum || '—' }}
          </strong>
        </div>
      </section>

      <section class="card notice-card">
        <van-checkbox v-model="form.notice" shape="round">
          {{ t('booking.agreeNotice') }}
        </van-checkbox>
        <p v-for="(line, index) in noticeLines" :key="index" class="notice-text">
          {{ line }}
        </p>
      </section>
    </div>

    <div class="submit-bar">
      <van-button type="primary" size="large" block :loading="submitting" @click="submit">
        {{ t('booking.submit') }}
      </van-button>
    </div>

    <van-calendar
      v-model:show="showCalendar"
      :min-date="minDate"
      @confirm="onDateConfirm"
    />

    <plate-keyboard
      v-if="!intl"
      v-model:show="showKeyboard"
      :confirm-text="t('booking.done')"
      @input="onPlateKey"
      @backspace="onPlateBackspace"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { PlateKeyboard } from '@car-park/ui'
import { addBooking, getLeftLotNum, listParks } from '@/api/parking'
import {
  CN_MOBILE_REGEX,
  CN_PLATE_REGEX,
  E164_REGEX,
  formatDateTime,
  normalizePhone,
  normalizePlate,
  PHONE_STORAGE_KEY,
  PLATE_REGEX,
} from '@car-park/utils'
import { useI18n } from '@/locales'
import LangSwitch from '@/components/LangSwitch.vue'

const router = useRouter()
const { t, siteName } = useI18n()

// Deployment region (packages/ui/DESIGN.md). 'cn' (default, the deployed H5):
// province plate keyboard + strict mainland formats. 'intl': free-text plate
// in any script + phone with country code.
const intl = import.meta.env.VITE_REGION === 'intl'

const MAX_PLATE_LENGTH = 8 // new-energy plates are 8 chars; ordinary plates 7

const today = formatDateTime(new Date())

const parkList = ref([])
const selectedPark = reactive({ lotNum: null, leftLotNum: null })
const form = reactive({
  carPlate: '',
  cellPhone: '',
  park: '',
  bookingDate: today,
  notice: false,
})
const showKeyboard = ref(false)
const showCalendar = ref(false)
const submitting = ref(false)
const minDate = new Date()

const noticeLines = computed(() => t('booking.noticeLines'))

// Legacy behavior: before a park is picked the two fields show the
// totals summed across every lot; after picking one they track that lot
// (with live availability for the selected date).
const displayLotNum = computed(() =>
  selectedPark.lotNum == null ? '' : String(selectedPark.lotNum),
)
const displayLeftLotNum = computed(() =>
  selectedPark.leftLotNum == null ? '' : String(selectedPark.leftLotNum),
)

onMounted(async () => {
  try {
    const rows = await listParks()
    parkList.value = rows
    selectedPark.lotNum = rows.reduce((sum, p) => sum + Number(p.lotNum || 0), 0)
    selectedPark.leftLotNum = rows.reduce((sum, p) => sum + Number(p.leftLotNum || 0), 0)
  } catch {
    showFailToast(t('booking.fetchParkFailed'))
  }
})

watch(
  () => form.park,
  (park) => {
    if (!park) return
    if (form.bookingDate) {
      refreshLeftLotNum(park, form.bookingDate)
    } else {
      copyParkIntoSelected(park)
    }
  },
)

watch(
  () => form.bookingDate,
  (date) => {
    if (form.park && date) {
      refreshLeftLotNum(form.park, date)
    }
  },
)

function copyParkIntoSelected(parkName) {
  const found = parkList.value.find((p) => p.name === parkName)
  if (found) {
    // copy, so mutating selectedPark never mutates parkList rows
    selectedPark.lotNum = found.lotNum
    selectedPark.leftLotNum = found.leftLotNum
  }
}

// 查询停车场某日期剩余车位 — legacy getLeftLotNum, same endpoint/params.
async function refreshLeftLotNum(parkName, date) {
  copyParkIntoSelected(parkName)
  try {
    selectedPark.leftLotNum = await getLeftLotNum(parkName, date)
  } catch {
    // keep the copied value; the submit guard below still blocks a stale 0
  }
}

function onPlateKey(key) {
  if (form.carPlate.length < MAX_PLATE_LENGTH) {
    form.carPlate += key
  }
}

function onPlateBackspace() {
  form.carPlate = form.carPlate.slice(0, -1)
}

function onDateConfirm(date) {
  form.bookingDate = formatDateTime(date)
  showCalendar.value = false
}

function validate() {
  if (!form.carPlate) return t('booking.validation.plateRequired')
  if (intl ? !PLATE_REGEX.test(form.carPlate) : !CN_PLATE_REGEX.test(form.carPlate)) {
    return t(intl ? 'booking.validation.plateInvalidIntl' : 'booking.validation.plateInvalid')
  }
  if (!form.cellPhone) return t('booking.validation.phoneRequired')
  if (intl ? !E164_REGEX.test(form.cellPhone) : !CN_MOBILE_REGEX.test(form.cellPhone)) {
    return t(intl ? 'booking.validation.phoneInvalidIntl' : 'booking.validation.phoneInvalid')
  }
  if (!form.park) return t('booking.validation.parkRequired')
  if (!form.bookingDate) return t('booking.validation.dateRequired')
  if (!form.notice) return t('booking.validation.noticeRequired')
  return null
}

async function submit() {
  if (intl) {
    // what the server stores is also the /last lookup key, so normalize first
    form.carPlate = normalizePlate(form.carPlate)
    form.cellPhone = normalizePhone(form.cellPhone)
  }
  const error = validate()
  if (error) {
    showFailToast(error)
    return
  }
  if (selectedPark.leftLotNum === 0) {
    showFailToast(t('booking.validation.noLots'))
    return
  }
  submitting.value = true
  try {
    // The success page loads the booking by phone via /parking/booking/last
    // (legacy flow), so the phone must be stored before navigating.
    localStorage.setItem(PHONE_STORAGE_KEY, form.cellPhone)
    await addBooking({
      carPlate: form.carPlate,
      cellPhone: form.cellPhone,
      park: form.park,
      bookingDate: form.bookingDate,
    })
    showSuccessToast(t('booking.bookingSuccess'))
    setTimeout(() => router.push('/success'), 600)
  } catch (err) {
    submitting.value = false
    showFailToast(err?.message || t('booking.bookingFailed'))
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: calc(88px + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.form {
  display: flex;
  flex-direction: column;
  gap: var(--np-space-3);
  padding: var(--np-space-3) var(--np-space-4);
}

.card {
  background: var(--np-surface);
  border: 1px solid var(--np-line);
  border-radius: var(--np-radius-lg);
  overflow: hidden;
}

.plate-field :deep(.van-field__control:not(:placeholder-shown)) {
  font-weight: 600;
  letter-spacing: 0.08em;
}

.park-radios {
  display: flex;
  flex-direction: column;
  gap: var(--np-space-2);
  padding: 0 var(--np-space-4) var(--np-space-3);
}

.park-radios :deep(.van-radio) {
  min-height: var(--np-tap);
  padding: 0 var(--np-space-3);
  border: 1px solid var(--np-line);
  border-radius: var(--np-radius-md);
  font-size: var(--np-text-md);
}

.park-radios :deep(.van-radio[aria-checked='true']) {
  border-color: var(--np-brand);
  background: var(--np-brand-soft);
  font-weight: 600;
}

/* Availability reads like a car-park sign: one big number. */
.availability {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--np-space-3);
  padding: var(--np-space-3) var(--np-space-4);
  background: var(--np-brand-soft);
}

.availability-label {
  font-size: var(--np-text-md);
  font-weight: 600;
  color: var(--np-ink);
}

.availability-total {
  margin-top: 2px;
  font-size: var(--np-text-sm);
  color: var(--np-ink-2);
  font-variant-numeric: tabular-nums;
}

.availability-count {
  font-size: var(--np-text-2xl);
  font-weight: 800;
  line-height: 1;
  color: var(--np-success);
  font-variant-numeric: tabular-nums;
}

.availability-count.full {
  color: var(--np-danger);
}

.notice-card {
  padding: var(--np-space-4);
}

.notice-text {
  margin: var(--np-space-2) 0 0;
  font-size: var(--np-text-sm);
  line-height: 1.65;
  color: var(--np-ink-2);
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: var(--np-space-3) var(--np-space-4) calc(var(--np-space-3) + env(safe-area-inset-bottom));
  background: var(--np-paper);
  border-top: 1px solid var(--np-line);
  z-index: 99;
}
</style>

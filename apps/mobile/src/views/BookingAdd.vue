<template>
  <div class="page">
    <div class="header-bar">
      <h1 class="page-title">{{ t('booking.title') }}</h1>
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
      <div class="field-card">
        <van-field
          v-model="form.carPlate"
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
          :placeholder="t('booking.cellPhonePlaceholder')"
          type="tel"
          maxlength="11"
          required
        />
        <van-cell :title="t('booking.parkLocation')" required />
        <van-radio-group v-model="form.park" class="park-radios">
          <van-radio v-for="park in parkList" :key="park.id" :name="park.name">
            {{ park.name }}
          </van-radio>
        </van-radio-group>
        <van-field :model-value="displayLotNum" :label="t('booking.totalLots')" readonly />
        <van-field :model-value="displayLeftLotNum" :label="t('booking.leftLots')" readonly />
        <van-field
          v-model="form.bookingDate"
          :label="t('booking.bookingDate')"
          :placeholder="t('booking.selectDatePlaceholder')"
          readonly
          required
          is-link
          @click="showCalendar = true"
        />
      </div>

      <div class="notice-card">
        <van-checkbox v-model="form.notice" shape="round">
          {{ t('booking.agreeNotice') }}
        </van-checkbox>
        <p v-for="(line, index) in noticeLines" :key="index" class="notice-text">
          {{ line }}
        </p>
      </div>
    </div>

    <div class="submit-bar">
      <van-button type="primary" block round :loading="submitting" @click="submit">
        {{ t('booking.submit') }}
      </van-button>
    </div>

    <van-calendar
      v-model:show="showCalendar"
      :min-date="minDate"
      color="#1989fa"
      @confirm="onDateConfirm"
    />

    <plate-keyboard
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
import { MOBILE_REGEX, NOTICE_LINES_EN, NOTICE_LINES_ZH, PHONE_STORAGE_KEY, PLATE_REGEX, formatDateTime } from '@car-park/utils'
import { useI18n } from '@/locales'

const router = useRouter()
const { locale, setLocale, t } = useI18n()

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

const noticeLines = computed(() => {
  return locale.value === 'zh-CN' || locale.value === 'zh'
    ? NOTICE_LINES_ZH
    : NOTICE_LINES_EN
})

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
  if (!PLATE_REGEX.test(form.carPlate)) return t('booking.validation.plateInvalid')
  if (!form.cellPhone) return t('booking.validation.phoneRequired')
  if (!MOBILE_REGEX.test(form.cellPhone)) return t('booking.validation.phoneInvalid')
  if (!form.park) return t('booking.validation.parkRequired')
  if (!form.bookingDate) return t('booking.validation.dateRequired')
  if (!form.notice) return t('booking.validation.noticeRequired')
  return null
}

async function submit() {
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
  padding-bottom: calc(80px + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #323233;
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
  padding: 12px 16px;
}

.field-card {
  border-radius: 12px;
  overflow: hidden;
}

.park-radios {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px 16px;
  background: #fff;
}

.notice-card {
  margin-top: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

.notice-text {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #646566;
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: #f7f8fa;
  z-index: 99;
}
</style>

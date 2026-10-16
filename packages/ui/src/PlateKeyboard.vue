<template>
  <van-popup v-model:show="show" position="bottom" round :close-on-click-overlay="true">
    <div class="keyboard">
      <div class="kb-row">
        <button
          v-for="key in PROVINCES"
          :key="key"
          class="kb-key kb-key-province"
          @click="onKey(key)"
        >
          {{ key }}
        </button>
      </div>
      <div class="kb-row">
        <button
          v-for="key in LETTERS_ROW_1"
          :key="key"
          class="kb-key"
          @click="onKey(key)"
        >
          {{ key }}
        </button>
      </div>
      <div class="kb-row">
        <button
          v-for="key in LETTERS_ROW_2"
          :key="key"
          class="kb-key"
          @click="onKey(key)"
        >
          {{ key }}
        </button>
      </div>
      <div class="kb-row">
        <button
          v-for="key in DIGITS"
          :key="key"
          class="kb-key"
          @click="onKey(key)"
        >
          {{ key }}
        </button>
        <button class="kb-key kb-key-backspace" @click="emit('backspace')">
          <van-icon name="delete-o" size="20" />
        </button>
      </div>
      <div class="kb-row">
        <button class="kb-key kb-key-confirm" @click="show = false">{{ confirmText }}</button>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { PROVINCES, LETTERS_ROW_1, LETTERS_ROW_2, DIGITS } from '@car-park/utils'

const show = defineModel('show', { type: Boolean, default: false })
const props = defineProps({
  confirmText: {
    type: String,
    default: 'Done',
  },
})
const emit = defineEmits(['input', 'backspace'])

function onKey(key) {
  emit('input', key)
}
</script>

<style scoped>
.keyboard {
  padding: 8px 4px calc(8px + env(safe-area-inset-bottom));
  background: var(--np-sunken, #d7dade);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kb-row {
  display: flex;
  justify-content: center;
  gap: 4px;
}

.kb-key {
  flex: 1 1 0;
  min-width: 0;
  height: 42px;
  border: none;
  border-radius: 5px;
  background: #fff;
  font-size: 16px;
  color: var(--np-ink, #323233);
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.15);
  -webkit-tap-highlight-color: transparent;
}

.kb-key:active {
  background: var(--np-line, #e8e8e8);
}

.kb-key-province {
  font-size: 14px;
}

.kb-key-backspace {
  flex: 1.6 1 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kb-key-confirm {
  flex: 1 1 0;
  height: 44px;
  background: var(--np-brand, #1989fa);
  color: #fff;
  font-size: 17px;
}

.kb-key-confirm:active {
  background: var(--np-brand-strong, #1472d4);
}
</style>

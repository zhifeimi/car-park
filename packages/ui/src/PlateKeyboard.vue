<template>
  <van-popup v-model:show="show" position="bottom" round :close-on-click-overlay="true">
    <div class="keyboard">
      <!-- Provinces: 31 keys need their own panel. In one row they shrink to ~12px and overlap. -->
      <div v-if="panel === 'province'" class="kb-grid">
        <button
          v-for="key in PROVINCES"
          :key="key"
          class="kb-key kb-key-province"
          @click="onKey(key)"
        >
          {{ key }}
        </button>
        <button class="kb-key kb-key-backspace" :aria-label="backspaceText" @click="emit('backspace')">
          <van-icon name="delete-o" size="20" />
        </button>
      </div>

      <template v-else>
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
          <button class="kb-key kb-key-backspace" :aria-label="backspaceText" @click="emit('backspace')">
            <van-icon name="delete-o" size="20" />
          </button>
        </div>
      </template>

      <div class="kb-row">
        <button class="kb-key kb-key-switch" @click="togglePanel">
          {{ panel === 'province' ? lettersText : provincesText }}
        </button>
        <button class="kb-key kb-key-confirm" @click="show = false">{{ confirmText }}</button>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { ref, watch } from 'vue'
import { DIGITS, LETTERS_ROW_1, LETTERS_ROW_2, PROVINCES } from '@car-park/utils'

const show = defineModel('show', { type: Boolean, default: false })
const props = defineProps({
  /** The plate typed so far. The province panel shows while it is empty, as a plate starts with a province. */
  plate: { type: String, default: '' },
  confirmText: { type: String, default: 'Done' },
  /** Switch-key labels and the backspace key's accessible name */
  lettersText: { type: String, default: 'ABC' },
  provincesText: { type: String, default: '省' },
  backspaceText: { type: String, default: 'Delete' },
})
const emit = defineEmits(['input', 'backspace'])

// Follows the plate: empty → provinces, after the first character → letters and digits.
// The switch key overrides it until the plate next becomes empty or non-empty.
const panel = ref('province')
watch(
  () => props.plate.length === 0,
  (empty) => {
    panel.value = empty ? 'province' : 'alnum'
  },
  { immediate: true },
)

function togglePanel() {
  panel.value = panel.value === 'province' ? 'alnum' : 'province'
}

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

/* 8 columns × 4 rows = 31 provinces + backspace, every key the same width */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 6px 4px;
}

.kb-key {
  flex: 1 1 0;
  min-width: 0;
  height: 42px;
  padding: 0;
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
  font-size: 17px;
}

.kb-key-backspace {
  flex: 1.6 1 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kb-key-switch {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}

.kb-key-confirm {
  flex: 2.5 1 0;
  height: 44px;
  background: var(--np-brand, #1989fa);
  color: #fff;
  font-size: 17px;
}

.kb-key-confirm:active {
  background: var(--np-brand-strong, #1472d4);
}
</style>

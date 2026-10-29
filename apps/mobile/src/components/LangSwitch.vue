<template>
  <div class="lang-switch">
    <button
      v-for="option in localeOptions"
      :key="option.code"
      type="button"
      :lang="option.code"
      :class="['lang-btn', { active: locale === option.code }]"
      @click="setLocale(option.code)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script setup>
import { localeOptions, useI18n } from '@/locales'

const { locale, setLocale } = useI18n()
</script>

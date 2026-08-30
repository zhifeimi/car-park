<template>
  <div class="pagination-container">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      background
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  total: { type: Number, required: true },
  page: { type: Number, default: 1 },
  limit: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
})

const emit = defineEmits(['update:page', 'update:limit', 'pagination'])

const currentPage = computed({
  get: () => props.page,
  set: (value) => emit('update:page', value),
})

const pageSize = computed({
  get: () => props.limit,
  set: (value) => emit('update:limit', value),
})

function handleSizeChange(value) {
  if (currentPage.value * value > props.total) {
    currentPage.value = 1
  }
  emit('pagination', { page: currentPage.value, limit: value })
}

function handleCurrentChange(value) {
  emit('pagination', { page: value, limit: pageSize.value })
}
</script>

<style scoped>
.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

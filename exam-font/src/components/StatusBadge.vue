<template>
  <span :class="[
    'px-3 py-1 rounded-full text-xs font-medium inline-flex items-center',
    type === 'success' ? 'bg-green-500/20 text-green-400' :
    type === 'warning' ? 'bg-yellow-500/20 text-yellow-400' :
    type === 'danger' ? 'bg-red-500/20 text-red-400' :
    type === 'info' ? 'bg-blue-500/20 text-blue-400' :
    'bg-gray-500/20 text-gray-400'
  ]">
    <span v-if="icon" class="mr-1">{{ icon }}</span>
    <slot></slot>
  </span>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'info' },
  icon: { type: String, default: '' }
})
</script>

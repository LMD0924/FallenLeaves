<template>
  <div class="animate-pulse">
    <div class="space-y-3">
      <div v-for="i in lines" :key="i"
           :class="[
             'rounded-lg bg-white/10',
             i === 1 && 'h-8',
             i === 2 && 'h-4 w-3/4',
             i === 3 && 'h-4 w-1/2'
           ]"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  lines: { type: Number, default: 3 }
})
</script>

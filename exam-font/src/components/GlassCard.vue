<template>
  <div :class="[
    'backdrop-blur-md bg-gradient-to-br from-white/5 to-white/3 rounded-2xl border transition-all duration-300',
    hoverable ? 'hover:border-indigo-400/30 hover:from-white/8 hover:to-white/5 cursor-pointer' : '',
    borderColor || 'border-white/10',
    customClass
  ]" @click="handleClick">
    <slot></slot>
  </div>
</template>

<script setup>
defineProps({
  hoverable: { type: Boolean, default: true },
  borderColor: { type: String, default: '' },
  customClass: { type: String, default: '' }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  emit('click')
}
</script>

import { ref, computed, watch, onMounted } from 'vue'

// 随机颜色生成函数
const generateRandomColor = () => {
  // 生成一个随机的主色调 (HSL)
  const hue = Math.floor(Math.random() * 360);
  // 设置固定的饱和度和亮度，确保颜色美观
  const saturation = 70;
  const lightness = 55;
  
  return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
};

// 从localStorage获取主题设置，如果没有则默认使用暗色主题
const isDark = ref(localStorage.getItem('theme') === 'dark' ||
  (localStorage.getItem('theme') === null && window.matchMedia('(prefers-color-scheme: dark)').matches) ||
  false)

// 生成随机主色调
const randomPrimaryColor = ref(generateRandomColor());

// 根据随机主色调生成主题配置
const generateTheme = (dark = false) => {
  const baseTheme = dark ? darkThemeBase : lightThemeBase;
  
  // 生成基于随机主色调的主题
  const theme = JSON.parse(JSON.stringify(baseTheme));
  
  // 更新主色调
  theme.colors.primary = randomPrimaryColor.value;
  
  // 从HSL颜色值中提取色相、饱和度和亮度
  const hslMatch = randomPrimaryColor.value.match(/hsl\((\d+),\s*(\d+)%,\s*(\d+)%\)/);
  if (hslMatch) {
    const [, hue, saturation, lightness] = hslMatch;
    
    // 创建半透明的HSLA颜色
    theme.effects.glow = `0 0 20px hsla(${hue}, ${saturation}%, ${lightness}%, 0.3)`;
    theme.effects.glowLg = `0 0 40px hsla(${hue}, ${saturation}%, ${lightness}%, 0.4)`;
    
    // 生成与主色调相关的辅助色，使主题变化更明显
    const secondaryHue = (parseInt(hue) + 60) % 360;
    const accentHue = (parseInt(hue) + 120) % 360;
    theme.colors.secondary = `hsl(${secondaryHue}, ${saturation}%, ${lightness}%)`;
    theme.colors.accent = `hsl(${accentHue}, ${saturation}%, ${lightness}%)`;
  }
  
  return theme;
};

// 基础主题配置（不包含随机颜色）
const lightThemeBase = {
  name: 'light',
  colors: {
    primary: '#3b82f6', // 默认颜色，会被随机颜色替换
    secondary: '#8b5cf6',
    accent: '#06b6d4',
    success: '#10b981',
    warning: '#f59e0b',
    error: '#ef4444',
    background: '#ffffff',
    surface: '#f8fafc',
    surfaceVariant: '#f1f5f9',
    text: '#1e293b',
    textSecondary: '#64748b',
    textMuted: '#94a3b8',
    border: '#e2e8f0',
    borderLight: '#f1f5f9',
    shadow: 'rgba(0, 0, 0, 0.1)',
    shadowDark: 'rgba(0, 0, 0, 0.2)',
    glass: 'rgba(255, 255, 255, 0.8)',
    glassDark: 'rgba(255, 255, 255, 0.1)',
    gradient: {
      primary: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      secondary: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
      accent: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
      success: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
      warm: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
      cool: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
      dark: 'linear-gradient(135deg, #2c3e50 0%, #3498db 100%)',
      sunset: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
      ocean: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      forest: 'linear-gradient(135deg, #134e5e 0%, #71b280 100%)'
    }
  },
  effects: {
    blur: 'blur(20px)',
    shadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    shadowLg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
    shadowXl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
    glow: '0 0 20px rgba(59, 130, 246, 0.3)',
    glowLg: '0 0 40px rgba(59, 130, 246, 0.4)'
  }
}

const darkThemeBase = {
  name: 'dark',
  colors: {
    primary: '#60a5fa', // 默认颜色，会被随机颜色替换
    secondary: '#a78bfa',
    accent: '#22d3ee',
    success: '#34d399',
    warning: '#fbbf24',
    error: '#f87171',
    background: '#000000', // 改为纯黑色
    surface: '#0a0a0a', // 深黑色
    surfaceVariant: '#1a1a1a', // 较深的黑色
    text: '#f8fafc',
    textSecondary: '#cbd5e1',
    textMuted: '#94a3b8',
    border: '#2a2a2a', // 深灰色边框
    borderLight: '#333333', // 稍亮的深灰色
    shadow: 'rgba(0, 0, 0, 0.5)', // 更深的阴影
    shadowDark: 'rgba(0, 0, 0, 0.7)', // 更深的阴影
    glass: 'rgba(0, 0, 0, 0.8)', // 黑色玻璃效果
    glassDark: 'rgba(0, 0, 0, 0.3)', // 深黑色玻璃效果
    gradient: {
      primary: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      secondary: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
      accent: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
      success: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
      warm: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
      cool: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
      dark: 'linear-gradient(135deg, #000000 0%, #1a1a2e 100%)', // 黑色渐变
      sunset: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
      ocean: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      forest: 'linear-gradient(135deg, #134e5e 0%, #71b280 100%)'
    }
  },
  effects: {
    blur: 'blur(20px)',
    shadow: '0 4px 6px -1px rgba(0, 0, 0, 0.5), 0 2px 4px -1px rgba(0, 0, 0, 0.3)',
    shadowLg: '0 10px 15px -3px rgba(0, 0, 0, 0.5), 0 4px 6px -2px rgba(0, 0, 0, 0.3)',
    shadowXl: '0 20px 25px -5px rgba(0, 0, 0, 0.6), 0 10px 10px -5px rgba(0, 0, 0, 0.3)',
    glow: '0 0 20px rgba(96, 165, 250, 0.4)',
    glowLg: '0 0 40px rgba(96, 165, 250, 0.5)'
  }
}

// 主题配置（基于随机主色调）
const lightTheme = ref(generateTheme(false));
const darkTheme = ref(generateTheme(true));

// 当前主题
const currentTheme = computed(() => isDark.value ? darkTheme.value : lightTheme.value)

// 切换主题
const toggleTheme = () => {
  isDark.value = !isDark.value
  // 保存到localStorage
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  updateDocumentTheme()
}

// 设置主题
const setTheme = (theme) => {
  isDark.value = theme === 'dark'
  // 保存到localStorage
  localStorage.setItem('theme', theme)
  updateDocumentTheme()
}

// 重置随机主题
const resetRandomTheme = () => {
  console.log('🔄 重置随机主题');
  // 生成新的随机主色调
  randomPrimaryColor.value = generateRandomColor();
  console.log('🎨 新的随机主色调:', randomPrimaryColor.value);
  
  // 更新主题
  lightTheme.value = generateTheme(false);
  darkTheme.value = generateTheme(true);
  
  // 应用更新
  updateDocumentTheme();
  console.log('✅ 随机主题已更新');
}

// 初始化主题
const initTheme = () => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme) {
    isDark.value = savedTheme === 'dark'
  } else {
    // 如果没有保存的主题，使用系统偏好
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    isDark.value = prefersDark
    localStorage.setItem('theme', prefersDark ? 'dark' : 'light')
  }
  updateDocumentTheme()
}

// 更新文档主题
const updateDocumentTheme = () => {
  const root = document.documentElement
  const theme = currentTheme.value

  // 设置CSS变量
  Object.entries(theme.colors).forEach(([key, value]) => {
    if (typeof value === 'object') {
      Object.entries(value).forEach(([subKey, subValue]) => {
        root.style.setProperty(`--color-${key}-${subKey}`, subValue)
      })
    } else {
      root.style.setProperty(`--color-${key}`, value)
    }
  })

  Object.entries(theme.effects).forEach(([key, value]) => {
    root.style.setProperty(`--effect-${key}`, value)
  })

  // 设置HTML类名
  if (isDark.value) {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
}

// 监听系统主题变化
const watchSystemTheme = () => {
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', (e) => {
    // 只有在没有手动设置过主题时才跟随系统
    if (!localStorage.getItem('theme')) {
      isDark.value = e.matches
      updateDocumentTheme()
    }
  })
}

// 组件挂载时初始化主题
onMounted(() => {
  initTheme()
  watchSystemTheme()
})

// 监听主题变化
watch(isDark, () => {
  updateDocumentTheme()
}, { immediate: true })

// 动画配置
const animations = {
  fadeIn: {
    enter: 'transition-opacity duration-300 ease-out',
    enterFrom: 'opacity-0',
    enterTo: 'opacity-100',
    leave: 'transition-opacity duration-200 ease-in',
    leaveFrom: 'opacity-100',
    leaveTo: 'opacity-0'
  },
  slideIn: {
    enter: 'transition-all duration-300 ease-out',
    enterFrom: 'opacity-0 transform translate-y-4',
    enterTo: 'opacity-100 transform translate-y-0',
    leave: 'transition-all duration-200 ease-in',
    leaveFrom: 'opacity-100 transform translate-y-0',
    leaveTo: 'opacity-0 transform translate-y-4'
  },
  scaleIn: {
    enter: 'transition-all duration-300 ease-out',
    enterFrom: 'opacity-0 transform scale-95',
    enterTo: 'opacity-100 transform scale-100',
    leave: 'transition-all duration-200 ease-in',
    leaveFrom: 'opacity-100 transform scale-100',
    leaveTo: 'opacity-0 transform scale-95'
  },
  slideUp: {
    enter: 'transition-all duration-500 ease-out',
    enterFrom: 'opacity-0 transform translate-y-8',
    enterTo: 'opacity-100 transform translate-y-0',
    leave: 'transition-all duration-300 ease-in',
    leaveFrom: 'opacity-100 transform translate-y-0',
    leaveTo: 'opacity-0 transform translate-y-8'
  }
}

// 玻璃拟态效果 - 更新为黑色主题
const glassMorphism = {
  light: 'bg-white/80 backdrop-blur-xl border border-white/20',
  dark: 'bg-black/80 backdrop-blur-xl border border-white/10', // 改为黑色
  primary: 'bg-blue-500/20 backdrop-blur-xl border border-blue-500/30',
  secondary: 'bg-purple-500/20 backdrop-blur-xl border border-purple-500/30'
}

// 渐变背景
const gradients = {
  primary: 'bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500',
  secondary: 'bg-gradient-to-br from-cyan-400 via-blue-500 to-purple-600',
  warm: 'bg-gradient-to-br from-orange-400 via-pink-500 to-red-500',
  cool: 'bg-gradient-to-br from-cyan-400 via-blue-500 to-indigo-600',
  dark: 'bg-gradient-to-br from-black via-gray-900 to-purple-900', // 改为黑色渐变
  sunset: 'bg-gradient-to-br from-yellow-400 via-orange-500 to-pink-500',
  ocean: 'bg-gradient-to-br from-blue-400 via-cyan-500 to-teal-500',
  forest: 'bg-gradient-to-br from-green-400 via-emerald-500 to-teal-600'
}

export {
  isDark,
  currentTheme,
  toggleTheme,
  setTheme,
  resetRandomTheme,
  animations,
  glassMorphism,
  gradients,
  lightTheme,
  darkTheme,
  initTheme
}


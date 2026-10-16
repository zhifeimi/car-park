import { createApp } from 'vue'
import Vant from 'vant'
import 'vant/lib/index.css'
import '../../../packages/ui/src/tokens.css'
import App from './App.vue'
import router from './router'
import './styles/index.css'
import { initLocale } from './locales'

// Per-park biome preset (see packages/ui/DESIGN.md)
document.documentElement.dataset.park = import.meta.env.VITE_PARK_THEME || 'forest'

initLocale()

createApp(App).use(Vant).use(router).mount('#app')

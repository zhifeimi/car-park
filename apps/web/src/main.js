import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '../../../packages/ui/src/tokens.css'
import './styles/index.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores/pinia'

// Per-park biome preset (see packages/ui/DESIGN.md)
document.documentElement.dataset.park = import.meta.env.VITE_PARK_THEME || 'forest'

const app = createApp(App)
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')

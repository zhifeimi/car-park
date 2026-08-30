import { createApp } from 'vue'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'
import './styles/index.css'
import { initLocale } from './locales'

initLocale()

createApp(App).use(Vant).use(router).mount('#app')

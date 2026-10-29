import { watch } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import BookingAdd from '@/views/BookingAdd.vue'
import BookingSuccess from '@/views/BookingSuccess.vue'
import { getLocale, siteName, t } from '@/locales'

// Hash history + Vite's base ('/mobile/' by default) = parity with the deployed H5
// (park.example.com/mobile/, hash mode in the legacy uni-app manifest). The Vercel
// build passes --base / to serve from the domain root.
const routes = [
  {
    path: '/',
    name: 'booking-add',
    component: BookingAdd,
    meta: { title: 'booking.title' },
  },
  {
    path: '/success',
    name: 'booking-success',
    component: BookingSuccess,
    meta: { title: 'success.title' },
  },
]

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
})

// Locale-aware (VITE_SITE_NAME_EN for English visitors) and refreshed on a
// language switch, not only on the next navigation.
function setTitle(route) {
  if (route.meta.title) {
    const site = siteName.value
    document.title = site ? `${t(route.meta.title)} · ${site}` : t(route.meta.title)
  }
}

router.afterEach(setTitle)
watch(getLocale, () => setTitle(router.currentRoute.value))

export default router

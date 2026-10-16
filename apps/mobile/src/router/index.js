import { createRouter, createWebHashHistory } from 'vue-router'
import BookingAdd from '@/views/BookingAdd.vue'
import BookingSuccess from '@/views/BookingSuccess.vue'
import { t } from '@/locales'

// Hash history + '/mobile/' base = parity with the deployed H5
// (park.example.com/mobile/, hash mode in the legacy uni-app manifest).
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
  history: createWebHashHistory('/mobile/'),
  routes,
})

router.afterEach((to) => {
  if (to.meta.title) {
    const site = import.meta.env.VITE_SITE_NAME
    document.title = site ? `${t(to.meta.title)} · ${site}` : t(to.meta.title)
  }
})

export default router

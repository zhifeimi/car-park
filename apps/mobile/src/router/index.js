import { createRouter, createWebHashHistory } from 'vue-router'
import BookingAdd from '@/views/BookingAdd.vue'
import BookingSuccess from '@/views/BookingSuccess.vue'

// Hash history + '/mobile/' base = parity with the deployed H5
// (park.example.com/mobile/, hash mode in the legacy uni-app manifest).
const routes = [
  {
    path: '/',
    name: 'booking-add',
    component: BookingAdd,
    meta: { title: '停车场预约' },
  },
  {
    path: '/success',
    name: 'booking-success',
    component: BookingSuccess,
    meta: { title: '预约成功' },
  },
]

const router = createRouter({
  history: createWebHashHistory('/mobile/'),
  routes,
})

router.afterEach((to) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
})

export default router

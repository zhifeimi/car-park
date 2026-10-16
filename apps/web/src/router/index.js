import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '@/stores/pinia'
import { useUserStore } from '@/stores/user'
import { t } from '@/locales'
import Layout from '@/layout/Layout.vue'
import Login from '@/views/login/index.vue'
import Dashboard from '@/views/dashboard/index.vue'
import Park from '@/views/park/index.vue'
import Booking from '@/views/booking/index.vue'
import User from '@/views/user/index.vue'

// Static routes — no DB-driven sys_menu anymore (single admin role).
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: 'login.pageTitle' },
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: 'nav.dashboard' },
      },
      {
        path: 'park',
        name: 'Park',
        component: Park,
        meta: { title: 'nav.park' },
      },
      {
        path: 'booking',
        name: 'Booking',
        component: Booking,
        meta: { title: 'nav.booking' },
      },
      {
        path: 'user',
        name: 'User',
        component: User,
        meta: { title: 'nav.user' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const userStore = useUserStore(pinia)
  if (to.path !== '/login' && !userStore.token) {
    return '/login'
  }
  if (to.path === '/login' && userStore.token) {
    return '/'
  }
})

router.afterEach((to) => {
  if (to.meta?.title) {
    document.title = `${t(to.meta.title)} - ${import.meta.env.VITE_SITE_NAME || t('nav.systemTitle')}`
  }
})

export default router

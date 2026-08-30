import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '@/stores/pinia'
import { useUserStore } from '@/stores/user'
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
    meta: { title: '登录' },
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
        meta: { title: '首页' },
      },
      {
        path: 'park',
        name: 'Park',
        component: Park,
        meta: { title: '停车场管理' },
      },
      {
        path: 'booking',
        name: 'Booking',
        component: Booking,
        meta: { title: '预约记录' },
      },
      {
        path: 'user',
        name: 'User',
        component: User,
        meta: { title: '管理员' },
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
    document.title = `${to.meta.title} - 停车场预约管理系统`
  }
})

export default router

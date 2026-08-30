import { defineStore } from 'pinia'
import { login as loginApi } from '../api/auth.js'

const TOKEN_KEY = 'Admin-Token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    username: '',
    nickname: '',
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
  },
  actions: {
    async login(username, password) {
      const data = await loginApi({ username, password })
      this.token = data.token
      this.username = data.username
      this.nickname = data.nickname || ''
      localStorage.setItem(TOKEN_KEY, data.token)
    },
    setProfile(profile) {
      this.username = profile.username
      this.nickname = profile.nickname || ''
    },
    logout() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      localStorage.removeItem(TOKEN_KEY)
    },
  },
})

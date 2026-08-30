import { createPinia } from 'pinia'

// Shared instance for code that executes outside a component setup context
// (router guards and Axios interceptors).
export const pinia = createPinia()

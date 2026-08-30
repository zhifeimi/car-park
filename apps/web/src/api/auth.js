/**
 * Auth domain client — login, profile, admin account lifecycle. Same
 * contract as parking.js: domain data out, void mutations, Error(server msg)
 * on business failure.
 */
import { request } from './http.js'

/** POST /auth/login — {token, username, nickname}. */
export const login = (data) => request({ url: '/auth/login', method: 'post', data })

/** GET /auth/me — the current admin's profile. */
export const getMe = () => request({ url: '/auth/me', method: 'get' })

/** POST /auth/password — void. */
export const changePassword = (data) => request({ url: '/auth/password', method: 'post', data })

/** GET /auth/users — all admin accounts. */
export const listUsers = () => request({ url: '/auth/users', method: 'get' })

/** POST /auth/users — void. */
export const addUser = (data) => request({ url: '/auth/users', method: 'post', data })

/** DELETE /auth/users/{id} — void. */
export const deleteUser = (id) => request({ url: `/auth/users/${id}`, method: 'delete' })

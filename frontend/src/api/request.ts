import axios, { type AxiosError, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'
import type { Result } from './types'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

/** 请求拦截器：自动携带令牌 */
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 响应拦截器：拆包业务码，统一错误提示 */
request.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const body = response.data

    // 非 JSON 响应（如文件流）直接透传
    if (!body || typeof body.code === 'undefined') {
      return response as unknown as AxiosResponse<Result>
    }

    if (body.code === 200) {
      // 返回统一响应体 Result，业务层再通过 res.data 取真正的 data
      return body as unknown as AxiosResponse<Result>
    }

    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error: AxiosError<Result>) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === 401) {
      clearAuth()
      ElMessage.error('登录已过期，请重新登录')
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
      return Promise.reject(error)
    }

    if (status === 403) {
      ElMessage.error(message || '没有操作权限')
    } else if (status === 404) {
      ElMessage.error(message || '请求的资源不存在')
    } else if (status && status >= 500) {
      ElMessage.error('服务器异常，请稍后重试')
    } else {
      ElMessage.error(message || error.message || '网络异常，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default request

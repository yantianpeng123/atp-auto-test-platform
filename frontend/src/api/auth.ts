import request from './request'
import type {
  CaptchaData,
  LoginData,
  LoginParams,
  RegisterParams,
  Result,
  UserInfo
} from './types'

/** 登录 */
export function login(data: LoginParams): Promise<LoginData> {
  return request.post<unknown, Result<LoginData>>('/auth/login', data).then((res) => res.data)
}

/** 注册 */
export function register(data: RegisterParams): Promise<null> {
  return request.post<unknown, Result<null>>('/auth/register', data).then((res) => res.data)
}

/** 登出 */
export function logout(): Promise<null> {
  return request.post<unknown, Result<null>>('/auth/logout').then((res) => res.data)
}

/** 获取图形验证码 */
export function getCaptcha(): Promise<CaptchaData> {
  return request.get<unknown, Result<CaptchaData>>('/auth/captcha').then((res) => res.data)
}

/** 当前登录用户信息 */
export function getUserInfo(): Promise<UserInfo> {
  return request.get<unknown, Result<UserInfo>>('/user/info').then((res) => res.data)
}

/** 用户名是否可用 */
export function checkUsername(username: string): Promise<boolean> {
  return request
    .get<unknown, Result<boolean>>('/user/check-username', { params: { username } })
    .then((res) => res.data)
}

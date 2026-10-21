/**
 * English for the backend's user-facing messages. The server answers in
 * Chinese and those strings are frozen with the API contract (the deployed H5
 * shows them), so the clients translate here instead of the server.
 * server-messages.test.ts fails when the backend gains a message this table
 * lacks. Locales other than zh-CN and en fall back to English, which beats
 * Chinese for a worldwide audience.
 */
export const SERVER_MESSAGES_EN: Record<string, string> = {
  // public booking flow
  '车牌号不能为空': 'Please enter the license plate',
  '车牌号格式错误': 'The license plate contains characters that are not allowed',
  '手机号不能为空': 'Please enter a phone number',
  '手机号格式错误': 'Enter the full mobile number, with country code if outside mainland China',
  '停车场不能为空': 'Please choose a parking lot',
  '预约日期不能为空': 'Please choose a date',
  '预约日期格式错误，应为 yyyy-MM-dd': 'Dates must be written as yyyy-MM-dd',
  '预约日期不能早于今天': 'The date cannot be in the past',
  '该车牌在所选日期已有生效的预约记录': 'This vehicle already has a booking on that date',
  '停车场不存在': 'That parking lot no longer exists',
  '所选日期不在停车场开放预约范围内': 'This lot does not take bookings on that date',
  '该停车场当日预约名额已满': 'This lot is fully booked on that date',
  '预约记录不存在': 'Booking not found',
  '预约记录id不能为空': 'Booking ID is required',
  '不支持修改预约记录内容': 'A booking can be cancelled but not edited',
  '该预约记录已取消或已失效': 'This booking is already cancelled or no longer valid',
  '无法取消已过期的历史预约': 'Past bookings cannot be cancelled',
  '请求过于频繁，请稍后再试': 'Too many requests. Wait a minute and try again',
  '请求体格式错误': 'The request could not be read',
  '系统未知错误，请反馈给管理员': 'Something went wrong. Please tell the park office',

  // admin console
  '用户名不能为空': 'Username is required',
  '密码不能为空': 'Password is required',
  '旧密码不能为空': 'Current password is required',
  '新密码不能为空': 'New password is required',
  '密码长度必须在8到64个字符之间': 'Password must be 8 to 64 characters',
  '新密码长度必须在8到64个字符之间': 'New password must be 8 to 64 characters',
  '用户名只能包含字母、数字和下划线，长度3到30': 'Username: 3 to 30 letters, digits or underscores',
  '用户名或密码错误': 'Wrong username or password',
  '用户不存在': 'User not found',
  '旧密码错误': 'Current password is wrong',
  '用户名已存在': 'That username is taken',
  '不能删除当前登录的管理员': 'You cannot delete the account you are signed in with',
  '不能删除最后一个管理员': 'The last admin account cannot be deleted',
  '停车场名称不能为空': 'Parking lot name is required',
  '停车场名称不能超过100个字符': 'Parking lot name can be at most 100 characters',
  '停车场名称已存在': 'A parking lot with that name already exists',
  '停车场id不能为空': 'Parking lot ID is required',
  '可预约车位总数不能为空': 'Number of bookable spaces is required',
  '可预约车位总数不能为负数': 'Number of bookable spaces cannot be negative',
  '可预约起始日期不能晚于结束日期': 'The booking window must start before it ends',
  '每天开放时间必须早于关闭时间': 'Opening time must be before closing time',
}

/** The server's text in the visitor's language: unchanged for zh-CN, else English if known. */
export function localizeServerMessage(msg: string | undefined, locale: string): string | undefined {
  if (!msg || locale === 'zh-CN') return msg
  return SERVER_MESSAGES_EN[msg] ?? msg
}

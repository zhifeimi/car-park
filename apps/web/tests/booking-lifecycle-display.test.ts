import { bookingLifecycleDisplay } from '@car-park/utils'

Deno.test('web resolves the shared Booking Lifecycle Display seam', () => {
  const display = bookingLifecycleDisplay(-1, 'zh-CN')
  if (display.label !== '已取消' || display.treatment !== 'neutral') {
    throw new Error('web import-map resolution or shared cancelled display is wrong')
  }
})

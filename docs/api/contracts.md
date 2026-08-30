# API Contracts & Surface Matrix

## Response Envelopes

All business API endpoints return **HTTP 200** with an envelope structure, even for business failures (e.g. `code: 500` or validation error messages). Only authentication failures (`401`), forbidden access (`403`), and rate limiting (`429`) use real HTTP error statuses.

### 1. Standard Result Envelope (`R<T>`)
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```

- `code: 200` — Business success.
- `code: 500` — Business failure (e.g. `该停车场在所选日期车位已满`, `不支持修改预约记录内容`).

### 2. Paginated Table Data Envelope (`TableData<T>`)
```json
{
  "total": 42,
  "rows": [ { ... } ],
  "code": 200,
  "msg": "查询成功"
}
```

---

## Anonymous Surface Matrix (Public Endpoints)

The public surface is strictly pinned to **exactly seven endpoints** in `SecurityConfig` (verified by `AnonymousSurfaceMatrixTest`):

1. `POST /auth/login` — Staff login
2. `GET /parking/park/list` — Public parking lot listing
3. `GET /parking/park/leftLotNum` — Live slot availability query
4. `GET /parking/booking/list` — Booking status lookup
5. `GET /parking/booking/last` — Latest booking for citizen success page
6. `POST /parking/booking` — Public booking intake (rate limited to 10/min/IP)
7. `PUT /parking/booking` — Public cancellation (`status: -1` ONLY)

> **Security Note**: Any other mutation or data access requires a valid JWT `Authorization: Bearer <token>` header.

---

## Staff QR Scanner Contract

The QR code displayed on `apps/mobile` (`BookingSuccess.vue`) encodes a JSON string with exact Chinese keys:

```json
{
  "车牌号": "粤A12345",
  "手机号": "13800138000",
  "停车场": "黄麻社区停车场",
  "日期": "2026-08-21"
}
```

Gate scanners directly parse these four fields. Semantics, casing, and key names must remain identical across frontends and mobile versions.

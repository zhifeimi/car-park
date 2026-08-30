package press.mizhifei.parking.web.envelope;

/**
 * Unified response envelope — field-for-field the legacy {@code AjaxResult}:
 * {@code {"code":200,"msg":"操作成功","data":…}}.
 *
 * <p>Semantics: {@code code 200} = success, {@code 500} = business failure, {@code 401} =
 * unauthenticated (written by the security entry point). The {@code data} key is always
 * present (possibly {@code null}) — the deployed H5 reads {@code res.data.data} directly.
 *
 * <p>Business failures keep HTTP status 200 with the error in the envelope, exactly like the
 * legacy system; only the security layer (401/403) and the rate limiter (429) use real
 * HTTP statuses.
 */
public class R<T> {

    public static final int SUCCESS = 200;
    public static final int FAIL = 500;
    public static final int UNAUTHORIZED = 401;

    private int code;
    private String msg;
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** {code:200, msg:"操作成功"} */
    public static R<Void> ok() {
        return new R<>(SUCCESS, "操作成功", null);
    }

    /** {code:200, msg:"操作成功", data} */
    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, "操作成功", data);
    }

    /** {code:200, msg, data} */
    public static <T> R<T> ok(String msg, T data) {
        return new R<>(SUCCESS, msg, data);
    }

    /** {code:500, msg} */
    public static <T> R<T> fail(String msg) {
        return new R<>(FAIL, msg, null);
    }

    /** {code, msg} */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

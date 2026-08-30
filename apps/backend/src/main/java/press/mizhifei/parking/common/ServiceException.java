package press.mizhifei.parking.common;

/**
 * Business exception surfaced to the client as {@code {code,msg}} with HTTP 200
 * (legacy AjaxResult semantics — see {@code GlobalExceptionHandler}).
 */
public class ServiceException extends RuntimeException {

    private final int code;

    public ServiceException(String message) {
        this(500, message);
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

package press.mizhifei.parking.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.web.envelope.R;

/**
 * 全局异常处理 — legacy-compatible semantics: business and validation failures keep
 * <b>HTTP 200</b> with the error in the {@code {code,msg}} envelope (the deployed H5 and the
 * admin interceptor branch on {@code code}, not HTTP status). Only the security layer (401/403)
 * and the rate limiter (429) emit real HTTP error statuses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** @Valid request body failures → first field error message, e.g. "车牌号格式错误". */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return R.fail(message);
    }

    /** @Validated request-param failures. */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().isEmpty()
                ? "参数校验失败"
                : e.getConstraintViolations().iterator().next().getMessage();
        return R.fail(message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail("缺少参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail("参数格式错误：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleUnreadableBody(HttpMessageNotReadableException e) {
        return R.fail("请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail("不支持'" + e.getMethod() + "'请求");
    }

    @ExceptionHandler(ServiceException.class)
    public R<Void> handleService(ServiceException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    /** Catch-all: log the stack, never leak it. */
    @ExceptionHandler(Exception.class)
    public R<Void> handleUnknown(Exception e) {
        log.error("请求处理异常", e);
        return R.fail("系统未知错误，请反馈给管理员");
    }
}

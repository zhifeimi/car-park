package press.mizhifei.parking.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import press.mizhifei.parking.domain.BookingStatus;

/**
 * Parking lot + booking request DTOs.
 *
 * <p>Update DTOs are partial: {@code null} fields are left untouched (legacy MyBatis
 * {@code <if test="x != null">} SET semantics — the mobile cancel flow relies on this,
 * sending only what it wants to change plus {@code status:-1}).
 */
public final class ParkingDtos {

    private ParkingDtos() {
    }

    /**
     * Any country's number plate, so one build serves parks worldwide: letters of any
     * script (with combining marks) and digits, single inner spaces / hyphens / middle
     * dots, 2-16 characters. Mainland plates (粤A12345, 粤AD12345) are a subset; the
     * H5's {@code VITE_REGION=cn} mode still enforces the strict mainland format
     * client-side. The character class is also this field's XSS guard (ADR-005).
     * Mirrors {@code PLATE_REGEX} in packages/utils.
     */
    public static final String PLATE_REGEX =
            "^[\\p{L}\\p{N}](?:[\\p{L}\\p{M}\\p{N}]|[ \\-·](?=[\\p{L}\\p{N}])){1,15}$";

    /** Mainland mobile (13800138000, the deployed H5's format) or E.164 (+447700900123). */
    public static final String PHONE_REGEX = "^(?:1[3-9]\\d{9}|\\+[1-9]\\d{6,14})$";

    public record ParkCreateRequest(
            @NotBlank(message = "停车场名称不能为空") @Size(max = 100, message = "停车场名称不能超过100个字符") String name,
            @NotNull(message = "可预约车位总数不能为空") @Min(value = 0, message = "可预约车位总数不能为负数") Long lotNum,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate openDateBegin,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate openDateEnd,
            @JsonFormat(pattern = "HH:mm:ss") LocalTime openTime,
            @JsonFormat(pattern = "HH:mm:ss") LocalTime closeTime,
            @Size(max = 500) String remark) {
    }

    public record ParkUpdateRequest(
            @NotNull(message = "停车场id不能为空") Long id,
            @Size(max = 100, message = "停车场名称不能超过100个字符") String name,
            @Min(value = 0, message = "可预约车位总数不能为负数") Long lotNum,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate openDateBegin,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate openDateEnd,
            @JsonFormat(pattern = "HH:mm:ss") LocalTime openTime,
            @JsonFormat(pattern = "HH:mm:ss") LocalTime closeTime,
            @Size(max = 500) String remark) {
    }

    public record BookingCreateRequest(
            @NotBlank(message = "车牌号不能为空")
            @Pattern(regexp = PLATE_REGEX, message = "车牌号格式错误") String carPlate,
            @NotBlank(message = "停车场不能为空") String park,
            @NotNull(message = "预约日期不能为空") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = PHONE_REGEX, message = "手机号格式错误") String cellPhone,
            @Size(max = 500) String remark) {
    }

    public record BookingUpdateRequest(
            @NotNull(message = "预约记录id不能为空") Long id,
            @Pattern(regexp = PLATE_REGEX, message = "车牌号格式错误") String carPlate,
            String park,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @Pattern(regexp = PHONE_REGEX, message = "手机号格式错误") String cellPhone,
            @Size(max = 500) String remark,
            BookingStatus status) {
    }
}

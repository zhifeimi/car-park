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

    /** Mainland China plate: ordinary 7-char and new-energy 8-char forms. */
    public static final String CN_PLATE_REGEX =
            "^([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z]"
                    + "(([0-9]{5}[DF])|([DF][A-HJ-NP-Z0-9][0-9]{4})))"
                    + "|([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z]"
                    + "[A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳])$";

    public static final String CN_MOBILE_REGEX = "^1[3-9]\\d{9}$";

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
            @Pattern(regexp = CN_PLATE_REGEX, message = "车牌号格式错误") String carPlate,
            @NotBlank(message = "停车场不能为空") String park,
            @NotNull(message = "预约日期不能为空") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = CN_MOBILE_REGEX, message = "手机号格式错误") String cellPhone,
            @Size(max = 500) String remark) {
    }

    public record BookingUpdateRequest(
            @NotNull(message = "预约记录id不能为空") Long id,
            @Pattern(regexp = CN_PLATE_REGEX, message = "车牌号格式错误") String carPlate,
            String park,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @Pattern(regexp = CN_MOBILE_REGEX, message = "手机号格式错误") String cellPhone,
            @Size(max = 500) String remark,
            BookingStatus status) {
    }
}

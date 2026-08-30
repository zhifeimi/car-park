package press.mizhifei.parking.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import press.mizhifei.parking.common.ServiceException;

/**
 * 停车预约状态生命周期 (Booking Status Lifecycle).
 *
 * <p>0 = 预约成功 (Active), -1 = 已取消 (Cancelled).
 */
public enum BookingStatus {

    /** 预约成功 / 生效中 (Active) */
    ACTIVE(0, "预约成功"),

    /** 已取消 (Cancelled) */
    CANCELLED(-1, "已取消");

    private final int code;
    private final String label;

    BookingStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Whether the booking is currently active and occupying a capacity slot.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Whether the booking is in a cancellable state.
     */
    public boolean isCancellable() {
        return this == ACTIVE;
    }

    @JsonCreator
    public static BookingStatus fromCode(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return fromCode(number.intValue());
        }
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        try {
            return fromCode(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            for (BookingStatus status : values()) {
                if (status.name().equalsIgnoreCase(str) || status.label.equalsIgnoreCase(str)) {
                    return status;
                }
            }
            throw new ServiceException("未知预约状态: " + str);
        }
    }

    public static BookingStatus fromCode(int code) {
        for (BookingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new ServiceException("未知预约状态: " + code);
    }
}

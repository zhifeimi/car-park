package press.mizhifei.parking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Set;
import press.mizhifei.parking.common.ServiceException;

/**
 * 停车场信息 (Park Domain Entity) — table {@code cp_park}.
 * <p>
 * Encapsulates parking lot capacity configuration, operating schedule windows,
 * and invariant validation.
 *
 * <p>There is intentionally <b>no</b> {@code leftLotNum} field: remaining spaces are never
 * persisted, they are computed on the fly as {@code lot_num - count(bookings with status=0)}
 * (see {@code ParkRepository}). Do not persist availability.
 *
 * <p><b>Join-key hazard:</b> {@code cp_parking_booking.park} references this lot by its
 * <em>name</em>, not by id. Renaming a park orphans its bookings from availability counts.
 */
@Entity
@Table(name = "cp_park")
public class Park extends BaseEntity {

    /** ISO 3166-1 alpha-2 codes accepted for {@link #country}. */
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 停车场名称 — join key for bookings, treat as immutable (see class javadoc) */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 国家/地区 — ISO 3166-1 alpha-2, upper case ({@code CN} = mainland China; HK, MO and TW
     * have codes of their own). Required on create and on every later save. Drives the
     * H5's plate entry: the province keyboard for a {@code CN} lot, free text otherwise.
     * {@code null} only on rows that predate the column and were not backfilled
     * ({@code scripts/ops/park-country-migration.sql}); their next edit must set it.
     */
    @Column(name = "country", length = 2)
    private String country;

    /** 可预约车位总数 */
    @Column(name = "lot_num")
    private Long lotNum;

    /** 可预约起始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "open_date_begin")
    private LocalDate openDateBegin;

    /** 可预约结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "open_date_end")
    private LocalDate openDateEnd;

    /** 每天开放时间 */
    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "open_time")
    private LocalTime openTime;

    /** 每天关闭时间 */
    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "close_time")
    private LocalTime closeTime;

    public Park() {
    }

    /**
     * Domain factory method creating a new validated Park instance.
     */
    public static Park create(String name, String country, Long lotNum, LocalDate openDateBegin,
                              LocalDate openDateEnd, LocalTime openTime,
                              LocalTime closeTime, String remark) {
        Park park = new Park();
        park.name = (name == null || name.isBlank()) ? null : name.trim();
        park.country = normalizeCountry(country);
        park.lotNum = lotNum;
        park.openDateBegin = openDateBegin;
        park.openDateEnd = openDateEnd;
        park.openTime = openTime;
        park.closeTime = closeTime;
        park.setRemark(remark);
        park.validateInvariants();
        return park;
    }

    /**
     * Applies partial updates to the park and validates the resulting combined state.
     * Only non-null parameters are overwritten.
     */
    public void applyPartialUpdate(String name, String country, Long lotNum, LocalDate openDateBegin,
                                   LocalDate openDateEnd, LocalTime openTime,
                                   LocalTime closeTime, String remark) {
        if (name != null) {
            if (name.isBlank()) {
                throw new ServiceException("停车场名称不能为空");
            }
            this.name = name.trim();
        }
        if (country != null) {
            this.country = normalizeCountry(country);
        }
        if (lotNum != null) {
            this.lotNum = lotNum;
        }
        if (openDateBegin != null) {
            this.openDateBegin = openDateBegin;
        }
        if (openDateEnd != null) {
            this.openDateEnd = openDateEnd;
        }
        if (openTime != null) {
            this.openTime = openTime;
        }
        if (closeTime != null) {
            this.closeTime = closeTime;
        }
        if (remark != null) {
            setRemark(remark);
        }
        validateInvariants();
    }

    /**
     * Validates domain invariants on capacity and operational schedule.
     */
    public void validateInvariants() {
        if (name == null || name.isBlank()) {
            throw new ServiceException("停车场名称不能为空");
        }
        if (country == null) {
            throw new ServiceException("国家/地区不能为空");
        }
        if (!ISO_COUNTRIES.contains(country)) {
            throw new ServiceException("国家/地区代码无效");
        }
        if (lotNum != null && lotNum < 0) {
            throw new ServiceException("可预约车位总数不能为负数");
        }
        if (openDateBegin != null && openDateEnd != null && openDateBegin.isAfter(openDateEnd)) {
            throw new ServiceException("可预约起始日期不能晚于结束日期");
        }
        if (openTime != null && closeTime != null && !openTime.isBefore(closeTime)) {
            throw new ServiceException("每天开放时间必须早于关闭时间");
        }
    }

    /** Trimmed, upper case; blank → {@code null} (rejected by {@link #validateInvariants()}). */
    private static String normalizeCountry(String country) {
        return (country == null || country.isBlank()) ? null : country.trim().toUpperCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Long getLotNum() {
        return lotNum;
    }

    public void setLotNum(Long lotNum) {
        this.lotNum = lotNum;
    }

    public LocalDate getOpenDateBegin() {
        return openDateBegin;
    }

    public void setOpenDateBegin(LocalDate openDateBegin) {
        this.openDateBegin = openDateBegin;
    }

    public LocalDate getOpenDateEnd() {
        return openDateEnd;
    }

    public void setOpenDateEnd(LocalDate openDateEnd) {
        this.openDateEnd = openDateEnd;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }
}

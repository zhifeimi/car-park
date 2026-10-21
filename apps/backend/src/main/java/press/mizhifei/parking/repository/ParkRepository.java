package press.mizhifei.parking.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.web.dto.ParkListRow;

/**
 * {@code cp_park} access. The two native queries are verbatim ports of the legacy
 * {@code ParkMapper.xml} — this SQL is the frozen contract behind the deployed H5.
 */
@Repository
public interface ParkRepository extends JpaRepository<Park, Long> {

    /** Pessimistic write lock by park name — for concurrency control in booking creation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Park> findByName(String name);

    /** Returns true if a park with the given name exists. */
    boolean existsByName(String name);

    /** Returns true if another park with the given name exists (excluding the given id). */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Park list with computed remaining spaces for <b>today</b> — port of
     * {@code selectParkList}/{@code selectParkVo}:
     * {@code cp.lot_num - count(cpb.id)} via {@code left join cp_parking_booking cpb
     * on cpb.booking_date = curdate() and cpb.park = cp.name and cpb.status = 0
     * group by cp.id}.
     *
     * <p>{@code today} is passed in rather than read with {@code curdate()}: the database clock
     * may run in another zone (UTC on a cloud MySQL), and the park's day is the backend {@code TZ},
     * the same clock {@code BookingService} checks dates against.
     *
     * <p>Filters mirror the legacy {@code <if>} blocks exactly: {@code name} LIKE, all others
     * <em>equality</em>. Two deliberate fixes of legacy latent bugs: (1) the legacy XML placed
     * {@code <where>} after {@code group by} (invalid SQL, only "worked" because filters were
     * never exercised) — here WHERE precedes GROUP BY; (2) filters now qualify the {@code cp.}
     * table explicitly.
     *
     * <p>Blank-string parameters must be passed as {@code null} (the {@code :p is null} guards
     * implement the legacy {@code <if test="p != null and p != ''">} semantics).
     * Mobile calls this unpaged ({@code Pageable.unpaged()}) — the legacy implementation
     * returned all rows when {@code pageNum} was absent.
     */
    @Query(value = """
            select cp.id as id,
                   cp.name as name,
                   cp.lot_num as lotNum,
                   cp.lot_num - count(cpb.id) as leftLotNum,
                   cp.open_date_begin as openDateBegin,
                   cp.open_date_end as openDateEnd,
                   cp.open_time as openTime,
                   cp.close_time as closeTime,
                   cp.remark as remark,
                   cp.create_by as createBy,
                   cp.create_time as createTime,
                   cp.update_by as updateBy,
                   cp.update_time as updateTime
            from cp_park cp
                     left join cp_parking_booking cpb
                               on cpb.booking_date = :today
                                   and cpb.park = cp.name
                                   and cpb.status = 0
            where (:name is null or cp.name like concat('%', :name, '%'))
              and (:lotNum is null or cp.lot_num = :lotNum)
              and (:openDateBegin is null or cp.open_date_begin = :openDateBegin)
              and (:openDateEnd is null or cp.open_date_end = :openDateEnd)
              and (:openTime is null or cp.open_time = :openTime)
              and (:closeTime is null or cp.close_time = :closeTime)
            group by cp.id""",
            countQuery = """
            select count(*)
            from cp_park cp
            where (:name is null or cp.name like concat('%', :name, '%'))
              and (:lotNum is null or cp.lot_num = :lotNum)
              and (:openDateBegin is null or cp.open_date_begin = :openDateBegin)
              and (:openDateEnd is null or cp.open_date_end = :openDateEnd)
              and (:openTime is null or cp.open_time = :openTime)
              and (:closeTime is null or cp.close_time = :closeTime)""",
            nativeQuery = true)
    Page<ParkListRow> search(@Param("name") String name,
                             @Param("lotNum") Long lotNum,
                             @Param("openDateBegin") String openDateBegin,
                             @Param("openDateEnd") String openDateEnd,
                             @Param("openTime") String openTime,
                             @Param("closeTime") String closeTime,
                             @Param("today") LocalDate today,
                             Pageable pageable);

    /**
     * Remaining spaces for a park on a date — port of {@code selectParkLeftLotNum} (params stay
     * {@code String}; MySQL casts them): unknown park yields {@code null - 0 = null}.
     */
    @Query(value = """
            select (select cp.lot_num
                    from cp_park cp
                    where cp.name = :park)
                       -
                   (select count(cpb.id)
                    from cp_parking_booking cpb
                    where cpb.park = :park
                      and cpb.booking_date = :date
                      and cpb.status = 0)""",
            nativeQuery = true)
    Integer leftLotNum(@Param("park") String park, @Param("date") String date);
}

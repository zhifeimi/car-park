package press.mizhifei.parking.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.service.BookingCommand;
import press.mizhifei.parking.service.BookingConfirmation;
import press.mizhifei.parking.service.BookingCriteria;
import press.mizhifei.parking.service.BookingService;
import press.mizhifei.parking.service.ExportService;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingCreateRequest;
import press.mizhifei.parking.web.dto.ParkingDtos.BookingUpdateRequest;
import press.mizhifei.parking.web.dto.StatisticsInfo;
import press.mizhifei.parking.web.envelope.R;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * 停车预约. Public surface (citizen H5): last, create, update (cancel).
 * list, statisticInfo, delete and export require an admin token — an anonymous unfiltered
 * list was a full dump of every plate and phone. The legacy per-id read ({@code GET /{id}})
 * was deleted with the anonymous-surface tightening (issue 02) — the success page loads by
 * phone + plate via {@code /last}.
 */
@RestController
@RequestMapping("/parking/booking")
public class BookingController {

    private final BookingService bookingService;
    private final ExportService exportService;

    public BookingController(BookingService bookingService,
                             ExportService exportService) {
        this.bookingService = bookingService;
        this.exportService = exportService;
    }

    /** Admin. Equality filters + legacy sort order; paged when pageNum/pageSize present. */
    @GetMapping("/list")
    public TableData list(BookingCriteria criteria) {
        return bookingService.list(criteria);
    }

    /** Public, rate-limited. Latest booking for a phone + plate (by create_time) — the success page reload. */
    @GetMapping("/last")
    public R<ParkingBooking> last(@RequestParam String cellPhone, @RequestParam String carPlate) {
        return R.ok(bookingService.lastBooking(cellPhone, carPlate));
    }

    /** Public, rate-limited. Server validates availability/invariants and sets {@code status=0}. */
    @PostMapping
    public R<Long> add(@Valid @RequestBody BookingCreateRequest request) {
        BookingConfirmation confirmation = bookingService.intake(new BookingCommand(
                request.carPlate(),
                request.park(),
                request.bookingDate(),
                request.cellPhone(),
                request.remark()
        ));
        return R.ok(confirmation.id());
    }

    /** Public, rate-limited. Cancel only: {@code {id, cellPhone, status:-1}}, phone must match the booking. */
    @PutMapping
    public R<Void> edit(@Valid @RequestBody BookingUpdateRequest request) {
        bookingService.update(request);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable String ids) {
        bookingService.deleteByIds(ids);
        return R.ok();
    }

    /**
     * Admin dashboard aggregate. {@code date} stays a String: the dashboard sends
     * {@code yyyy-MM-dd HH:mm:ss} and the SQL parses it with {@code date_format}.
     */
    @GetMapping("/statisticInfo")
    public R<StatisticsInfo> statisticInfo(@RequestParam String date) {
        return R.ok(bookingService.statistics(date));
    }

    @PostMapping("/export")
    public void export(BookingCriteria criteria, HttpServletResponse response) {
        exportService.exportBookings(response, bookingService.listAll(criteria));
    }
}

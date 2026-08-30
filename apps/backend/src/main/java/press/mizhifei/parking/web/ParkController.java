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
import press.mizhifei.parking.domain.Park;
import press.mizhifei.parking.service.ExportService;
import press.mizhifei.parking.service.ParkCriteria;
import press.mizhifei.parking.service.ParkService;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkCreateRequest;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkUpdateRequest;
import press.mizhifei.parking.web.envelope.R;
import press.mizhifei.parking.web.envelope.TableData;

/**
 * 停车场管理. {@code GET /list} and {@code GET /leftLotNum} are anonymous (citizen H5);
 * everything else requires an admin token — see {@code SecurityConfig} for the exact
 * public surface (the legacy wildcard permits were deliberately tightened).
 */
@RestController
@RequestMapping("/parking/park")
public class ParkController {

    private final ParkService parkService;
    private final ExportService exportService;

    public ParkController(ParkService parkService, ExportService exportService) {
        this.parkService = parkService;
        this.exportService = exportService;
    }

    /** Public. Filters: name (LIKE), lotNum/openDateBegin/openDateEnd/openTime/closeTime (equality). */
    @GetMapping("/list")
    public TableData list(ParkCriteria criteria) {
        return parkService.list(criteria);
    }

    /** Public. Remaining spaces for one park on one date; {@code data:null} for unknown parks. */
    @GetMapping("/leftLotNum")
    public R<Integer> leftLotNum(@RequestParam String park, @RequestParam String date) {
        return R.ok(parkService.leftLotNum(park, date));
    }

    @GetMapping("/{id}")
    public R<Park> getInfo(@PathVariable Long id) {
        return R.ok(parkService.get(id));
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody ParkCreateRequest request) {
        parkService.create(request);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@Valid @RequestBody ParkUpdateRequest request) {
        parkService.update(request);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable String ids) {
        parkService.deleteByIds(ids);
        return R.ok();
    }

    @PostMapping("/export")
    public void export(ParkCriteria criteria, HttpServletResponse response) {
        exportService.exportParks(response, parkService.listAll(criteria));
    }
}

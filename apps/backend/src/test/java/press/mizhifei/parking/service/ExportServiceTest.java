package press.mizhifei.parking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import press.mizhifei.parking.domain.BookingStatus;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.web.dto.ParkListRow;

/**
 * Unit tests for {@link ExportService} crossing the {@link java.io.OutputStream} seam.
 */
class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    @Test
    void exportParks_writesCorrectSheetAndFormattedCells() throws IOException {
        ParkListRow row1 = new TestParkListRow(
                1L,
                "黄麻停车场",
                100L,
                42L,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                LocalTime.of(8, 0, 0),
                LocalTime.of(22, 0, 0)
        );

        ParkListRow row2 = new TestParkListRow(
                2L,
                "岭头停车场",
                50L,
                0L,
                null,
                null,
                null,
                null
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportParks(out, List.of(row1, row2));

        byte[] bytes = out.toByteArray();
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = workbook.getSheet("停车场信息数据");
            assertThat(sheet).isNotNull();

            // 1. Verify Header Row
            Row header = sheet.getRow(0);
            assertThat(header).isNotNull();
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("停车场名称");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("可预约车位总数");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("剩余车位");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("可预约起始日期");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("可预约结束日期");
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo("每天开放时间");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("每天关闭时间");

            // 2. Verify Data Row 1
            Row r1 = sheet.getRow(1);
            assertThat(r1.getCell(0).getStringCellValue()).isEqualTo("黄麻停车场");
            assertThat(r1.getCell(1).getNumericCellValue()).isEqualTo(100.0);
            assertThat(r1.getCell(2).getNumericCellValue()).isEqualTo(42.0);
            assertThat(r1.getCell(3).getStringCellValue()).isEqualTo("2026-08-01");
            assertThat(r1.getCell(4).getStringCellValue()).isEqualTo("2026-12-31");
            assertThat(r1.getCell(5).getStringCellValue()).isEqualTo("08:00:00");
            assertThat(r1.getCell(6).getStringCellValue()).isEqualTo("22:00:00");

            // 3. Verify Data Row 2 (Nulls handled as empty strings)
            Row r2 = sheet.getRow(2);
            assertThat(r2.getCell(0).getStringCellValue()).isEqualTo("岭头停车场");
            assertThat(r2.getCell(1).getNumericCellValue()).isEqualTo(50.0);
            assertThat(r2.getCell(2).getNumericCellValue()).isEqualTo(0.0);
            assertThat(r2.getCell(3).getStringCellValue()).isEmpty();
            assertThat(r2.getCell(4).getStringCellValue()).isEmpty();
            assertThat(r2.getCell(5).getStringCellValue()).isEmpty();
            assertThat(r2.getCell(6).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void exportBookings_writesCorrectSheetAndFormattedCells() throws IOException {
        ParkingBooking b1 = new ParkingBooking();
        b1.setId(1L);
        b1.setCarPlate("粤A12345");
        b1.setPark("黄麻停车场");
        b1.setBookingDate(LocalDate.of(2026, 8, 16));
        b1.setCellPhone("13800138000");
        b1.setStatus(BookingStatus.ACTIVE);

        ParkingBooking b2 = new ParkingBooking();
        b2.setId(2L);
        b2.setCarPlate("粤B67890");
        b2.setPark("岭头停车场");
        b2.setBookingDate(null);
        b2.setCellPhone("13900139000");
        b2.setStatus(BookingStatus.CANCELLED);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportBookings(out, List.of(b1, b2));

        byte[] bytes = out.toByteArray();
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = workbook.getSheet("停车预约记录数据");
            assertThat(sheet).isNotNull();

            // 1. Verify Header Row
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("车牌号");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("停车场");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("预约日期");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("手机号");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("状态，0成功，-1取消");

            // 2. Verify Data Rows
            Row r1 = sheet.getRow(1);
            assertThat(r1.getCell(0).getStringCellValue()).isEqualTo("粤A12345");
            assertThat(r1.getCell(1).getStringCellValue()).isEqualTo("黄麻停车场");
            assertThat(r1.getCell(2).getStringCellValue()).isEqualTo("2026-08-16");
            assertThat(r1.getCell(3).getStringCellValue()).isEqualTo("13800138000");
            assertThat(r1.getCell(4).getNumericCellValue()).isEqualTo(0.0);

            Row r2 = sheet.getRow(2);
            assertThat(r2.getCell(0).getStringCellValue()).isEqualTo("粤B67890");
            assertThat(r2.getCell(1).getStringCellValue()).isEqualTo("岭头停车场");
            assertThat(r2.getCell(2).getStringCellValue()).isEmpty();
            assertThat(r2.getCell(3).getStringCellValue()).isEqualTo("13900139000");
            assertThat(r2.getCell(4).getNumericCellValue()).isEqualTo(-1.0);
        }
    }

    @Test
    void export_emptyList_createsValidSheetWithHeadersOnly() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportBookings(out, List.of());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheet("停车预约记录数据");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("车牌号");
        }
    }

    /** Simple test implementation of ParkListRow projection interface */
    private record TestParkListRow(
            Long id,
            String name,
            Long lotNum,
            Long leftLotNum,
            LocalDate openDateBegin,
            LocalDate openDateEnd,
            LocalTime openTime,
            LocalTime closeTime
    ) implements ParkListRow {
        @Override public Long getId() { return id; }
        @Override public String getName() { return name; }
        @Override public Long getLotNum() { return lotNum; }
        @Override public Long getLeftLotNum() { return leftLotNum; }
        @Override public LocalDate getOpenDateBegin() { return openDateBegin; }
        @Override public LocalDate getOpenDateEnd() { return openDateEnd; }
        @Override public LocalTime getOpenTime() { return openTime; }
        @Override public LocalTime getCloseTime() { return closeTime; }
        @Override public String getRemark() { return null; }
        @Override public String getCreateBy() { return null; }
        @Override public java.time.LocalDateTime getCreateTime() { return null; }
        @Override public String getUpdateBy() { return null; }
        @Override public java.time.LocalDateTime getUpdateTime() { return null; }
    }
}

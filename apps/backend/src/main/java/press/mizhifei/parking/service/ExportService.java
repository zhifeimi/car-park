package press.mizhifei.parking.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import press.mizhifei.parking.common.ServiceException;
import press.mizhifei.parking.domain.ParkingBooking;
import press.mizhifei.parking.web.dto.ParkListRow;

/**
 * 报表流式导出 (Tabular Report Export Deep Module).
 * <p>
 * Consolidates POI spreadsheet generation, typography/styling, column formatting,
 * HTTP header serialization, and direct stream piping behind a cohesive domain interface.
 * Operates with zero heap byte-array buffering by streaming directly to output streams.
 */
@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int DEFAULT_COLUMN_WIDTH = 20 * 256;

    /** Schema definition for 停车场信息数据 */
    private static final TableSchema<ParkListRow> PARK_SCHEMA = new TableSchema<>(List.of(
            new Column<>("停车场名称", ParkListRow::getName),
            new Column<>("可预约车位总数", ParkListRow::getLotNum),
            new Column<>("剩余车位", ParkListRow::getLeftLotNum),
            new Column<>("可预约起始日期", r -> formatDate(r.getOpenDateBegin())),
            new Column<>("可预约结束日期", r -> formatDate(r.getOpenDateEnd())),
            new Column<>("每天开放时间", r -> formatTime(r.getOpenTime())),
            new Column<>("每天关闭时间", r -> formatTime(r.getCloseTime()))
    ));

    /** Schema definition for 停车预约记录数据 */
    private static final TableSchema<ParkingBooking> BOOKING_SCHEMA = new TableSchema<>(List.of(
            new Column<>("车牌号", ParkingBooking::getCarPlate),
            new Column<>("停车场", ParkingBooking::getPark),
            new Column<>("预约日期", b -> formatDate(b.getBookingDate())),
            new Column<>("手机号", ParkingBooking::getCellPhone),
            new Column<>("状态，0成功，-1取消", b -> b.getStatus() == null ? null : (long) b.getStatus().getCode())
    ));

    /**
     * Stream 停车场信息数据 directly to an HTTP response.
     */
    public void exportParks(HttpServletResponse response, List<ParkListRow> rows) {
        writeToResponse(response, "停车场数据", "停车场信息数据", PARK_SCHEMA, rows);
    }

    /**
     * Stream 停车场信息数据 directly to a target OutputStream (for testing and non-HTTP use).
     */
    public void exportParks(OutputStream out, List<ParkListRow> rows) {
        writeToStream(out, "停车场信息数据", PARK_SCHEMA, rows);
    }

    /**
     * Stream 停车预约记录数据 directly to an HTTP response.
     */
    public void exportBookings(HttpServletResponse response, List<ParkingBooking> rows) {
        writeToResponse(response, "预约记录数据", "停车预约记录数据", BOOKING_SCHEMA, rows);
    }

    /**
     * Stream 停车预约记录数据 directly to a target OutputStream (for testing and non-HTTP use).
     */
    public void exportBookings(OutputStream out, List<ParkingBooking> rows) {
        writeToStream(out, "停车预约记录数据", BOOKING_SCHEMA, rows);
    }

    /**
     * Writes tabular data to an HttpServletResponse with standard .xlsx MIME headers.
     */
    private <T> void writeToResponse(HttpServletResponse response, String fileName,
                                     String sheetName, TableSchema<T> schema, List<T> rows) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + encodedFileName + ".xlsx");

        try {
            writeToStream(response.getOutputStream(), sheetName, schema, rows);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new ServiceException("导出" + fileName + "失败: " + e.getMessage());
        }
    }

    /**
     * Core streaming engine: renders SXSSFWorkbook and writes directly to target OutputStream.
     */
    private <T> void writeToStream(OutputStream out, String sheetName,
                                   TableSchema<T> schema, List<T> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 1. Render Header Row
            Row headerRow = sheet.createRow(0);
            List<Column<T>> columns = schema.columns();
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).header());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, columns.get(i).width());
            }

            // 2. Render Data Rows
            int rowIdx = 1;
            for (T item : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                    Object val = columns.get(colIdx).extractor().apply(item);
                    setCellValue(row.createCell(colIdx), val);
                }
            }

            // 3. Stream to output and dispose temporary SXSSF disk files
            workbook.write(out);
            workbook.dispose();
        } catch (IOException e) {
            throw new ServiceException("生成报表数据失败: " + e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    private static String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }

    /**
     * Column schema definition with header title, width, and typed value extractor.
     */
    public record Column<T>(String header, int width, Function<T, Object> extractor) {
        public Column(String header, Function<T, Object> extractor) {
            this(header, DEFAULT_COLUMN_WIDTH, extractor);
        }
    }

    /**
     * Table schema containing ordered columns.
     */
    public record TableSchema<T>(List<Column<T>> columns) {}
}

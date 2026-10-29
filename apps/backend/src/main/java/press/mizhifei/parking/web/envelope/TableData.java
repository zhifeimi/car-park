package press.mizhifei.parking.web.envelope;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

/**
 * Paginated list envelope — field-for-field the legacy {@code TableDataInfo}:
 * {@code {"total":N,"rows":[…],"code":200,"msg":"查询成功"}}.
 *
 * <p>One additive, optional field: {@code today} ({@code yyyy-MM-dd}, the park's date by its
 * clock), set only by {@code GET /parking/park/list} so the H5 never takes "today" from the
 * phone's clock. Omitted when null, so every other list keeps the legacy shape exactly.
 */
public class TableData {

    private long total;
    private List<?> rows;
    private int code = 200;
    private String msg = "查询成功";
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate today;

    public TableData() {
    }

    public TableData(long total, List<?> rows) {
        this.total = total;
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<?> getRows() {
        return rows;
    }

    public void setRows(List<?> rows) {
        this.rows = rows;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public LocalDate getToday() {
        return today;
    }

    public TableData withToday(LocalDate today) {
        this.today = today;
        return this;
    }
}

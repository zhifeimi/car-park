package press.mizhifei.parking.web.envelope;

import java.util.List;

/**
 * Paginated list envelope — field-for-field the legacy {@code TableDataInfo}:
 * {@code {"total":N,"rows":[…],"code":200,"msg":"查询成功"}}.
 */
public class TableData {

    private long total;
    private List<?> rows;
    private int code = 200;
    private String msg = "查询成功";

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
}

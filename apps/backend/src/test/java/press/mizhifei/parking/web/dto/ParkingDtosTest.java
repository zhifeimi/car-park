package press.mizhifei.parking.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Pins the worldwide plate / phone formats. The same sample table lives in
 * packages/utils/src/validation.test.ts; keep both in sync.
 */
class ParkingDtosTest {

    private static final Pattern PLATE = Pattern.compile(ParkingDtos.PLATE_REGEX);
    private static final Pattern PHONE = Pattern.compile(ParkingDtos.PHONE_REGEX);

    @Test
    void acceptsPlatesFromAnyCountry() {
        List<String> plates = List.of(
                "粤A12345", "粤AD12345", "粤Z1234港", // mainland, new-energy, cross-border
                "AB12 CDE", "M-AB 1234", "7ABC123", "ABC-1234", // UK, DE, US, BR
                "品川 300 さ 12-34", "12가 3456", "กข 1234", "А123ВС 77"); // JP, KR, TH, RU
        assertThat(plates).allMatch(p -> PLATE.matcher(p).matches());
    }

    @Test
    void rejectsMarkupJunkAndBadSpacing() {
        List<String> plates = List.of(
                "<script>", "AB12'", "A&B1", "A", "", " AB12", "AB12 ", "AB  12", "-AB12", "AB--12",
                "ABCDEFGHIJKLMNOPQ"); // 17 chars
        assertThat(plates).noneMatch(p -> PLATE.matcher(p).matches());
    }

    @Test
    void acceptsMainlandMobileAndE164() {
        assertThat(List.of("13800138000", "+8613800138000", "+447700900123", "+12025550143"))
                .allMatch(p -> PHONE.matcher(p).matches());
    }

    @Test
    void rejectsUnnormalizedOrMalformedPhones() {
        assertThat(List.of("12800138000", "1380013800", "+0123456789", "+44 7700 900123", "447700900123",
                "+1234567890123456"))
                .noneMatch(p -> PHONE.matcher(p).matches());
    }
}

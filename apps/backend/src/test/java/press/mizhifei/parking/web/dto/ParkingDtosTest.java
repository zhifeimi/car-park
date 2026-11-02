package press.mizhifei.parking.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkCreateRequest;
import press.mizhifei.parking.web.dto.ParkingDtos.ParkUpdateRequest;

/**
 * Pins the worldwide plate / phone formats. The same sample table lives in
 * packages/utils/src/validation.test.ts; keep both in sync.
 */
class ParkingDtosTest {

    private static final Pattern PLATE = Pattern.compile(ParkingDtos.PLATE_REGEX);
    private static final Pattern PHONE = Pattern.compile(ParkingDtos.PHONE_REGEX);
    private static final Pattern COUNTRY = Pattern.compile(ParkingDtos.COUNTRY_REGEX);

    @Test
    void countryIsATwoLetterCode() {
        assertThat(List.of("CN", "cn", "GB", "Us")).allMatch(c -> COUNTRY.matcher(c).matches());
        assertThat(List.of("", "C", "CHN", "C1", "中国", "<b>", " CN"))
                .noneMatch(c -> COUNTRY.matcher(c).matches());
    }

    @Test
    void countryBeanValidation_requiredOnCreateButOptionalOnPartialUpdate() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(createWithCountry(null)))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("country"));
            assertThat(validator.validate(createWithCountry("")))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("country"));
            assertThat(validator.validate(updateWithCountry(null))).isEmpty();
            assertThat(validator.validate(updateWithCountry("")))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("country"));
            assertThat(validator.validate(updateWithCountry("CHN")))
                    .anyMatch(v -> v.getPropertyPath().toString().equals("country"));
        }
    }

    private static ParkCreateRequest createWithCountry(String country) {
        return new ParkCreateRequest("Test park", country, 1L, null, null, null, null, null);
    }

    private static ParkUpdateRequest updateWithCountry(String country) {
        return new ParkUpdateRequest(1L, null, country, null, null, null, null, null, null);
    }

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

package press.mizhifei.parking.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The one canonical spelling of a number plate — what is stored, compared for vehicle
 * exclusivity and used as the {@code /last} lookup key. The worldwide plate format lets a
 * visitor type separators ({@code AB-123}, {@code AB 123}, {@code AB·123}); they are
 * presentation only, so all of them collapse to {@code AB123}. Mainland plates carry no
 * separators and are unchanged apart from upper-casing. Mirrors {@code normalizePlate} in
 * packages/utils.
 */
public final class Plates {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-·]+");

    private Plates() {
    }

    /** NFC, separators removed, upper case; {@code null} stays {@code null}. */
    public static String canonical(String plate) {
        if (plate == null) {
            return null;
        }
        String nfc = Normalizer.normalize(plate, Normalizer.Form.NFC);
        return SEPARATORS.matcher(nfc).replaceAll("").toUpperCase(Locale.ROOT);
    }
}

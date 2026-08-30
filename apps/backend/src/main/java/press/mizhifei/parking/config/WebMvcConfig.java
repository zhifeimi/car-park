package press.mizhifei.parking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import press.mizhifei.parking.domain.BookingStatus;

/**
 * Spring MVC configuration — registers parameter converters for web endpoints.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, BookingStatus>() {
            @Override
            public BookingStatus convert(String source) {
                return BookingStatus.fromCode(source);
            }
        });
    }
}

package press.mizhifei.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CarPark 停车场预约管理系统 — application entry point.
 *
 * <p>Greenfield vanilla Spring Boot replacement for the legacy parking-booking backend.
 * JPA auditing is enabled in {@link press.mizhifei.parking.config.JpaAuditingConfig}.
 */
@SpringBootApplication
public class CarParkApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarParkApplication.class, args);
    }
}

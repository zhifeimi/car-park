package press.mizhifei.parking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * springdoc/Swagger UI — dev profile only ({@code springdoc.*.enabled} is false in the base
 * config, true in {@code application-dev.yml}). Reachable at {@code /swagger-ui.html} with
 * the "bearer" scheme pre-registered: unlike the legacy anonymous Swagger, you must paste a
 * token from {@code POST /auth/login} via the Authorize button to call protected endpoints.
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI carParkOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CarPark API")
                        .description("停车场预约管理系统接口文档")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}

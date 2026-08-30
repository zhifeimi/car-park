# OpenAPI & Swagger Specification

## Development Swagger UI

In the `dev` Spring profile, SpringDoc OpenAPI is automatically enabled at:
`http://localhost:8080/swagger-ui.html`

Raw OpenAPI v3 JSON schema is available at:
`http://localhost:8080/v3/api-docs`

## Production Security Policy

In production (`dev` profile inactive), OpenAPI documentation and Swagger UI routes are disabled to prevent external reconnaissance.

## Client Generation

The TypeScript API client in `packages/api-client` is designed to match the OpenAPI specification and Spring MVC controller contracts byte-for-byte.

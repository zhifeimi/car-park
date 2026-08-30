# ADR 003: Stateless JWT Authentication Without Redis

## Status
Accepted

## Context
The legacy application used Redis for session storage and token caching, adding operational overhead and infrastructure complexity for a lightweight community parking service.

## Decision
Adopt standard stateless HMAC-SHA256 JWT tokens with a 720-minute (12-hour) expiration.
- Secrets are passed exclusively via environment variables (`JWT_SECRET`).
- Token validation is purely computational and in-memory.
- Rate limiting is implemented with in-memory Bucket4j token buckets keyed by client IP.

## Consequences
- Redis dependency is completely removed from production and dev environments.
- Server is simpler to deploy, package, and containerize.

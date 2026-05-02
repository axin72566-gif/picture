# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Picture management platform backend (picture-backend) — Spring Boot 2.7.6 + Java 8 + Maven. Provides REST APIs for a picture-sharing application with multi-tenant "spaces," collaborative editing via WebSocket, and Tencent Cloud COS object storage.

## Build & Run

```bash
# Run the application (development)
mvn spring-boot:run

# Package
mvn clean package -DskipTests

# Run tests
mvn test

# Run a specific test class
mvn test -Dtest=PictureBackendApplicationTests
```

The application starts on **port 8123** with context path `/api`. API docs are available at `/api/doc.html` (Knife4j/Swagger).

## Configuration

- **`application.yml`** — main config: datasource, Redis, MyBatis-Plus, session (Redis-backed), file upload max 10MB.
- **`application-local.yml`** — secrets (COS credentials). Active profile is `local`. This file is git-ignored.
- **`biz/spaceUserAuthConfig.json`** — role-permission mappings for spaces (viewer/editor/admin), loaded at startup.

## Architecture

### Layered Structure

Standard Spring Boot layered architecture: **Controller** → **Service** → **Mapper** (MyBatis-Plus). There is no separate repository layer — services extend `ServiceImpl<Mapper, Entity>` and call mappers directly.

Key packages:
- `controller/` — REST endpoints, thin (validation, auth check, delegate to service)
- `service/impl/` — business logic
- `mapper/` — MyBatis-Plus BaseMapper interfaces, with XML in `resources/mapper/`
- `model/entity/` — database entities; `model/vo/` — view objects (returned to frontend); `model/dto/` — request DTOs
- `manager/` — reusable "manager" components (COS, auth, upload templates, WebSocket, scheduled cleanup)
- `config/` — Spring configuration (CORS, MyBatis-Plus pagination plugin, Disruptor, thread pool, WebSocket)
- `common/` — `BaseResponse<T>` wrapper, `ResultUtils`, generic `DeleteRequest`/`PageRequest`
- `exception/` — `BusinessException`, `ErrorCode` enum, global `@RestControllerAdvice` handler

### Authentication & Authorization (Two-Tier)

1. **Platform-level roles** (user/admin): `@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)` annotation + `RoleCheckAspect` AOP. Used for system-wide admin operations.
2. **Space-level permissions** (viewer/editor/admin): `@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)` annotation, backed by Sa-Token with a multi-account system via `StpKit`. Space permissions are defined in `spaceUserAuthConfig.json` and resolved by `SpaceUserAuthManager.getPermissionList(Space, User)`.

The auth flow uses Cookie Session (Spring Session backed by Redis, 30-day max age). User identity is retrieved via `userService.getLoginUser(request)` which calls Sa-Token's `StpUtil.getLoginId()`.

### Space Types

Three space types control permission resolution (see `SpaceUserAuthManager.getPermissionList`):
- **Public gallery** (space == null): admins have full access; regular users have viewer access
- **Private space** (`spaceType=0`): only the creator has admin permissions
- **Team space** (`spaceType=1`): permissions depend on the user's role in the `space_user` table

### Picture Upload (Template Method Pattern)

`PictureUploadTemplate` defines the upload pipeline: validate → generate COS path → write temp file → upload to COS → extract image metadata → build result → clean up temp file. Two subclasses: `FilePictureUpload` (multipart file) and `UrlPictureUpload` (download from URL).

### Multi-Level Caching

`PictureServiceImpl` uses **Caffeine** (local) + **Redis** (distributed) for picture list queries. Cache key: `picture:{queryCondition MD5}`. Additionally, Redis stores:
- `picture:view:{pictureId}` — view count
- `picture:like:{pictureId}` — like count (likes also persist async to `picture_like` table)

### Collaborative Editing (WebSocket + Disruptor)

WebSocket endpoint for real-time picture collaborative editing:
- **`WsHandshakeInterceptor`** — validates user identity and picture permissions on handshake
- **`PictureEditHandler`** — manages editing sessions per picture (`ConcurrentHashMap`), enforces single-editor lock, broadcasts to all viewers
- **Disruptor ring buffer** — `PictureEditEventProducer` publishes events, `PictureEditEventWorkHandler` consumes them asynchronously, decoupling WebSocket I/O from business logic

### Scheduled Cleanup

`@Scheduled` tasks in `manager/pictureClear/`:
- **Public**: cleans rejected or long-pending review pictures
- **Private**: evicts oldest pictures when over quota; removes files not accessed for 180 days
- **Team**: same as private, plus sends system notifications

### Database

MySQL 8.x, accessed via MyBatis-Plus 3.5.9. Key conventions:
- **Logic delete**: `isDelete` field (0=normal, 1=deleted), configured globally in `application.yml`
- **Snowflake IDs**: `IdType.ASSIGN_ID` for user, picture, comment entities
- **Auto-increment IDs**: space, space_user, picture_like, sys_notice
- **MyBatis-Plus pagination plugin**: `PaginationInnerInterceptor(DbType.MYSQL)`
- **map-underscore-to-camel-case: false** — column names in XML must match entity field names exactly

Schema: `SQL/create_tables.sql`. Run against the `picture` database.

### Key Dependencies

| Dependency | Purpose |
|---|---|
| Sa-Token 1.39.0 | Auth framework (multi-account, permission checking) |
| MyBatis-Plus 3.5.9 | ORM + pagination |
| Tencent COS SDK 5.6.227 | Object storage for pictures |
| Disruptor 3.4.2 | Lock-free ring buffer for WebSocket messages |
| Caffeine 2.9.3 | Local cache |
| Knife4j 4.4.0 | API documentation UI |
| Hutool 5.8.43 | General-purpose utilities |
| Jsoup 1.15.3 | HTML parsing (batch picture import from Bing) |

## Important Implementation Details

- **Long serialization**: All `Long` fields are serialized as strings in JSON responses to prevent JavaScript precision loss (configured via Jackson `ToStringSerializer` in `JsonConfig` and `PictureEditHandler`).
- **Sharding module** (`manager/sharding/`) is **commented out / disabled** — it was a planned feature for sharding pictures by `spaceId` using ShardingSphere.
- **`backgroundExecutor`** thread pool (CPU cores × 2 max threads, 1000 queue) is used for async tasks.
- **CORS** allows all origins with credentials (development setup).
- `spring-boot-maven-plugin` has `<skip>true</skip>` — the plugin config is there but repackaging is handled via the `repackage` execution goal.

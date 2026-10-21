# Database Schema (MySQL) — Schema Initialization & Evolution

This project uses a hybrid schema approach:
- `backend/db/schema.sql` provides an initial "baseline" schema that is loaded by MySQL when the container starts for the first time.
- Hibernate (`spring.jpa.hibernate.ddl-auto=update`) continuously evolves the database to match the JPA entity mappings.

The intent behind this setup is pragmatic: the app can boot with a known starting point (via `schema.sql`), but it can also add missing columns when the entity definitions change without needing a separate migration tool.

## How the database schema gets created/updated

### 1. Fresh local stack (Docker init)

When you run `docker compose up ...`, the MySQL image executes `schema.sql` during initialization because `docker-compose.yml` mounts it into:
- `/docker-entrypoint-initdb.d/schema.sql`

This only runs when the MySQL data volume is empty (for example, after you run `docker compose down -v`).

Why this matters: if your volume already contains a schema, MySQL will not re-run `schema.sql`, and you will instead rely on Hibernate to make subsequent changes.

### 2. Apply `schema.sql` to an existing database

The repo includes a helper target:
- `make schema`

It runs `backend/db/schema.sql` against the currently running MySQL instance. This is useful when you change the baseline schema but do not want to recreate the entire Docker volume.

### 3. Hibernate auto-evolution (`ddl-auto=update`)

Backend config:
- `backend/src/main/resources/application.properties`
  - `spring.jpa.hibernate.ddl-auto=update`

Hibernate uses the JPA entity definitions under `backend/src/main/java/com/example/demo/entity/` to add/adjust columns so the runtime model can persist and read data.

Why this matters: the baseline `schema.sql` and the entity mappings are not perfectly aligned field-for-field (for example, `schema.sql` does not include every "live position" column that exists in `Aircraft.java`).

`ddl-auto=update` is what makes the app work after startup by adding the missing columns.

## Validation checklist (after bringing up the stack)

After starting Docker and the backend, confirm the runtime schema exists as expected:

1. Verify base tables exist: `celebrities`, `aircraft`, `airports`, `flights`, `users`.
2. Verify entity-mapped extra columns exist:
   - for `aircraft`, confirm `airborne`, `last_lat`, `last_seen` columns exist
   - for `flights`, confirm `live_info_json` and other entity-mapped fields exist
3. Use phpMyAdmin to spot-check:
   - uniqueness: `aircraft.tail_number`
   - nullability: `aircraft.owner_id`

## References

- Baseline SQL: [`backend/db/schema.sql`](backend/db/schema.sql)
- JPA entity mappings: `backend/src/main/java/com/example/demo/entity/*`
- Hibernate schema evolution config: `backend/src/main/resources/application.properties`


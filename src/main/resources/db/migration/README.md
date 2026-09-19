# Flyway database migrations

All database schema changes must be committed as versioned Flyway SQL migrations.

## Naming convention

`V<version>__<description>.sql`

Examples:

- `V1__create_identity.sql` — T01
- `V2__create_registration_cycles.sql` — T02
- `V3__create_topics_and_departments.sql` — T03
- `V4__create_groups_registrations.sql` — T04
- `V5__create_evaluation.sql` — T05

## Rules

- Never edit a migration after it has been shared or applied.
- Create a new migration for every schema change.
- Do not use Hibernate `ddl-auto=update`.
- Do not create an empty placeholder SQL migration.
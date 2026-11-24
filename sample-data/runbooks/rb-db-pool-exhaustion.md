# Runbook: Database Connection Pool Exhaustion
**ID**: `rb-db-pool-exhaustion-002`
**Tags**: `database`, `postgres`, `hikari`, `connections`

## Symptoms
- Error messages indicating `HikariPool-1 - Connection is not available, request timed out after 30000ms`.
- High count of active queries in `pg_stat_activity`.
- Spiking queue lengths for database read/write queries.

## Investigation Steps
1. Execute `SELECT count(*), state FROM pg_stat_activity GROUP BY state;`
2. Search for long-running idle in transaction or unindexed queries.
3. Check application logs for leaked connections or missing `@Transactional` bounds.

## Remediation Actions
- **Terminate Offending Backend**: Run `pg_terminate_backend(pid)` for blocking queries. Requires approval.
- **Scale Pool Size**: Increase `maximum-pool-size` dynamically or restart with updated Hikari configuration.
- **Reboot Service**: Gracefully restart service pods to purge unreleased connections. Requires approval.

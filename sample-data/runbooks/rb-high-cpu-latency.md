# Runbook: High CPU and Latency Spike Resolution
**ID**: `rb-high-cpu-latency-001`
**Tags**: `performance`, `cpu`, `latency`, `scaling`

## Symptoms
- Elevated CPU utilization (> 85%) sustained for over 5 minutes.
- Increased p99 API latency across dependent microservices.
- 504 Gateway Timeout or 500 Internal Server errors reported by ingress.

## Investigation Steps
1. Identify offending process or container using container metrics.
2. Check recent deployment history in the last 2 hours to detect regression.
3. Inspect thread dumps or event loop latency metrics.
4. Verify database connection pool saturation.

## Remediation Actions
- **Horizontal Scaling**: Scale deployment replicas using Kubernetes HPA or deployment spec.
- **Rollback**: If correlated with a recent release, initiate deployment rollback. Requires approval.
- **Circuit Breaking**: Enable downstream circuit breaker to prevent cascading failures.

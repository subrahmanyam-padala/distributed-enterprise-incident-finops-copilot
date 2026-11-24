# Runbook: Memory Leak and OOMKilled Container
**ID**: `rb-oom-memory-leak-003`
**Tags**: `memory`, `oom`, `jvm`, `heap`

## Symptoms
- Container status `OOMKilled` (Exit Code 137).
- Garbage collection pauses exceeding SLA threshold (> 2s pause).
- Heap memory continuously trending upward without recovery post-GC.

## Investigation Steps
1. Inspect container termination reasons with `kubectl describe pod`.
2. Analyze JVM heap histogram or Python heap allocation profiler.
3. Correlate memory rise with incoming request rate or large payload batch jobs.

## Remediation Actions
- **Temporary Memory Limit Increase**: Bump container memory limit by 50%.
- **Trigger Heap Dump**: Generate heap dump before restarting container for offline profiling.
- **Drain and Restart**: Gracefully drain traffic and restart unhealthy container replicas. Requires approval.

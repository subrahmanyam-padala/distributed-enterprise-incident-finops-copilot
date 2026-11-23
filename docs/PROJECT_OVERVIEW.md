# OpsPilot - Project Overview

## 1. Executive Summary

**Distributed Enterprise Incident & FinOps Copilot (OpsPilot)** is an enterprise software platform designed to solve two deeply interconnected operational challenges in modern software engineering organizations:

1. **High-Stress Production Incident Management**: During production failures, engineers spend up to 70% of incident response time context-switching across logs, monitoring dashboards, deployment histories, and disconnected wikis to locate root causes.
2. **Cloud Financial Opacity (FinOps)**: Engineering organizations suffer from unexpected cloud billing spikes, misconfigured infrastructure, and budget overruns with zero visibility into how specific deployments or incidents triggered the cost increases.

OpsPilot unifies these domains into a single resilient, distributed copilot. It ingests production alerts, manages the incident lifecycle with strict multi-tenant isolation, triggers asynchronous AI investigations over Apache Kafka, consults operational runbooks using Hybrid RAG (pgvector + lexical search), inspects real-time telemetry through Model Context Protocol (MCP) tools, correlates cost spikes with engineering changes using deterministic mathematics, and presents findings on an intuitive operations dashboard.

---

## 2. Core Tenets & Operating Principles

1. **Assistance Over Automation (Human-in-the-Loop)**: AI assists engineers by gathering evidence, ranking hypotheses, and proposing runbook remediations. **It never executes destructive or high-risk operational actions automatically.** All remediation requires authenticated human approval.
2. **Deterministic Financial Arithmetic**: Cloud cost analytics, budget threshold alerts, and anomaly detection use strict statistical algorithms (Z-score / rolling-window interquartile range). LLMs are strictly forbidden from performing financial arithmetic; they are only permitted to synthesize and explain calculated anomalies against verified deployment records.
3. **Traceable Evidence & Honest Citations**: Every finding presented by the AI must cite verified evidence (a specific metric spike, deployment commit, or retrieved runbook chunk). Hallucinated citations or unverified assertions are structurally rejected.
4. **Resilient Distributed Boundaries**: Inter-service communication relies on an asynchronous Kafka backbone with KRaft mode, transactional outbox publishing, idempotent consumer deduplication, and dead-letter queues (DLQ).
5. **Zero-Trust Multi-Tenancy**: Tenant identity is strictly derived from authenticated cryptographic server-side context (JWT claims), never from unverified request payloads.

---

## 3. System Personas & Role-Based Access Control (RBAC)

OpsPilot enforces fine-grained access control across three primary organizational roles:

| Role | Permissions | Typical User |
| :--- | :--- | :--- |
| **ADMIN** | Manage organization settings, invite/remove users, configure budgets, define alert thresholds, view all audit logs. | Site Reliability Engineering Director, Head of Infrastructure |
| **OPERATOR** | Acknowledge & resolve incidents, trigger AI re-triage, approve suggested remediations, upload runbooks, import billing data. | On-Call SRE, DevOps Engineer, Senior Backend Engineer |
| **VIEWER** | Read-only access to incident timelines, AI investigation summaries, FinOps dashboards, and operational metrics. | Engineering Manager, Product Owner, FinOps Analyst |

---

## 4. Key Value Proposition & Success Metrics

- **MTTR Reduction**: Cuts Mean Time to Resolution by automating evidence collection (logs, health checks, commit history) in parallel within seconds of alert ingestion.
- **Runbook Accessibility**: Directly maps relevant runbook sections to ongoing incidents using hybrid vector-lexical search with Reciprocal Rank Fusion (RRF).
- **Cost Spike Attribution**: Correlates cloud cost anomalies directly to specific microservice deployments or incident remediation scale-outs within hours rather than at month-end billing reviews.
- **Enterprise Auditability**: Maintains immutable audit logs of every alert received, incident state transition, triage event, and user action for post-mortem compliance.

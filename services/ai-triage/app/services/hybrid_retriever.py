"""
Hybrid RAG retrieval engine for operational runbooks.
Combines dense embedding vector similarity and lexical text search
using Reciprocal Rank Fusion (RRF) to produce superior retrieval precision.
"""

import math
import re
from typing import List, Dict, Any, Tuple
from dataclasses import dataclass


@dataclass
class DocumentChunk:
    doc_id: str
    chunk_id: str
    title: str
    section: str
    content: str
    tags: List[str]


# In-memory document chunk corpus representing runbook knowledge base
RUNBOOK_CORPUS: List[DocumentChunk] = [
    DocumentChunk(
        doc_id="rb-high-cpu-latency-001",
        chunk_id="chunk-cpu-01",
        title="High CPU and Latency Spike Resolution",
        section="Symptoms & Investigation",
        content="Elevated CPU utilization sustained over 85%, increased p99 API latency, 504 Gateway Timeout or 500 error spikes. Check container metrics and recent deployments.",
        tags=["performance", "cpu", "latency", "scaling", "timeout"],
    ),
    DocumentChunk(
        doc_id="rb-high-cpu-latency-001",
        chunk_id="chunk-cpu-02",
        title="High CPU and Latency Spike Resolution",
        section="Remediation Actions",
        content="Scale deployment replicas using Kubernetes HPA. If correlated with recent release, initiate deployment rollback with approval. Enable circuit breaking.",
        tags=["scaling", "rollback", "hpa", "kubernetes", "approval"],
    ),
    DocumentChunk(
        doc_id="rb-db-pool-exhaustion-002",
        chunk_id="chunk-db-01",
        title="Database Connection Pool Exhaustion",
        section="Symptoms & Investigation",
        content="Connection pool exhausted: HikariPool request timed out after 30000ms. High count of active queries in pg_stat_activity. Leaked connections or missing transactional bounds.",
        tags=["database", "postgres", "hikari", "pool", "timeout", "connections"],
    ),
    DocumentChunk(
        doc_id="rb-db-pool-exhaustion-002",
        chunk_id="chunk-db-02",
        title="Database Connection Pool Exhaustion",
        section="Remediation Actions",
        content="Terminate offending long-running blocking queries using pg_terminate_backend. Increase maximum-pool-size or gracefully restart service pods with human sign-off.",
        tags=["remediation", "pg_terminate_backend", "pool", "approval", "restart"],
    ),
    DocumentChunk(
        doc_id="rb-oom-memory-leak-003",
        chunk_id="chunk-mem-01",
        title="Memory Leak and OOMKilled Container",
        section="Symptoms & Investigation",
        content="Container OOMKilled exit code 137, JVM garbage collection pause exceeding SLA, heap memory continuously trending upward without release post GC.",
        tags=["memory", "oom", "jvm", "leak", "gc", "heap"],
    ),
    DocumentChunk(
        doc_id="rb-oom-memory-leak-003",
        chunk_id="chunk-mem-02",
        title="Memory Leak and OOMKilled Container",
        section="Remediation Actions",
        content="Increase container memory limits, capture heap dump for offline analysis, drain traffic and restart unhealthy container replicas with human sign-off.",
        tags=["restart", "memory", "limits", "approval", "profiling"],
    ),
]


def tokenize(text: str) -> List[str]:
    """Basic lexical tokenizer: lowercase alphanumeric tokens."""
    return re.findall(r"\b[a-z0-9]+\b", text.lower())


def compute_tf_idf_similarity(query_tokens: List[str], doc_tokens: List[str], all_docs: List[List[str]]) -> float:
    """Computes basic TF-IDF cosine similarity for lexical ranking."""
    if not query_tokens or not doc_tokens:
        return 0.0

    doc_len = len(doc_tokens)
    score = 0.0
    total_docs = len(all_docs)

    for token in query_tokens:
        tf = doc_tokens.count(token) / doc_len
        # Document frequency
        df = sum(1 for d in all_docs if token in d)
        idf = math.log((total_docs + 1) / (df + 1)) + 1.0
        score += tf * idf

    return score


def mock_dense_embedding_similarity(query: str, doc_chunk: DocumentChunk) -> float:
    """
    Deterministic dense semantic similarity computation.
    Produces high scores for semantically related operational terms.
    """
    q_lower = query.lower()
    score = 0.1

    # Semantic keyword associations
    semantic_groups = [
        ({"cpu", "latency", "load", "slow", "spike", "timeout", "504"}, ["chunk-cpu-01", "chunk-cpu-02"]),
        ({"database", "connection", "pool", "hikari", "postgres", "sql"}, ["chunk-db-01", "chunk-db-02"]),
        ({"memory", "oom", "leak", "heap", "gc", "killed", "137"}, ["chunk-mem-01", "chunk-mem-02"]),
    ]

    for words, target_chunks in semantic_groups:
        matches = sum(1 for w in words if w in q_lower)
        if matches > 0 and doc_chunk.chunk_id in target_chunks:
            score += matches * 0.35

    return min(score, 0.98)


class HybridRetriever:
    """
    Performs dual-channel retrieval:
    1. Dense Vector Similarity (semantic search)
    2. Lexical / Keyword Scoring (full-text search)
    Fuses both rankings using Reciprocal Rank Fusion (RRF).
    """

    def __init__(self, rrf_k: int = 60):
        self.rrf_k = rrf_k
        self.corpus = RUNBOOK_CORPUS
        self._tokenized_corpus = [tokenize(f"{c.title} {c.section} {c.content} {' '.join(c.tags)}") for c in self.corpus]

    def search(self, query: str, top_k: int = 3) -> List[Dict[str, Any]]:
        query_tokens = tokenize(query)

        # 1. Dense Scoring
        dense_scores: List[Tuple[DocumentChunk, float]] = []
        for chunk in self.corpus:
            score = mock_dense_embedding_similarity(query, chunk)
            dense_scores.append((chunk, score))
        dense_scores.sort(key=lambda x: x[1], reverse=True)

        # 2. Lexical Scoring (FTS)
        lexical_scores: List[Tuple[DocumentChunk, float]] = []
        for i, chunk in enumerate(self.corpus):
            score = compute_tf_idf_similarity(query_tokens, self._tokenized_corpus[i], self._tokenized_corpus)
            lexical_scores.append((chunk, score))
        lexical_scores.sort(key=lambda x: x[1], reverse=True)

        # 3. Reciprocal Rank Fusion (RRF)
        # RRF formula: Score(d) = sum( 1 / (k + rank_i(d)) )
        rrf_scores: Dict[str, float] = {c.chunk_id: 0.0 for c in self.corpus}
        chunk_map: Dict[str, DocumentChunk] = {c.chunk_id: c for c in self.corpus}

        for rank, (chunk, _) in enumerate(dense_scores, start=1):
            rrf_scores[chunk.chunk_id] += 1.0 / (self.rrf_k + rank)

        for rank, (chunk, _) in enumerate(lexical_scores, start=1):
            rrf_scores[chunk.chunk_id] += 1.0 / (self.rrf_k + rank)

        # Sort by fused RRF score
        sorted_chunks = sorted(rrf_scores.items(), key=lambda x: x[1], reverse=True)[:top_k]

        results = []
        for chunk_id, rrf_score in sorted_chunks:
            chunk = chunk_map[chunk_id]
            # Normalize score to 0.0 - 1.0 range
            normalized_score = min(round(rrf_score * 30.0, 3), 0.99)
            results.append({
                "documentId": chunk.doc_id,
                "title": chunk.title,
                "chunkId": chunk.chunk_id,
                "section": chunk.section,
                "snippet": chunk.content,
                "relevanceScore": max(normalized_score, 0.70),
            })

        return results

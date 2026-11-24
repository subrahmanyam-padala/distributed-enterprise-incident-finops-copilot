"""
OpsPilot — End-to-End System Integration & Health Verification Script

Validates the full multi-service architecture:
1. AI Triage Pipeline (LangGraph StateGraph, Hybrid RAG retriever, trace correlation)
2. MCP Server (Operational telemetry tools, auth, headers)
3. Web Dashboard (TypeScript builds, production artifacts)
"""

import sys
import os
import subprocess

def log_step(name: str):
    print(f"\n========================================================")
    print(f"  [RUNNING] {name}")
    print(f"========================================================")

def log_success(msg: str):
    print(f"  [PASS] {msg}")

def log_failure(msg: str):
    print(f"  [FAIL] {msg}")

def verify_python_triage_engine(root_dir: str) -> bool:
    log_step("1. Verifying Python AI Triage Engine Components")
    script = """
import sys
from app.workflows.investigation_graph import build_investigation_graph, InvestigationState
from app.services.hybrid_retriever import HybridRetriever
from app.middleware.trace_correlation import extract_correlation_id

# 1. StateGraph
graph = build_investigation_graph()
assert graph is not None, "Failed to compile LangGraph investigation workflow"

# 2. Hybrid RAG
retriever = HybridRetriever()
docs = retriever.search(query="High CPU latency spike timeout", top_k=2)
assert len(docs) > 0, "Hybrid retriever returned no runbooks"

# 3. Trace correlation
headers = {"x-correlation-id": "e2e-trace-999"}
cid = extract_correlation_id(headers)
assert cid == "e2e-trace-999", "Failed to extract correlation ID"

print(f"PASS: LangGraph compiled, Hybrid RRF returned {len(docs)} citations, trace extracted.")
"""
    triage_dir = os.path.join(root_dir, "services", "ai-triage")
    res = subprocess.run([sys.executable, "-c", script], cwd=triage_dir, capture_output=True, text=True)
    if res.returncode == 0:
        log_success(res.stdout.strip())
        return True
    else:
        log_failure(f"AI Triage verification failed:\n{res.stderr}")
        return False

def verify_python_mcp_server(root_dir: str) -> bool:
    log_step("2. Verifying Python MCP Operational Telemetry Server")
    script = """
import sys
from app.main import app
from fastapi.testclient import TestClient
from app.config import settings

client = TestClient(app)

# 1. Health
res_health = client.get("/api/v1/health")
assert res_health.status_code == 200, f"Health check failed: {res_health.status_code}"
assert res_health.json()["status"] == "UP"

# 2. Auth protection
res_unauth = client.get("/api/v1/tools/deployments/recent?service=checkout-service")
assert res_unauth.status_code == 401, f"Expected 401 Unauthorized, got {res_unauth.status_code}"

# 3. Telemetry tools with auth
auth_headers = {"Authorization": f"Bearer {settings.mcp_auth_token}"}
res_deploy = client.get("/api/v1/tools/deployments/recent?service=checkout-service", headers=auth_headers)
assert res_deploy.status_code == 200 and res_deploy.json()["success"] is True

res_diff = client.get("/api/v1/tools/deployments/diff?service=checkout-service&base_version=v2.4.0&target_version=v2.4.1", headers=auth_headers)
assert res_diff.status_code == 200 and res_diff.json()["data"]["configChangesDetected"] is True

print("PASS: MCP Health UP, auth boundary enforced, telemetry tools verified.")
"""
    mcp_dir = os.path.join(root_dir, "services", "mcp-server")
    res = subprocess.run([sys.executable, "-c", script], cwd=mcp_dir, capture_output=True, text=True)
    if res.returncode == 0:
        log_success(res.stdout.strip())
        return True
    else:
        log_failure(f"MCP Server verification failed:\n{res.stderr}")
        return False

def verify_frontend_artifacts(root_dir: str) -> bool:
    log_step("3. Verifying React Web Frontend Production Build Artifacts")
    dist_dir = os.path.join(root_dir, "apps", "web", "dist")
    index_html = os.path.join(dist_dir, "index.html")
    assets_dir = os.path.join(dist_dir, "assets")

    if not os.path.exists(index_html):
        log_failure(f"Missing build file: {index_html}. Please run `npm run build` in apps/web.")
        return False

    assets = os.listdir(assets_dir) if os.path.exists(assets_dir) else []
    js_files = [f for f in assets if f.endswith(".js")]
    css_files = [f for f in assets if f.endswith(".css")]

    assert len(js_files) > 0, "No bundled JS asset found"
    assert len(css_files) > 0, "No bundled CSS asset found"

    log_success(f"Verified production index.html and {len(assets)} compiled assets (JS: {js_files[0]}, CSS: {css_files[0]})")
    return True

def main():
    print("""
====================================================================
   OpsPilot — Incident & FinOps Copilot E2E Smoke Verification
====================================================================
""")
    root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    triage_ok = verify_python_triage_engine(root_dir)
    mcp_ok = verify_python_mcp_server(root_dir)
    frontend_ok = verify_frontend_artifacts(root_dir)

    print("\n--------------------------------------------------------------------")
    if triage_ok and mcp_ok and frontend_ok:
        print("  🎉 ALL OPSPiLOT ARCHITECTURAL PILLARS PASS VERIFICATION!")
        print("--------------------------------------------------------------------")
        sys.exit(0)
    else:
        print("  ❌ ONE OR MORE VERIFICATIONS FAILED")
        print("--------------------------------------------------------------------")
        sys.exit(1)

if __name__ == "__main__":
    main()

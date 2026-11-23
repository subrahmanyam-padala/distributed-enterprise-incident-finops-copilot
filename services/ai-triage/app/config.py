"""
Application configuration loaded from environment variables.
Uses pydantic-settings for type-safe, validated configuration.
"""

from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import List


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # Application
    app_env: str = "development"
    log_level: str = "INFO"

    # FastAPI
    ai_triage_host: str = "0.0.0.0"
    ai_triage_port: int = 8000

    # PostgreSQL (async)
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "opspilot"
    postgres_user: str = "opspilot_user"
    postgres_password: str = "opspilot_secret_change_in_prod"

    @property
    def async_database_url(self) -> str:
        return (
            f"postgresql+asyncpg://{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group_id: str = "opspilot-ai-triage"

    # LLM Provider
    llm_provider: str = "mock"  # "mock" | "openai" | "anthropic" | "gemini"
    llm_api_key: str = ""
    llm_model: str = "gemini-1.5-pro"

    # MCP Server
    mcp_server_url: str = "http://localhost:8001"
    mcp_auth_token: str = "mcp_local_dev_token_change_in_prod"

    # CORS
    cors_allowed_origins: List[str] = ["http://localhost:5173", "http://localhost:3000"]

    # Topics
    kafka_topic_triage_requested: str = "incident.triage.requested.v1"
    kafka_topic_triage_completed: str = "incident.triage.completed.v1"
    kafka_topic_triage_dlq: str = "incident.triage.dlq.v1"


settings = Settings()

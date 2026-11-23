"""MCP Server configuration."""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False)

    app_env: str = "development"
    mcp_server_port: int = 8001
    mcp_server_host: str = "0.0.0.0"
    mcp_auth_token: str = "mcp_local_dev_token_change_in_prod"

    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "opspilot"
    postgres_user: str = "opspilot_user"
    postgres_password: str = "opspilot_secret_change_in_prod"


settings = Settings()

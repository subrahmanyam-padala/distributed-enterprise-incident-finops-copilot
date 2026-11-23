-- ==============================================================================
-- OpsPilot PostgreSQL Initialization Script
-- Runs once on first container startup
-- ==============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Confirm extensions are enabled
SELECT extname, extversion FROM pg_extension WHERE extname IN ('uuid-ossp', 'vector', 'pg_trgm');

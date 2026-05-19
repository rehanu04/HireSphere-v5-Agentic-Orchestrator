# Supabase pgvector & HNSW Integration Plan

This plan outlines the steps to securely connect the FastAPI backend to Supabase, configure `pgvector` with HNSW indexing for energy-efficient retrieval, and implement the "Sober and Durable" retrieval logic that prioritizes sustainability and agent stability.

## User Review Required

> [!WARNING]
> To execute the SQL commands required to create the `pgvector` extension, tables, HNSW indices, and RPC functions, we need a way to run SQL on your Supabase instance.
> **Option A**: I can generate a `supabase_setup.sql` file containing all the raw SQL, which you can manually paste and execute in your Supabase Dashboard's SQL Editor.
> **Option B**: If you provide the direct PostgreSQL connection string (e.g., `postgresql://postgres.[ref]:[password]@aws-0-[region].pooler.supabase.com:6543/postgres`) in a `.env` file, I can use `psycopg2` to run the migration directly from Python.
>
> Please indicate your preference (Option A is standard and highly recommended for security).

## Proposed Changes

---

### Backend Dependencies
#### [MODIFY] [backend-ai/requirements.txt](file:///c:/Users/rehan/AndroidProjects/ResumeMatchV2/backend-ai/requirements.txt)
- Add `supabase` for connecting to the Supabase REST and RPC API.
- Add `python-dotenv` for managing environment variables (like `SUPABASE_URL` and `SUPABASE_KEY`).

---

### Database Schema & Vector Indexing (SQL)
#### [NEW] [backend-ai/supabase_setup.sql](file:///c:/Users/rehan/AndroidProjects/ResumeMatchV2/backend-ai/supabase_setup.sql)
Create the database migration script. It will include:
1. `CREATE EXTENSION IF NOT EXISTS vector;`
2. `CREATE TABLE talent_embeddings (...)` including `id`, `profile_id`, `embedding` (vector), `sustainability_index`, and `agent_stability`.
3. **HNSW Indexing**: `CREATE INDEX ON talent_embeddings USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 100);` to meet the $O(d \cdot \log N)$ Green Coding standard.
4. **Sober & Durable RPC**: Create a Postgres function `match_talent` that takes a query vector. It will first retrieve the top candidates using the HNSW index (fast, low energy), and then re-rank them by adding weighted bonuses for the `sustainability_index` and `agent_stability` scalars.

---

### FastAPI Backend Integration
#### [MODIFY] [backend-ai/main.py](file:///c:/Users/rehan/AndroidProjects/ResumeMatchV2/backend-ai/main.py)
- **Supabase Client Initialization**: Securely initialize `supabase.create_client` using env variables.
- **`POST /v1/gauntlet/vector_search`**: An endpoint to take a query embedding, call the `match_talent` RPC on Supabase, and return the prioritized candidates.
- **`POST /v1/gauntlet/smoke_test_vector`**: A dedicated smoke test endpoint that will:
  1. Insert a dummy vector with high sustainability and stability.
  2. Insert another dummy vector with low scores.
  3. Query the store to verify the high-scoring vector is prioritized correctly.
  4. Clean up (delete) the test vectors.

---

### Active Context Updates
#### [MODIFY] [.agents/activeContext.md](file:///c:/Users/rehan/AndroidProjects/ResumeMatchV2/.agents/activeContext.md)
- Check off the Supabase pgvector connection and HNSW indexing tasks.
- Document that the data infrastructure is production-ready for DeployFest 2026.

## Verification Plan

### Automated Tests
- Trigger the newly created `POST /v1/gauntlet/smoke_test_vector` endpoint.
- Verify the HTTP response explicitly shows the "Sober and Durable" re-ranking successfully prioritizing the dummy candidate with the higher Sustainability Index and Agent Stability.

### Manual Verification
- You will verify the newly created `talent_embeddings` table and HNSW index in the Supabase Dashboard.
- You will review the backend terminal output for any errors during connection initialization.

# Technical Context: Durable Retrieval & State Logic

## 1. High-Performance Vector Retrieval (HNSW)
HireSphere v5 mandates **HNSW (Hierarchical Navigable Small World)** indexing to maintain logarithmic search complexity[cite: 2].



### The $O(d \cdot \log N)$ Standard
*   **Complexity**: Retrieval time must scale at $O(d \cdot \log N)$, where $d$ is vector dimensionality and $N$ is the number of vectors[cite: 2].
*   **Tuning Parameters**:
    *   **m (Max connections)**: Set between 16 and 48 for dense talent embeddings[cite: 2].
    *   **ef_construction**: Standardized at 100–200 to ensure high-quality proximity graphs and prevent local clusters[cite: 2].
    *   **ef_search**: Dynamically adjusted to balance query latency vs. recall accuracy[cite: 2].

## 2. Checkpointed Recovery Logic (Durable Execution)
To prevent state loss during crashes, we implement **P-HNSW (Persistent HNSW)**[cite: 2].
*   **Logging**: Use **NLog** and **NlistLog** to record graph mutations in persistent memory[cite: 2].
*   **Recovery**: System must be capable of reliable recovery with negligible overhead compared to traditional SSD-based mechanisms[cite: 2].
*   **Memory Bank Truth**: The `.agents/` directory serves as the persistent source of truth for agent session continuity[cite: 1, 2].

## 3. Tech Stack
*   **Frontend**: Kotlin 2.1, Jetpack Compose 1.8[cite: 2].
*   **Backend**: FastAPI 0.115, Supabase (Postgres 17 + pgvector)[cite: 1, 2].
*   **AI Engine**: Google Antigravity (Gemini 3.1 Pro)[cite: 1, 2].
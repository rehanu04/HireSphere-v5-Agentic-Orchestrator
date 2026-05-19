# Agent Team: Mission Rules & Governance

## 1. Terminal-First Workflow Policy
All agents adhere to the **"Request Review"** execution policy[cite: 1, 2].
*   **Rule**: Agents are forbidden from executing potentially destructive terminal commands (e.g., database migrations) without human authorization via the **Inbox**[cite: 2].
*   **Action**: Agents must provide a clear **Implementation Plan** before opening a terminal[cite: 1, 2].

## 2. SROM Factual Grounding (The Audit Trail)
Every decision must satisfy Deutsche Bank’s financial governance via **Self-Reflexive Ontology Mapping (SROM)**[cite: 1, 2].
*   **Rule**: Decompose all reasoning into **SROM Atoms**—the smallest indivisible units of verifiable knowledge[cite: 2].
*   **Verification**: Cross-reference every atom against authoritative grounding documents using atomic-level Natural Language Inference (NLI)[cite: 2].
*   **Traceability**: Every AI-driven decision must include a **"Forensic Root Cause"** analysis artifact[cite: 1, 2].

## 3. Multi-Agent Handover
*   **@pm Agent**: Designs requirements and outputs to `Technical_Specification.md`[cite: 1].
*   **@engineer Agent**: Consumes specs to generate code in the `app_build/` directory[cite: 1, 2].
*   **@qa Agent**: Executes the `audit_code` skill to hunt for dependency mismatches and logic breaks[cite: 1].

## 4. Operational Guardrails
*   **Progressive Disclosure**: Load specialized `SKILL.md` files only when relevant to the task[cite: 1, 2].
*   **Security**: Any attempt to bypass IAM or VPC Service Controls triggers immediate session termination[cite: 1, 2].
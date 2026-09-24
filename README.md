# ShadowPilot 🛡️🧠

> **Let an AI rehearse its actions before it touches the real world.**

ShadowPilot is an open-source middleware for AI agents that creates a **shadow execution** of tool calls before they are allowed to create real side effects.

Instead of asking only *"Can the model do this?"*, ShadowPilot asks:

- What will this action change?
- Is it reversible?
- Does it cross a real-world boundary?
- What does it cost?
- Should a human approve it?
- Can we produce a tamper-evident receipt of what the agent intended?

## Why this project

Open-source AI is increasingly moving toward long-lived agents, reusable skills, local execution, and agent observability. ShadowPilot takes a different layer of the stack: **pre-execution simulation and impact control**.

Think of it as a flight simulator + firewall for agent actions.

## 60-second demo

```bash
git clone https://github.com/artclass11/PRO.git
cd PRO
python -m pip install -e .
shadowpilot
```

The demo produces previews like:

```json
{
  "action": "charge",
  "mode": "shadow",
  "risk": "CRITICAL",
  "impact": {
    "score": 6,
    "level": "CRITICAL",
    "reasons": [
      "external side effect",
      "irreversible",
      "cost exceeds policy",
      "high-impact tool class"
    ]
  }
}
```

## Core idea

```text
                    ┌────────────────────┐
AI agent ──────────► │   ShadowPilot      │
                    │                    │
                    │  1. inspect action │
                    │  2. shadow-run     │
                    │  3. estimate impact│
                    │  4. apply policy   │
                    │  5. issue receipt  │
                    └───────┬────────────┘
                            │
                 ┌──────────┴───────────┐
                 ▼                      ▼
          safe/reversible          real-world action
             execute                    execute

            No approval              approval/policy
               needed                   required
```

## What makes it different

ShadowPilot is designed around an **action contract**, not a chat interface. An action describes its reversibility, external effects, cost, tool class, and arguments. The same contract can sit in front of coding agents, browser agents, finance tools, automation systems, or local scripts.

The receipt also includes a SHA-256 digest of the decision record so downstream systems can detect accidental or deliberate record changes.

## Roadmap

### v0.2 — adapters
- MCP tool adapter
- REST/webhook adapter
- filesystem sandbox adapter
- browser action adapter

### v0.3 — real shadow world
- temporary git worktree simulation
- HTTP request recorder/replayer
- database transaction sandbox
- synthetic browser DOM

### v0.4 — impact graph
- dependency-aware blast radius
- data classification
- permission graph
- cost and latency budgets

### v0.5 — counterfactual agents
Run two or more candidate plans in shadow mode and compare predicted side effects before execution.

### v1.0 — universal agent safety layer
A provider-neutral protocol for **simulate → inspect → approve → execute → receipt**.

## Design principles

- Local-first by default
- No model provider lock-in
- Deterministic policy decisions
- Explicit human approval for high-impact actions
- Reproducible receipts
- Small core, optional adapters

## Security note

ShadowPilot is a control layer, not a proof that an action is safe. Simulation quality depends on the adapters and models behind it. Never treat a shadow preview as a guarantee of real-world outcomes.

## Contributing

The project is intentionally small. The easiest way to contribute is to add an adapter plus tests for a real tool category.

## License

MIT

## Security posture

ShadowPilot is intended to be deployed with least privilege. Keep credentials outside the repository, use synthetic fixtures for tests, and require an explicit approval policy before connecting adapters that can perform external side effects.

The repository includes CodeQL, dependency auditing, Dependabot configuration, and a security policy template.

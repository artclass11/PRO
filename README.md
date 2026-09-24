# ShadowPilot 🛡️

> Let an AI rehearse its actions before it touches the real world.

ShadowPilot is an open-source, provider-neutral middleware for AI agents. It creates a shadow execution of tool calls, estimates impact, applies deterministic policy, and records a tamper-evident receipt before a real side effect is allowed.

## Lifecycle

inspect → shadow-run → assess → approve → execute → receipt

## 60-second demo

    git clone https://github.com/artclass11/PRO.git
    cd PRO
    python -m pip install -e '.[dev]'
    pytest
    shadowpilot

The built-in CLI demonstrates a safe preview and a high-risk example without performing a real external side effect.

## What makes it different

Most agent frameworks focus on generating the next tool call. ShadowPilot focuses on the boundary immediately before the side effect.

An Action describes reversibility, external effects, and estimated cost. A Policy converts those signals into a deterministic risk level and execution gate. Receipts include a SHA-256 digest so later record changes can be detected.

## Architecture

    AI agent
       │
       ▼
    ┌───────────────┐
    │ Action        │
    │ tool + args   │
    │ impact flags  │
    └───────┬───────┘
            ▼
    ┌────────────────────────┐
    │ ShadowPilot            │
    │ preview → assess       │
    │ policy → receipt       │
    └──────────┬─────────────┘
               │
         ┌─────┴──────┐
         ▼            ▼
      blocked      execute
                       │
                       ▼
                    receipt

## Example

    from shadowpilot import Action, Policy, RiskLevel, ShadowPilot

    pilot = ShadowPilot(
        Policy(max_risk_to_execute=RiskLevel.LOW, max_cost=10)
    )

    pilot.register(
        "write_file",
        lambda args, shadow: {
            "operation": "write",
            "path": args["path"],
            "bytes": len(args["content"]),
            "shadow": shadow,
        },
    )

    action = Action(
        "draft",
        "write_file",
        {"path": "notes.txt", "content": "hello"},
        reversible=True,
    )

    result = pilot.run(action, execute=False)
    print(result.risk.name)
    print(result.preview)

## Security boundary

ShadowPilot is a control layer, not a sandbox. Registered handlers are trusted code and must keep preview mode side-effect free.

The project deliberately does not claim that a shadow preview proves a real-world action is safe. Pair it with least-privilege credentials, OS/container isolation, authorization controls, and transactional safeguards.

See SECURITY.md and docs/THREAT_MODEL.md.

## Project hygiene

- MIT licensed
- Python 3.10+
- dependency-free runtime core
- automated tests
- Dependabot
- CodeQL and security workflows
- contribution, issue, and pull-request templates

## Roadmap

v0.2 — MCP, REST/webhook, filesystem-sandbox and browser adapters.

v0.3 — temporary git worktrees, HTTP recorder/replayer, database transaction sandboxes and synthetic browser DOMs.

v0.4 — dependency-aware blast radius, data classification, permission graphs, cost/latency budgets.

v1.0 — provider-neutral protocol for simulate → inspect → approve → execute → receipt.

## Contributing

Read CONTRIBUTING.md. New adapters should include tests demonstrating that preview mode does not produce external side effects.

## License

MIT.

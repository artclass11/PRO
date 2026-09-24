# ShadowPilot 🛡️

> Rehearse the side effect. Gate the side effect. Record the decision.

ShadowPilot is a provider-neutral Python middleware for AI agents that turns proposed tool calls into a controlled lifecycle: **preview → assess → approve → execute → receipt**.

## Start

```bash
git clone https://github.com/artclass11/PRO.git
cd PRO
python -m pip install -e '.[dev]'
pytest
shadowpilot demo
```

## Security model

Adapters provide separate `preview()` and `execute()` functions. ShadowPilot applies deterministic policy checks for reversibility, external effects, cost, data classification, and blocked tools. Receipts are hash-chained and redact common credential-shaped values.

ShadowPilot is a control layer, not a sandbox: adapter preview implementations must remain side-effect free.

## Status

v0.2.0 alpha. See `CONTRIBUTING.md`, `SECURITY.md`, and `docs/THREAT_MODEL.md`.

## License

MIT.
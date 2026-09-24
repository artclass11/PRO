# Contributing to ShadowPilot

Thanks for helping build a small, inspectable safety layer for AI agents.

## Development

    python -m pip install -e '.[dev]'
    pytest -q

## Contribution rules

- Keep the core dependency-free.
- Prefer explicit policy rules over opaque heuristics.
- Never place real credentials or customer data in tests.
- Add regression tests for security-sensitive changes.
- Keep preview paths side-effect free.
- Document changes to the trust boundary.

## Pull requests

Use a focused title and describe the behavior change, tests, and security impact. See .github/PULL_REQUEST_TEMPLATE.md.

## Issues

Security vulnerabilities should not be reported in public issues. See SECURITY.md.

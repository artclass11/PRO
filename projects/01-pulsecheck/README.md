# Day 01 — PulseCheck

PulseCheck is a small, dependency-free HTTP/TLS health checker for developers, CI jobs, homelabs, and small teams.

## Features
- HTTP/HTTPS status checks
- Redirect-aware final URL
- Request latency
- Basic TLS certificate expiry visibility for HTTPS targets
- Human-readable or JSON output
- Non-zero exit code when any target fails

## Usage
```bash
python -m pulsecheck https://example.com https://example.org
python -m pulsecheck https://example.com --json
```

The implementation lives beside this README under `src/pulsecheck/`; tests are under `tests/`.
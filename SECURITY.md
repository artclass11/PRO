# Security Policy

## Supported versions

Only the latest release on the default branch is actively supported.

## Reporting a vulnerability

Please do **not** open a public GitHub issue for a suspected security vulnerability.

Until a security contact is configured for this repository, report privately through GitHub's private vulnerability reporting feature if it is enabled for the repository. Include reproduction steps, affected files, impact, and any suggested mitigation.

Never include API keys, passwords, tokens, private customer data, or other secrets in an issue or pull request.

## Secure-by-default principles

- No credentials are stored in source code.
- External side effects should remain blocked by policy until explicitly approved.
- Test and simulation paths should use synthetic data.
- Generated receipts should not contain secrets or sensitive payloads.

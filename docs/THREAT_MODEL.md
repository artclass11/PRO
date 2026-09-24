# ShadowPilot Threat Model

## Purpose

ShadowPilot sits between an AI planner and a tool boundary. Its job is to reduce accidental or policy-violating side effects before execution.

## In scope

- accidental execution of high-impact actions
- missing human approval for configured external effects
- basic tampering with stored receipts
- leakage of obvious credential-shaped strings into receipts
- inconsistent or untestable risk decisions

## Out of scope

- a malicious adapter that lies about its preview implementation
- operating-system compromise
- full rollback of third-party systems
- correctness of a model's natural-language reasoning

## Trust boundaries

The registered handler is trusted code. ShadowPilot does not sandbox arbitrary Python. A preview handler must honor the shadow=True contract and remain side-effect free.

The receipt hash chain provides tamper evidence. It does not provide signer identity, non-repudiation, or protection if an attacker can rewrite every record and its verification context.

For high-assurance systems, pair ShadowPilot with least-privilege credentials, OS/container isolation, external authorization, and an append-only signed audit store.

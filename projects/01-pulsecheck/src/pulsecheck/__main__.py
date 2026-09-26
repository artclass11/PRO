from __future__ import annotations

import argparse
import json
import socket
import ssl
import time
import urllib.error
import urllib.request
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlparse

@dataclass
class CheckResult:
    url: str
    ok: bool
    status: int | None
    latency_ms: float | None
    final_url: str | None
    tls_days_remaining: int | None
    error: str | None

def tls_days_remaining(hostname: str, timeout: float) -> int | None:
    ctx = ssl.create_default_context()
    with socket.create_connection((hostname, 443), timeout=timeout) as sock:
        with ctx.wrap_socket(sock, server_hostname=hostname) as tls_sock:
            cert = tls_sock.getpeercert()
    expires = cert.get("notAfter")
    if not expires:
        return None
    expires_at = datetime.strptime(expires, "%b %d %H:%M:%S %Y %Z").replace(tzinfo=timezone.utc)
    return int((expires_at - datetime.now(timezone.utc)).total_seconds() // 86400)

def check(url: str, timeout: float = 10.0) -> CheckResult:
    request = urllib.request.Request(url, headers={"User-Agent": "pulsecheck/0.1"})
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            latency = (time.perf_counter() - started) * 1000
            final_url = response.geturl()
            status = response.status
            tls_days = None
            if final_url.startswith("https://"):
                host = urlparse(final_url).hostname or ""
                tls_days = tls_days_remaining(host, timeout)
            return CheckResult(url, 200 <= status < 400, status, round(latency, 2), final_url, tls_days, None)
    except (urllib.error.URLError, TimeoutError, socket.timeout, ssl.SSLError, OSError) as exc:
        latency = (time.perf_counter() - started) * 1000
        return CheckResult(url, False, None, round(latency, 2), None, None, str(exc))

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Small dependency-free HTTP/TLS health checker.")
    parser.add_argument("urls", nargs="+", help="HTTP(S) URLs to check")
    parser.add_argument("--timeout", type=float, default=10.0, help="request timeout in seconds")
    parser.add_argument("--json", action="store_true", help="emit machine-readable JSON")
    args = parser.parse_args(argv)
    results: list[dict[str, Any]] = [asdict(check(url, args.timeout)) for url in args.urls]
    if args.json:
        print(json.dumps(results, indent=2))
    else:
        for item in results:
            marker = "OK" if item["ok"] else "FAIL"
            status = item["status"] if item["status"] is not None else "-"
            latency = item["latency_ms"] if item["latency_ms"] is not None else "-"
            print(f"{marker:4} {status:>3} {latency:>8} ms  {item['url']}")
            if item["error"]:
                print(f"     {item['error']}")
    return 0 if all(item["ok"] for item in results) else 1

if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations
import hashlib
import json
import time
import uuid
from dataclasses import asdict, dataclass, field
from enum import IntEnum
from typing import Any, Callable

class RiskLevel(IntEnum):
    SAFE = 0
    LOW = 1
    MEDIUM = 2
    HIGH = 3
    CRITICAL = 4

@dataclass
class Action:
    name: str
    tool: str
    args: dict[str, Any] = field(default_factory=dict)
    description: str = ""
    reversible: bool = True
    external_side_effect: bool = False
    estimated_cost: float = 0.0

@dataclass
class ActionResult:
    action_id: str
    mode: str
    risk: RiskLevel
    impact: dict[str, Any]
    preview: Any = None
    receipt: dict[str, Any] | None = None

@dataclass
class Policy:
    max_risk_to_execute: RiskLevel = RiskLevel.LOW
    max_cost: float = 1.0
    require_confirmation_for_external: bool = True

Handler = Callable[[dict[str, Any], bool], Any]

class ShadowPilot:
    """Small middleware for shadow-running AI tool calls before real execution."""
    def __init__(self, policy: Policy | None = None):
        self.policy = policy or Policy()
        self.handlers: dict[str, Handler] = {}
        self.receipts: list[dict[str, Any]] = []

    def register(self, tool: str, handler: Handler) -> None:
        self.handlers[tool] = handler

    def assess(self, action: Action) -> tuple[RiskLevel, dict[str, Any]]:
        score = 0
        reasons: list[str] = []
        if action.external_side_effect:
            score += 2
            reasons.append("external side effect")
        if not action.reversible:
            score += 2
            reasons.append("irreversible")
        if action.estimated_cost > self.policy.max_cost:
            score += 1
            reasons.append("cost exceeds policy")
        if action.tool in {"delete", "payment", "send_email", "publish", "shell"}:
            score += 1
            reasons.append("high-impact tool class")
        level = RiskLevel(min(score, int(RiskLevel.CRITICAL)))
        impact = {
            "score": score,
            "level": level.name,
            "reasons": reasons,
            "reversible": action.reversible,
            "external_side_effect": action.external_side_effect,
            "estimated_cost": action.estimated_cost,
        }
        return level, impact

    def _receipt(self, action: Action, mode: str, risk: RiskLevel, impact: dict[str, Any], preview: Any) -> dict[str, Any]:
        payload = {
            "action_id": str(uuid.uuid4()),
            "timestamp": int(time.time()),
            "mode": mode,
            "action": asdict(action),
            "risk": risk.name,
            "impact": impact,
            "preview": preview,
        }
        canonical = json.dumps(payload, sort_keys=True, default=str).encode()
        payload["hash"] = hashlib.sha256(canonical).hexdigest()
        return payload

    def run(self, action: Action, *, execute: bool = False, approved: bool = False) -> ActionResult:
        risk, impact = self.assess(action)
        handler = self.handlers.get(action.tool)
        if handler is None:
            preview = {"status": "no handler", "tool": action.tool, "args": action.args}
        else:
            try:
                preview = handler(action.args, True)
            except Exception as exc:
                preview = {"status": "preview_error", "error": type(exc).__name__, "message": str(exc)}
        action_id = str(uuid.uuid4())
        mode = "shadow"
        receipt = self._receipt(action, mode, risk, impact, preview)
        receipt["action_id"] = action_id
        if execute:
            blocked = False
            reasons = []
            if risk > self.policy.max_risk_to_execute:
                blocked = True
                reasons.append(f"risk {risk.name} exceeds policy {self.policy.max_risk_to_execute.name}")
            if action.external_side_effect and self.policy.require_confirmation_for_external and not approved:
                blocked = True
                reasons.append("external action requires approval")
            if blocked:
                impact = {**impact, "blocked": True, "block_reasons": reasons}
                receipt["impact"] = impact
                result = ActionResult(action_id, "blocked", risk, impact, preview, receipt)
                self.receipts.append(receipt)
                return result
            if handler is None:
                impact = {**impact, "executed": False, "error": "no handler"}
            else:
                try:
                    preview = handler(action.args, False)
                    mode = "executed"
                except Exception as exc:
                    impact = {**impact, "executed": False, "error": type(exc).__name__}
            receipt = self._receipt(action, mode, risk, impact, preview)
            receipt["action_id"] = action_id
        result = ActionResult(action_id, mode, risk, impact, preview, receipt)
        self.receipts.append(receipt)
        return result

    def export_receipts(self) -> list[dict[str, Any]]:
        return list(self.receipts)

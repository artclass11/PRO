from shadowpilot import Action, Policy, RiskLevel, ShadowPilot

def test_safe_shadow_run_does_not_execute():
    calls = []
    pilot = ShadowPilot()
    pilot.register("write", lambda args, shadow: calls.append((args, shadow)) or {"ok": True})
    result = pilot.run(Action("write", "write", {"path": "a.txt"}), execute=False)
    assert result.mode == "shadow"
    assert result.risk == RiskLevel.SAFE
    assert calls == [({"path": "a.txt"}, True)]

def test_external_irreversible_action_is_blocked_by_default():
    pilot = ShadowPilot(Policy(max_risk_to_execute=RiskLevel.LOW))
    pilot.register("payment", lambda args, shadow: {"charged": not shadow})
    result = pilot.run(Action("pay", "payment", {"amount": 100}, reversible=False, external_side_effect=True), execute=True)
    assert result.mode == "blocked"
    assert "requires approval" in " ".join(result.impact["block_reasons"])

def test_approved_action_can_execute():
    pilot = ShadowPilot(Policy(max_risk_to_execute=RiskLevel.CRITICAL))
    pilot.register("payment", lambda args, shadow: {"charged": not shadow})
    result = pilot.run(Action("pay", "payment", {"amount": 1}, reversible=False, external_side_effect=True), execute=True, approved=True)
    assert result.mode == "executed"
    assert result.preview["charged"] is True

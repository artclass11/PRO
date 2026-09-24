from shadowpilot import Action, Policy, RiskLevel, ShadowPilot


def test_cost_policy_blocks_execution():
    calls = []
    pilot = ShadowPilot(Policy(max_risk_to_execute=RiskLevel.CRITICAL, max_cost=1))
    pilot.register(
        "charge",
        lambda args, shadow: {"preview": True},
        lambda args, shadow: calls.append(True),
    )
    result = pilot.run(
        Action("charge", "charge", {"amount": 99}, estimated_cost=99),
        execute=True,
        approved=True,
    )
    assert result.mode == "blocked"
    assert calls == []


def test_receipt_contains_digest():
    pilot = ShadowPilot()
    result = pilot.run(Action("noop", "noop"), execute=False)
    assert result.receipt["hash"]
    assert len(result.receipt["hash"]) == 64
